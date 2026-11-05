package com.flaudadio.copilotstatusbar;

public enum CopilotLanguage {
    SPANISH("Español"),
    ENGLISH("English");

    private final String displayName;

    CopilotLanguage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String text(String spanish, String english) {
        return this == ENGLISH ? english : spanish;
    }

    public static CopilotLanguage fromValue(String value) {
        if (value != null) {
            for (CopilotLanguage language : values()) {
                if (language.name().equalsIgnoreCase(value)) {
                    return language;
                }
            }
        }
        return SPANISH;
    }
}
