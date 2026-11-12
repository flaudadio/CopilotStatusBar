package com.flaudadio.copilotstatusbar;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CopilotUsageService implements Disposable {
    private static final Logger LOG = Logger.getInstance(CopilotUsageService.class);
    private static final String COPILOT_QUOTA_SERVICE =
            "com.github.copilot.services.CopilotQuotaService";
    private static final long MANUAL_REFRESH_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(10);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Copilot AI credit cache refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private final AtomicBoolean thresholdAlerted = new AtomicBoolean();
    private volatile UsageSnapshot snapshot = UsageSnapshot.empty();
    private long lastCopilotRefreshRequestNanos;
    private ScheduledFuture<?> refreshTask;
    private String lastLoggedError;

    public CopilotUsageService() {
        updateRefreshInterval();
    }

    public UsageSnapshot getSnapshot() {
        return snapshot;
    }

    public void addListener(@NotNull Disposable parent, @NotNull Runnable listener) {
        listeners.add(listener);
        Disposer.register(parent, () -> listeners.remove(listener));
    }

    public void notifySettingsChanged() {
        notifyListeners();
    }

    public void refresh() {
        enqueueRefresh(false);
    }

    public void requestCopilotRefresh() {
        enqueueRefresh(true);
    }

    public synchronized void updateRefreshInterval() {
        if (executor.isShutdown()) {
            return;
        }
        if (refreshTask != null) {
            refreshTask.cancel(false);
        }
        int intervalSeconds = CopilotUsageSettings.getInstance().getRefreshIntervalSeconds();
        refreshTask = executor.scheduleWithFixedDelay(
                this::refresh, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void enqueueRefresh(boolean requestCopilotRefresh) {
        if (executor.isShutdown() || !refreshInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    UsageSnapshot updated = readCopilotQuota(requestCopilotRefresh);
                    snapshot = updated;
                    lastLoggedError = null;
                    checkThreshold(updated);
                } catch (IOException | RuntimeException exception) {
                    String error = exception.getMessage() == null
                            ? exception.getClass().getName()
                            : exception.getMessage();
                    if (!error.equals(lastLoggedError)) {
                        LOG.warn("No se pudieron leer los AI Credits de GitHub Copilot", exception);
                        lastLoggedError = error;
                    }
                    UsageSnapshot lastKnownSnapshot = snapshot;
                    snapshot = lastKnownSnapshot.hasData()
                            ? lastKnownSnapshot.withError(error)
                            : UsageSnapshot.failed(error);
                } finally {
                    refreshInProgress.set(false);
                    notifyListeners();
                }
            });
        } catch (RejectedExecutionException exception) {
            refreshInProgress.set(false);
            LOG.debug("Se omitió la lectura de la cuota porque el servicio ya se está cerrando.", exception);
        }
    }

    private UsageSnapshot readCopilotQuota(boolean requestCopilotRefresh) throws IOException {
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(COPILOT_QUOTA_SERVICE, false, getClass().getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "No se encontró el plugin oficial GitHub Copilot. Instálalo y actívalo para mostrar su cuota.",
                            "The official GitHub Copilot plugin was not found. Install and enable it to show the quota.",
                            "No s'ha trobat el connector oficial GitHub Copilot. Instal·la'l i activa'l per mostrar la quota.",
                            "سرکاری GitHub Copilot پلگ اِن نہیں ملا۔ کوٹہ دکھانے کے لیے اسے انسٹال اور فعال کریں۔"),
                    exception);
        }

        Object quotaService;
        try {
            quotaService = ApplicationManager.getApplication().getService(serviceClass);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "La versión instalada de GitHub Copilot no registró el servicio interno de cuotas.",
                            "The installed GitHub Copilot version did not register its internal quota service.",
                            "La versió instal·lada de GitHub Copilot no ha registrat el servei intern de quota.",
                            "انسٹال شدہ GitHub Copilot ورژن نے اپنی اندرونی کوٹہ سروس رجسٹر نہیں کی۔"),
                    exception);
        }
        if (quotaService == null) {
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "El plugin GitHub Copilot no expuso su servicio de cuotas. Comprueba que esté habilitado.",
                            "The GitHub Copilot plugin did not expose its quota service. Make sure it is enabled.",
                            "El connector GitHub Copilot no ha exposat el servei de quota. Comprova que estigui activat.",
                            "GitHub Copilot پلگ اِن نے کوٹہ سروس دستیاب نہیں کی۔ تصدیق کریں کہ یہ فعال ہے۔"));
        }

        long now = System.nanoTime();
        if (requestCopilotRefresh && (lastCopilotRefreshRequestNanos == 0
                || now - lastCopilotRefreshRequestNanos >= MANUAL_REFRESH_COOLDOWN_NANOS)) {
            invokeNoArgMethod(quotaService, "refreshQuotaInBackground", true);
            lastCopilotRefreshRequestNanos = now;
        }

        Object quotaResponse = invokeNoArgMethod(quotaService, "getCachedQuotaInfo", true);
        if (quotaResponse == null) {
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "GitHub Copilot todavía no cargó una cuota. Inicia sesión en el plugin oficial y vuelve a actualizar.",
                            "GitHub Copilot has not loaded a quota yet. Sign in to the official plugin and refresh again.",
                            "GitHub Copilot encara no ha carregat cap quota. Inicia sessió al connector oficial i torna-ho a actualitzar.",
                            "GitHub Copilot نے ابھی تک کوٹہ لوڈ نہیں کیا۔ سرکاری پلگ اِن میں سائن اِن کریں اور دوبارہ تازہ کریں۔"));
        }

        Object tokenBasedBillingEnabled =
                invokeNoArgMethod(quotaResponse, "getTokenBasedBillingEnabled", true);
        if (!Boolean.TRUE.equals(tokenBasedBillingEnabled)) {
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "GitHub Copilot no confirmó el uso de AI Credits para esta cuenta; no se mostrará otra métrica como sustituto.",
                            "GitHub Copilot did not confirm AI Credits for this account; another metric will not be shown instead.",
                            "GitHub Copilot no ha confirmat l'ús d'AI Credits per a aquest compte; no es mostrarà cap altra mètrica com a substitut.",
                            "GitHub Copilot نے اس اکاؤنٹ کے لیے AI Credits کی تصدیق نہیں کی؛ اس کے بجائے کوئی دوسرا پیمانہ نہیں دکھایا جائے گا۔"));
        }

        Object premiumInteractions = invokeNoArgMethod(quotaResponse, "getPremiumInteractions", true);
        if (premiumInteractions == null) {
            throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                    "El plugin GitHub Copilot no devolvió datos de AI Credits.",
                    "The GitHub Copilot plugin did not return AI Credits data.",
                    "El connector GitHub Copilot no ha retornat dades d'AI Credits.",
                    "GitHub Copilot پلگ اِن نے AI Credits کا ڈیٹا واپس نہیں کیا۔"));
        }

        Integer usedCredits = readInteger(premiumInteractions, "getCreditsUsed", true);
        if (usedCredits == null || usedCredits < 0) {
            throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                    "El plugin GitHub Copilot no devolvió un valor válido de AI Credits usados.",
                        "The GitHub Copilot plugin did not return a valid used AI Credits value.",
                        "El connector GitHub Copilot no ha retornat un valor vàlid dels AI Credits utilitzats.",
                        "GitHub Copilot پلگ اِن نے استعمال شدہ AI Credits کی درست قدر واپس نہیں کی۔"));
        }

        Integer entitlement = readInteger(premiumInteractions, "getEntitlement", false);
        Integer creditsRemaining = readInteger(premiumInteractions, "getQuotaRemaining", false);
        String resetDateUtc = readString(quotaResponse, "getResetDateUtc");
        if (resetDateUtc == null || resetDateUtc.isBlank()) {
            resetDateUtc = readString(quotaResponse, "getResetDate");
        }
        String copilotPlan = readString(quotaResponse, "getCopilotPlan");

        return new UsageSnapshot(
                usedCredits,
                entitlement,
                creditsRemaining,
                resetDateUtc,
                copilotPlan,
                Instant.now(),
                null);
    }

    private static Integer readInteger(Object target, String methodName, boolean required) throws IOException {
        Object value = invokeNoArgMethod(target, methodName, required);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                "La respuesta del plugin GitHub Copilot tiene un campo numérico incompatible.",
                "The GitHub Copilot plugin response contains an incompatible numeric field.",
                "La resposta del connector GitHub Copilot conté un camp numèric incompatible.",
                "GitHub Copilot پلگ اِن کے جواب میں غیر مطابقت رکھنے والا عددی خانہ ہے۔"));
    }

    private static String readString(Object target, String methodName) throws IOException {
        Object value = invokeNoArgMethod(target, methodName, false);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                "La respuesta del plugin GitHub Copilot tiene un campo de texto incompatible.",
                "The GitHub Copilot plugin response contains an incompatible text field.",
                "La resposta del connector GitHub Copilot conté un camp de text incompatible.",
                "GitHub Copilot پلگ اِن کے جواب میں غیر مطابقت رکھنے والا متنی خانہ ہے۔"));
    }

    private static Object invokeNoArgMethod(Object target, String methodName, boolean required) throws IOException {
        Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (NoSuchMethodException exception) {
            if (!required) {
                return null;
            }
            throw new IOException(
                    CopilotUsageSettings.getInstance().getLanguage().text(
                            "La versión instalada de GitHub Copilot no es compatible con la integración de AI Credits (",
                            "The installed GitHub Copilot version is incompatible with the AI Credits integration (",
                            "La versió instal·lada de GitHub Copilot no és compatible amb la integració d'AI Credits (",
                            "انسٹال شدہ GitHub Copilot ورژن AI Credits انضمام کے ساتھ مطابقت نہیں رکھتا (")
                            + methodName + ").",
                    exception);
        }

        try {
            return method.invoke(target);
        } catch (IllegalAccessException exception) {
            throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                    "No se pudo acceder al servicio interno de cuotas de GitHub Copilot.",
                    "Could not access the internal GitHub Copilot quota service.",
                    "No s'ha pogut accedir al servei intern de quota de GitHub Copilot.",
                    "GitHub Copilot کی اندرونی کوٹہ سروس تک رسائی ممکن نہیں ہوئی۔"), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IOException(CopilotUsageSettings.getInstance().getLanguage().text(
                    "GitHub Copilot no pudo proporcionar los datos de cuota.",
                    "GitHub Copilot could not provide quota data.",
                    "GitHub Copilot no ha pogut proporcionar les dades de quota.",
                    "GitHub Copilot کوٹہ ڈیٹا فراہم نہیں کر سکا۔"), cause);
        }
    }

    private void checkThreshold(@NotNull UsageSnapshot current) {
        CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
        CopilotLanguage language = settings.getLanguage();
        int threshold = settings.getAlertThreshold();
        if (threshold <= 0 || !current.hasData() || current.usedCredits() < threshold) {
            if (!settings.isAlertOncePerSession()) {
                thresholdAlerted.set(false);
            }
            return;
        }
        if (!thresholdAlerted.compareAndSet(false, true)) {
            return;
        }

        String entitlement = current.creditEntitlement() == null
                ? ""
                : language.text(" de ", " of ", " de ", " میں سے ") + current.creditEntitlement();
        String message = language.text("Copilot alcanzó el umbral configurado: ",
                "Copilot reached the configured threshold: ",
                "Copilot ha arribat al llindar configurat: ",
                "Copilot مقررہ حد تک پہنچ گیا ہے: ")
                + current.usedCredits() + entitlement
                + " AI Credits. " + language.text(
                "El widget lee los datos del caché del plugin oficial.",
                "The widget reads data from the official plugin cache.",
                "El giny llegeix les dades de la memòria cau del connector oficial.",
                "ویجٹ سرکاری پلگ اِن کی کیش سے ڈیٹا پڑھتا ہے۔");
        ApplicationManager.getApplication().invokeLater(() ->
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("Copilot Usage Status")
                        .createNotification(language.text("Umbral de AI Credits alcanzado",
                                "AI Credits threshold reached",
                                "Llindar d'AI Credits assolit",
                                "AI Credits کی حد تک پہنچ گئے"), message, NotificationType.WARNING)
                        .notify(null));
    }

    private void notifyListeners() {
        ApplicationManager.getApplication().invokeLater(() ->
                listeners.forEach(Runnable::run));
    }

    @Override
    public synchronized void dispose() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
        executor.shutdownNow();
        listeners.clear();
    }

    public record UsageSnapshot(
            Integer usedCredits,
            Integer creditEntitlement,
            Integer creditsRemaining,
            String resetDateUtc,
            String copilotPlan,
            Instant cacheReadAt,
            String error
    ) {
        public static UsageSnapshot empty() {
            return new UsageSnapshot(null, null, null, null, null, null, null);
        }

        public static UsageSnapshot failed(String error) {
            return new UsageSnapshot(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    error == null || error.isBlank()
                            ? CopilotUsageSettings.getInstance().getLanguage().text(
                                    "Error leyendo el caché de GitHub Copilot.",
                                    "Error reading the GitHub Copilot cache.",
                                    "S'ha produït un error en llegir la memòria cau de GitHub Copilot.",
                                    "GitHub Copilot کیش پڑھنے میں خرابی۔")
                            : error);
        }

        public boolean hasData() {
            return usedCredits != null;
        }

        public UsageSnapshot withError(String error) {
            return new UsageSnapshot(
                    usedCredits,
                    creditEntitlement,
                    creditsRemaining,
                    resetDateUtc,
                    copilotPlan,
                    cacheReadAt,
                    error);
        }
    }
}
