package com.flaudadio.copilotstatusbar;

public enum CopilotLanguage {
    SPANISH("Español", "es"),
    ENGLISH("English", "en"),
    CATALAN("Català", "ca"),
    URDU_PAKISTAN("Urdu (Pakistan) — اردو (پاکستان)", "ur-PK");

    private final String displayName;
    private final String localeTag;

    CopilotLanguage(String displayName, String localeTag) {
        this.displayName = displayName;
        this.localeTag = localeTag;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String text(String spanish, String english, String catalan, String urduPakistan) {
        return switch (this) {
            case SPANISH -> spanish;
            case ENGLISH -> english;
            case CATALAN -> catalan;
            case URDU_PAKISTAN -> urduPakistan;
        };
    }

    public java.util.Locale getLocale() {
        return java.util.Locale.forLanguageTag(localeTag);
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
