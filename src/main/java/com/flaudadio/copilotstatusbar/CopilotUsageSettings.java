package com.flaudadio.copilotstatusbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "CopilotUsageSettings", storages = @Storage("copilot-usage.xml"))
public final class CopilotUsageSettings implements PersistentStateComponent<CopilotUsageSettings.SettingsState> {
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 60;
    public static final int MIN_REFRESH_INTERVAL_SECONDS = 30;
    public static final int MAX_REFRESH_INTERVAL_SECONDS = 3_600;

    private volatile SettingsState state = new SettingsState();

    public static CopilotUsageSettings getInstance() {
        return ApplicationManager.getApplication().getService(CopilotUsageSettings.class);
    }

    @Override
    public @NotNull SettingsState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull SettingsState loadedState) {
        SettingsState normalized = new SettingsState();
        normalized.alertThreshold = Math.max(0, loadedState.alertThreshold);
        normalized.refreshIntervalSeconds = clampRefreshInterval(loadedState.refreshIntervalSeconds);
        normalized.language = CopilotLanguage.fromValue(loadedState.language).name();
        state = normalized;
    }

    public int getAlertThreshold() {
        return state.alertThreshold;
    }

    public void setAlertThreshold(int threshold) {
        state.alertThreshold = Math.max(0, threshold);
    }

    public int getRefreshIntervalSeconds() {
        return state.refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int seconds) {
        state.refreshIntervalSeconds = clampRefreshInterval(seconds);
    }

    public CopilotLanguage getLanguage() {
        return CopilotLanguage.fromValue(state.language);
    }

    public void setLanguage(CopilotLanguage language) {
        state.language = (language == null ? CopilotLanguage.SPANISH : language).name();
    }

    private static int clampRefreshInterval(int seconds) {
        if (seconds < MIN_REFRESH_INTERVAL_SECONDS) {
            return DEFAULT_REFRESH_INTERVAL_SECONDS;
        }
        return Math.min(MAX_REFRESH_INTERVAL_SECONDS, seconds);
    }

    public static final class SettingsState {
        public int alertThreshold = 6_000;
        public int refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SECONDS;
        public String language = CopilotLanguage.SPANISH.name();
    }
}
