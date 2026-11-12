package com.flaudadio.copilotstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.ComponentOrientation;
import java.awt.Dimension;

public final class CopilotSettingsConfigurable implements Configurable {
    private JPanel panel;
    private JSpinner thresholdField;
    private JSpinner refreshIntervalField;
    private JCheckBox alertOncePerSessionField;
    private JComboBox<CopilotLanguage> languageCombo;

    @Override
    public @Nls String getDisplayName() {
        return "Copilot Usage Status";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        thresholdField = new JSpinner(new SpinnerNumberModel(6_000, 0, Integer.MAX_VALUE, 100));
        refreshIntervalField = new JSpinner(new SpinnerNumberModel(
                CopilotUsageSettings.DEFAULT_REFRESH_INTERVAL_SECONDS,
                CopilotUsageSettings.MIN_REFRESH_INTERVAL_SECONDS,
                CopilotUsageSettings.MAX_REFRESH_INTERVAL_SECONDS,
                10));
        alertOncePerSessionField = new JCheckBox();
        languageCombo = new JComboBox<>(CopilotLanguage.values());
        languageCombo.setRenderer((list, value, index, isSelected, cellHasFocus) ->
                new JLabel(value == null ? "" : value.getDisplayName()));
        languageCombo.addActionListener(event -> rebuildPanel());

        reset();
        return panel;
    }

