package com.flaudadio.copilotstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CopilotStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private StatusBar statusBar;

    @Override
    public @NotNull String ID() {
        return "CopilotUsageStatus";
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
        CopilotUsageService service = usageService();
        service.addListener(this, () -> {
            if (this.statusBar != null) {
                this.statusBar.updateWidget(ID());
            }
        });
    }

    @Override
    public @NotNull String getText() {
        CopilotUsageService.UsageSnapshot current = usageService().getSnapshot();
        if (!current.hasData()) {
            return "AI Credits: --";
        }
        String entitlement = current.creditEntitlement() == null || current.creditEntitlement() <= 0
                ? ""
                : "/" + current.creditEntitlement();
        return "AI Credits: " + current.usedCredits() + entitlement;
    }

    @Override
    public float getAlignment() {
        return 0;
    }

    @Override
    public @NotNull String getTooltipText() {
        CopilotLanguage language = CopilotUsageSettings.getInstance().getLanguage();
        CopilotUsageService.UsageSnapshot current = usageService().getSnapshot();
        if (!current.hasData()) {
            if (current.error() != null) {
                return language.text("No se pudieron leer los AI Credits: ", "Could not read AI Credits: ",
                        "No s'han pogut llegir els AI Credits: ", "AI Credits نہیں پڑھے جا سکے: ")
                        + current.error() + language.text(
                        " Revisa que el plugin oficial GitHub Copilot esté habilitado y con sesión iniciada.",
                        " Make sure the official GitHub Copilot plugin is enabled and signed in.",
                        " Comprova que el connector oficial GitHub Copilot estigui activat i amb la sessió iniciada.",
                        " تصدیق کریں کہ سرکاری GitHub Copilot پلگ اِن فعال ہے اور آپ سائن اِن ہیں۔");
            }
            return language.text(
                    "Esperando los datos del plugin oficial GitHub Copilot. "
                            + "Inicia sesión en Copilot y haz clic para solicitar una actualización.",
                    "Waiting for data from the official GitHub Copilot plugin. "
                            + "Sign in to Copilot and click to request an update.",
                    "S'estan esperant les dades del connector oficial GitHub Copilot. "
                            + "Inicia sessió a Copilot i fes clic per sol·licitar una actualització.",
                    "سرکاری GitHub Copilot پلگ اِن کے ڈیٹا کا انتظار ہے۔ "
                            + "Copilot میں سائن اِن کریں اور تازہ کاری کی درخواست کے لیے کلک کریں۔");
        }

        String entitlement = current.creditEntitlement() == null || current.creditEntitlement() <= 0
                ? ""
                : "/" + current.creditEntitlement();
        String remaining = current.creditsRemaining() == null
                ? ""
                : language.text(" | Restantes: ", " | Remaining: ", " | Restants: ", " | باقی: ")
                        + current.creditsRemaining();
        String resetDate = current.resetDateUtc() == null || current.resetDateUtc().isBlank()
                ? ""
                : language.text(" | Reinicio: ", " | Reset: ", " | Reinici: ", " | دوبارہ ترتیب: ")
                        + CopilotUsageFormatting.formatResetDate(
                                current.resetDateUtc(), language, ZoneId.systemDefault());
        String plan = current.copilotPlan() == null || current.copilotPlan().isBlank()
                ? ""
                : language.text(" | Plan: ", " | Plan: ", " | Pla: ", " | پلان: ") + current.copilotPlan();
        String cacheReadAt = current.cacheReadAt() == null
                ? language.text("desconocida", "unknown", "desconeguda", "نامعلوم")
                : TIME_FORMAT.format(current.cacheReadAt());
        String refreshError = current.error() == null
                ? ""
                : language.text(" | No se pudo actualizar; se conserva el último valor válido: ",
                        " | Could not refresh; keeping the last valid value: ",
                        " | No s'ha pogut actualitzar; es conserva l'últim valor vàlid: ",
                        " | تازہ کاری نہیں ہو سکی؛ آخری درست قدر برقرار ہے: ") + current.error();
        return language.text("AI Credits usados: ", "AI Credits used: ",
                "AI Credits utilitzats: ", "استعمال شدہ AI Credits: ")
                + current.usedCredits() + entitlement
                + remaining + resetDate + plan
                + language.text(" | Leído del caché de GitHub Copilot a las ",
                        " | Read from the GitHub Copilot cache at ",
                        " | Llegit de la memòria cau de GitHub Copilot a les ",
                        " | GitHub Copilot کیش سے پڑھنے کا وقت ") + cacheReadAt
                + refreshError
                + language.text(" | Clic para actualizar; doble clic para abrir la configuración.",
                        " | Click to refresh; double-click to open settings.",
                        " | Fes clic per actualitzar; fes doble clic per obrir la configuració.",
                        " | تازہ کرنے کے لیے کلک کریں؛ ترتیبات کھولنے کے لیے ڈبل کلک کریں۔");
    }

    @Override
    public @NotNull Consumer<MouseEvent> getClickConsumer() {
        return event -> {
            if (event.getClickCount() >= 2) {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                        statusBar == null ? null : statusBar.getProject(),
                        "com.flaudadio.copilotstatusbar.CopilotSettingsConfigurable");
            } else {
                usageService().requestCopilotRefresh();
            }
        };
    }

    private CopilotUsageService usageService() {
        return ApplicationManager.getApplication().getService(CopilotUsageService.class);
    }

    @Override
    public void dispose() {
        statusBar = null;
    }
}
