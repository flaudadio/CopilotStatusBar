package com.flaudadio.copilotstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;

public final class CopilotSettingsConfigurable implements Configurable {
    private JPanel panel;
    private JTextField thresholdField;
    private JTextField refreshIntervalField;
    private JComboBox<CopilotLanguage> languageCombo;

    @Override
    public @Nls String getDisplayName() {
        return "Copilot Usage Status";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        thresholdField = new JTextField(10);
        refreshIntervalField = new JTextField(10);
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
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        addField(language.text("Intervalo de actualización (segundos, 30-3600)",
                "Refresh interval (seconds, 30-3600)"), refreshIntervalField);
        addHint(language.text(
                "Predeterminado: 60 segundos. Cada lectura solicita al servicio oficial una actualización "
                        + "en segundo plano; el plugin oficial puede aplicar sus propios límites. Un clic en el widget "
                        + "también actualiza y un doble clic abre esta configuración.",
                "Default: 60 seconds. Each read requests a background update from the official service; "
                        + "the official plugin may apply its own limits. Click the widget to refresh, or double-click "
                        + "to open these settings."));
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
        labelComponent.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        panel.add(labelComponent);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        field.setAlignmentX(JComponent.LEFT_ALIGNMENT);
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
            return Integer.parseInt(thresholdField.getText().trim()) != settings.getAlertThreshold()
                    || Integer.parseInt(refreshIntervalField.getText().trim())
                    != settings.getRefreshIntervalSeconds()
                    || selectedLanguage() != settings.getLanguage();
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        int threshold = parseInteger(thresholdField);
        int refreshInterval = parseInteger(refreshIntervalField);
        CopilotLanguage language = selectedLanguage();
        if (threshold < 0) {
            throw new ConfigurationException(language.text(
                    "El umbral debe ser mayor o igual a cero.", "The threshold must be zero or greater."));
        }
        if (refreshInterval < CopilotUsageSettings.MIN_REFRESH_INTERVAL_SECONDS
                || refreshInterval > CopilotUsageSettings.MAX_REFRESH_INTERVAL_SECONDS) {
            throw new ConfigurationException(
                    language.text("El intervalo debe estar entre ", "The interval must be between ")
                            + CopilotUsageSettings.MIN_REFRESH_INTERVAL_SECONDS
                            + language.text(" y ", " and ")
                            + CopilotUsageSettings.MAX_REFRESH_INTERVAL_SECONDS
                            + language.text(" segundos.", " seconds."));
        }

        CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
        settings.setAlertThreshold(threshold);
        settings.setRefreshIntervalSeconds(refreshInterval);
        settings.setLanguage(language);
        CopilotUsageService service = ApplicationManager.getApplication().getService(CopilotUsageService.class);
        service.notifySettingsChanged();
        service.resetThresholdAlert();
        service.updateRefreshInterval();
        service.refresh();
    }

    private int parseInteger(JTextField field) throws ConfigurationException {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException exception) {
            throw new ConfigurationException(selectedLanguage().text(
                    "Ingresa un número entero válido.", "Enter a valid integer."));
        }
    }

    @Override
    public void reset() {
        if (thresholdField != null) {
            CopilotUsageSettings settings = CopilotUsageSettings.getInstance();
            thresholdField.setText(Integer.toString(settings.getAlertThreshold()));
            refreshIntervalField.setText(Integer.toString(settings.getRefreshIntervalSeconds()));
            languageCombo.setSelectedItem(settings.getLanguage());
            rebuildPanel();
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        thresholdField = null;
        refreshIntervalField = null;
        languageCombo = null;
    }
}
