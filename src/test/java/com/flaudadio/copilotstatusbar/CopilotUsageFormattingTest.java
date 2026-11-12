package com.flaudadio.copilotstatusbar;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CopilotUsageFormattingTest {
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void formatsResetInstantInLocalZoneAndSelectedLanguage() {
        String timestamp = "2026-01-15T12:00:00Z";

        String spanish = CopilotUsageFormatting.formatResetDate(timestamp, CopilotLanguage.SPANISH, BUENOS_AIRES);
        String english = CopilotUsageFormatting.formatResetDate(timestamp, CopilotLanguage.ENGLISH, BUENOS_AIRES);

        assertNotEquals(timestamp, spanish);
        assertNotEquals(timestamp, english);
        assertNotEquals(spanish, english);
        assertNotEquals(spanish, CopilotUsageFormatting.formatResetDate(
                timestamp, CopilotLanguage.SPANISH, ZoneId.of("UTC")));
    }

    @Test
    void leavesUnknownDateFormatsUnchanged() {
        String unrecognized = "not-a-date";

        assertEquals(unrecognized, CopilotUsageFormatting.formatResetDate(
                unrecognized, CopilotLanguage.ENGLISH, BUENOS_AIRES));
    }

    @Test
    void doesNotShiftDateOnlyValuesAcrossTimeZones() {
        String date = "2026-01-15";

        assertEquals(
                CopilotUsageFormatting.formatResetDate(date, CopilotLanguage.ENGLISH, ZoneId.of("UTC")),
                CopilotUsageFormatting.formatResetDate(date, CopilotLanguage.ENGLISH, BUENOS_AIRES));
    }

    @Test
    void formatsTimestampsWithCatalanAndUrduLocales() {
        String timestamp = "2026-01-15T12:00:00Z";
        String english = CopilotUsageFormatting.formatResetDate(timestamp, CopilotLanguage.ENGLISH, BUENOS_AIRES);

        assertNotEquals(english,
                CopilotUsageFormatting.formatResetDate(timestamp, CopilotLanguage.CATALAN, BUENOS_AIRES));
        assertNotEquals(english,
                CopilotUsageFormatting.formatResetDate(timestamp, CopilotLanguage.URDU_PAKISTAN, BUENOS_AIRES));
    }

    @Test
    void preservesEmptyValues() {
        assertEquals("", CopilotUsageFormatting.formatResetDate("", CopilotLanguage.ENGLISH, BUENOS_AIRES));
        assertNull(CopilotUsageFormatting.formatResetDate(null, CopilotLanguage.ENGLISH, BUENOS_AIRES));
    }
}