    private void rebuildPanel() {
        if (panel == null || languageCombo == null) {
            return;
        }
        CopilotLanguage language = selectedLanguage();
        ComponentOrientation orientation = language == CopilotLanguage.URDU_PAKISTAN
                ? ComponentOrientation.RIGHT_TO_LEFT
                : ComponentOrientation.LEFT_TO_RIGHT;
        panel.setComponentOrientation(orientation);
        panel.removeAll();
        addField(language.text("Idioma / Language", "Language / Idioma", "Llengua", "زبان"), languageCombo);
        addHint(language.text(
                "El widget lee AI Credits desde el caché del plugin oficial GitHub Copilot. "
                        + "No requiere un token de GitHub ni permisos de métricas de la organización.",
                "The widget reads AI Credits from the official GitHub Copilot plugin cache. "
                        + "No GitHub token or organization metrics permission is required.",
                "El giny llegeix els AI Credits de la memòria cau del connector oficial GitHub Copilot. "
                        + "No cal cap token de GitHub ni permís de mètriques de l'organització.",
                "ویجٹ سرکاری GitHub Copilot پلگ اِن کیش سے AI Credits پڑھتا ہے۔ "
                        + "GitHub ٹوکن یا تنظیمی میٹرکس کی اجازت درکار نہیں۔"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addField(language.text("Umbral de alerta en AI Credits (0 desactiva)", "AI Credits alert threshold (0 disables)",
                        "Llindar d'alerta d'AI Credits (0 el desactiva)", "AI Credits الرٹ کی حد (0 سے بند)"),
                thresholdField);
        addHint(language.text(
                "Valor predeterminado: 6000. La alerta solo avisa; no limita ni bloquea solicitudes.",
                "Default: 6000. The alert is informational; it does not limit or block requests.",
                "Valor predeterminat: 6000. L'alerta només avisa; no limita ni bloqueja les sol·licituds.",
                "طے شدہ قدر: 6000۔ الرٹ صرف اطلاع دیتا ہے؛ درخواستوں کو محدود یا بلاک نہیں کرتا۔"));
        alertOncePerSessionField.setText(language.text(
                "Notificar solo una vez por sesión del IDE",
                "Notify only once per IDE session",
                "Notifica només una vegada per sessió de l'IDE",
                "IDE سیشن میں صرف ایک بار اطلاع دیں"));
        alertOncePerSessionField.setToolTipText(language.text(
                "Desactívalo para recibir otra alerta si el uso baja del umbral y luego lo vuelve a superar.",
                "Turn this off to alert again if usage drops below the threshold and later crosses it again.",
                "Desactiva-ho per rebre una altra alerta si l'ús baixa del llindar i després el torna a superar.",
                "اگر استعمال حد سے نیچے آ کر دوبارہ اس سے بڑھ جائے تو پھر الرٹ پانے کے لیے اسے بند کریں۔"));
        alertOncePerSessionField.setComponentOrientation(orientation);
        alertOncePerSessionField.setAlignmentX(orientation.isLeftToRight()
                ? JComponent.LEFT_ALIGNMENT : JComponent.RIGHT_ALIGNMENT);
        panel.add(alertOncePerSessionField);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addField(language.text("Intervalo de actualización (segundos, 30-3600)",
                "Refresh interval (seconds, 30-3600)",
                "Interval d'actualització (segons, 30-3600)", "تازہ کاری کا وقفہ (سیکنڈ، 30-3600)"), refreshIntervalField);
        addHint(language.text(
                "Predeterminado: 60 segundos. La lectura periódica consulta el caché del plugin oficial. "
                        + "Un clic en el widget solicita una actualización; el plugin oficial puede aplicar sus propios límites. "
                        + "Un doble clic abre esta configuración.",
                "Default: 60 seconds. Periodic reads check the official plugin's cache. Click the widget to request "
                        + "an update; the official plugin may apply its own limits. Double-click to open these settings.",
                "Predeterminat: 60 segons. Les lectures periòdiques consulten la memòria cau del connector oficial. "
                        + "Fes clic al giny per sol·licitar una actualització; el connector pot aplicar els seus límits. "
                        + "Fes doble clic per obrir aquesta configuració.",
                "طے شدہ: 60 سیکنڈ۔ وقفے وقفے سے سرکاری پلگ اِن کیش پڑھی جاتی ہے۔ تازہ کاری کی درخواست کے لیے ویجٹ پر کلک کریں؛ "
                        + "سرکاری پلگ اِن اپنی حدود لاگو کر سکتا ہے۔ ترتیبات کھولنے کے لیے ڈبل کلک کریں۔"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addHint(language.text(
                "Requiere que GitHub Copilot esté instalado, habilitado y con sesión iniciada. "
                        + "La integración usa una API interna no pública del plugin oficial y puede cambiar "
                        + "con sus actualizaciones.",
                "GitHub Copilot must be installed, enabled, and signed in. This integration uses an "
                        + "unpublished internal API from the official plugin, which may change in future updates.",
                "Cal tenir GitHub Copilot instal·lat, activat i amb la sessió iniciada. Aquesta integració utilitza una "
                        + "API interna no pública del connector oficial, que pot canviar en actualitzacions futures.",
                "GitHub Copilot انسٹال، فعال اور سائن اِن ہونا چاہیے۔ یہ انضمام سرکاری پلگ اِن کا غیر عوامی اندرونی API استعمال کرتا ہے، "
                        + "جو آئندہ اپ ڈیٹس میں تبدیل ہو سکتا ہے۔"));
        panel.revalidate();
        panel.repaint();
    }

    private CopilotLanguage selectedLanguage() {
        Object selected = languageCombo == null ? null : languageCombo.getSelectedItem();
        return selected instanceof CopilotLanguage language ? language : CopilotLanguage.SPANISH;
    }

    private void addField(String label, JComponent field) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setLabelFor(field);
        ComponentOrientation orientation = panel.getComponentOrientation();
        labelComponent.setComponentOrientation(orientation);
        labelComponent.setHorizontalAlignment(orientation.isLeftToRight() ? JLabel.LEFT : JLabel.RIGHT);
        labelComponent.setAlignmentX(orientation.isLeftToRight()
                ? JComponent.LEFT_ALIGNMENT : JComponent.RIGHT_ALIGNMENT);
        panel.add(labelComponent);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        field.setComponentOrientation(orientation);
        field.setAlignmentX(orientation.isLeftToRight()
                ? JComponent.LEFT_ALIGNMENT : JComponent.RIGHT_ALIGNMENT);
        field.getAccessibleContext().setAccessibleName(label);
        field.setMaximumSize(new Dimension(460, field.getPreferredSize().height));
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void addHint(String text) {
        boolean leftToRight = panel.getComponentOrientation().isLeftToRight();
        String html = leftToRight
                ? "<html>" + text + "</html>"
                : "<html><div style='text-align: right;'>" + text + "</div></html>";
        JLabel hint = new JLabel(html);
        hint.setComponentOrientation(panel.getComponentOrientation());
        hint.setAlignmentX(leftToRight ? JComponent.LEFT_ALIGNMENT : JComponent.RIGHT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        panel.add(hint);
    }

    @Override
    public boolean isModified() {
        if (thresholdField == null) {
            return false;
        }
        try {
            CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
            return ((Number) thresholdField.getValue()).intValue() != settings.getAlertThreshold()
                    || ((Number) refreshIntervalField.getValue()).intValue()
                    != settings.getRefreshIntervalSeconds()
                    || alertOncePerSessionField.isSelected() != settings.isAlertOncePerSession()
                    || selectedLanguage() != settings.getLanguage();
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        int threshold = ((Number) thresholdField.getValue()).intValue();
        int refreshInterval = ((Number) refreshIntervalField.getValue()).intValue();
        CopilotLanguage language = selectedLanguage();

        CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
        settings.setAlertThreshold(threshold);
        settings.setAlertOncePerSession(alertOncePerSessionField.isSelected());
        settings.setRefreshIntervalSeconds(refreshInterval);
        settings.setLanguage(language);
        CopilotUsageService service = ApplicationManager.getApplication().getService(CopilotUsageService.class);
        service.notifySettingsChanged();
        service.updateRefreshInterval();
        service.refresh();
    }

    @Override
    public void reset() {
        if (thresholdField != null) {
            CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
            thresholdField.setValue(settings.getAlertThreshold());
            alertOncePerSessionField.setSelected(settings.isAlertOncePerSession());
            refreshIntervalField.setValue(settings.getRefreshIntervalSeconds());
            languageCombo.setSelectedItem(settings.getLanguage());
            rebuildPanel();
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        thresholdField = null;
        refreshIntervalField = null;
        alertOncePerSessionField = null;
        languageCombo = null;
    }
}
