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
        panel.removeAll();
        addField(language.text("Idioma / Language", "Language / Idioma"), languageCombo);
        addHint(language.text(
                "El widget lee AI Credits desde el caché del plugin oficial GitHub Copilot. "
                        + "No requiere un token de GitHub ni permisos de métricas de la organización.",
                "The widget reads AI Credits from the official GitHub Copilot plugin cache. "
                        + "No GitHub token or organization metrics permission is required."));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addField(language.text("Umbral de alerta en AI Credits (0 desactiva)", "AI Credits alert threshold (0 disables)"),
                thresholdField);
        addHint(language.text(
                "Valor predeterminado: 6000. La alerta solo avisa; no limita ni bloquea solicitudes.",
                "Default: 6000. The alert is informational; it does not limit or block requests."));
        alertOncePerSessionField.setText(language.text(
                "Notificar solo una vez por sesión del IDE",
                "Notify only once per IDE session"));
        alertOncePerSessionField.setToolTipText(language.text(
                "Desactívalo para recibir otra alerta si el uso baja del umbral y luego lo vuelve a superar.",
                "Turn this off to alert again if usage drops below the threshold and later crosses it again."));
        alertOncePerSessionField.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(alertOncePerSessionField);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addField(language.text("Intervalo de actualización (segundos, 30-3600)",
                "Refresh interval (seconds, 30-3600)"), refreshIntervalField);
        addHint(language.text(
                "Predeterminado: 60 segundos. La lectura periódica consulta el caché del plugin oficial. "
                        + "Un clic en el widget solicita una actualización; el plugin oficial puede aplicar sus propios límites. "
                        + "Un doble clic abre esta configuración.",
                "Default: 60 seconds. Periodic reads check the official plugin's cache. Click the widget to request "
                        + "an update; the official plugin may apply its own limits. Double-click to open these settings."));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addHint(language.text(
                "Requiere que GitHub Copilot esté instalado, habilitado y con sesión iniciada. "
                        + "La integración usa una API interna no pública del plugin oficial y puede cambiar "
                        + "con sus actualizaciones.",
                "GitHub Copilot must be installed, enabled, and signed in. This integration uses an "
                        + "unpublished internal API from the official plugin, which may change in future updates."));
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
        labelComponent.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(labelComponent);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        field.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        field.getAccessibleContext().setAccessibleName(label);
        field.setMaximumSize(new Dimension(460, field.getPreferredSize().height));
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void addHint(String text) {
        JLabel hint = new JLabel("<html>" + text + "</html>");
        hint.setAlignmentX(JComponent.LEFT_ALIGNMENT);
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
