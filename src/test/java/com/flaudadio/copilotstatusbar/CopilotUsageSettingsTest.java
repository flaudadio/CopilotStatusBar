package com.flaudadio.copilotstatusbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopilotUsageSettingsTest {
    @Test
    void normalizesPersistedSettings() {
        CopilotUsageSettings.SettingsState saved = new CopilotUsageSettings.SettingsState();
        saved.alertThreshold = -5;
        saved.alertOncePerSession = true;
        saved.refreshIntervalSeconds = 5;
        saved.language = "english";

        CopilotUsageSettings settings = new CopilotUsageSettings();
        settings.loadState(saved);

        assertEquals(0, settings.getAlertThreshold());
        assertTrue(settings.isAlertOncePerSession());
        assertEquals(CopilotUsageSettings.DEFAULT_REFRESH_INTERVAL_SECONDS,
                settings.getRefreshIntervalSeconds());
        assertEquals(CopilotLanguage.ENGLISH, settings.getLanguage());
    }

    @Test
    void clampsRefreshIntervalToSupportedMaximum() {
        CopilotUsageSettings.SettingsState saved = new CopilotUsageSettings.SettingsState();
        saved.refreshIntervalSeconds = CopilotUsageSettings.MAX_REFRESH_INTERVAL_SECONDS + 1;

        CopilotUsageSettings settings = new CopilotUsageSettings();
        settings.loadState(saved);

        assertEquals(CopilotUsageSettings.MAX_REFRESH_INTERVAL_SECONDS,
                settings.getRefreshIntervalSeconds());
    }

    @Test
    void defaultsUnknownLanguageAndAlertFrequency() {
        CopilotUsageSettings.SettingsState saved = new CopilotUsageSettings.SettingsState();
        saved.language = "unknown";
        saved.alertOncePerSession = false;

        CopilotUsageSettings settings = new CopilotUsageSettings();
        settings.loadState(saved);

        assertEquals(CopilotLanguage.SPANISH, settings.getLanguage());
        assertFalse(settings.isAlertOncePerSession());
    }
}

