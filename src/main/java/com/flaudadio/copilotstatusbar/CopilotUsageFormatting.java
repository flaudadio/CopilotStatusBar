package com.flaudadio.copilotstatusbar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class CopilotUsageFormatting {
    private CopilotUsageFormatting() {
    }

    static String formatResetDate(String value, CopilotLanguage language, ZoneId zone) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Locale locale = language.getLocale();
        Instant resetInstant;
        try {
            resetInstant = Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                resetInstant = OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignoredOffset) {
                try {
                    LocalDate resetDate = LocalDate.parse(value);
                    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(locale)
                            .format(resetDate);
                } catch (DateTimeParseException ignoredDate) {
                    return value;
                }
            }
        }

        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zone)
                .format(resetInstant);
    }
}

