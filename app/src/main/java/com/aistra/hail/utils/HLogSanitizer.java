package com.aistra.hail.utils;

/**
 * Removes the line breaks of a message before it is written to the log, so
 * that a value carrying one cannot forge a log line of its own.
 *
 * <p>The removal is written in Java on purpose. The analyzer that reports
 * forged log entries recognizes it only as a call to
 * {@link String#replace(CharSequence, CharSequence)}, and the Kotlin spelling
 * of the same call is an extension of another type that the analyzer does not
 * recognize, so a message cleaned in Kotlin is still reported.
 */
public final class HLogSanitizer {
    private HLogSanitizer() {
    }

    public static String sanitize(String message) {
        return message.replace("\n", " ").replace("\r", " ");
    }
}
