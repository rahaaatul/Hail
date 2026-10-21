package com.aistra.hail.utils

import android.util.Log

object HLog {
    private const val TAG = "Hail"

    // The message of a throwable is written by whoever raised it and can hold
    // text from outside the app, so it is flattened. The frames are written by
    // the runtime and keep their lines.
    private fun sanitizeTrace(throwable: Throwable): String {
        val seen = HashSet<Throwable>()
        return buildString {
            var current: Throwable? = throwable
            var prefix = ""
            while (current != null && seen.add(current)) {
                append(prefix).append(HLogSanitizer.sanitize(current.toString())).append('\n')
                current.stackTrace.forEach { frame -> append("\tat ").append(frame).append('\n') }
                current.suppressed.forEach { suppressed ->
                    append("\tSuppressed: ").append(HLogSanitizer.sanitize(suppressed.toString())).append('\n')
                    suppressed.stackTrace.forEach { frame -> append("\t\tat ").append(frame).append('\n') }
                }
                prefix = "Caused by: "
                current = current.cause
            }
        }.trimEnd()
    }

    fun i(tag: String, string: String) = Log.i(tag, HLogSanitizer.sanitize(string))
    fun e(t: Throwable) = Log.e(TAG, sanitizeTrace(t))
    fun e(string: String) = Log.e(TAG, HLogSanitizer.sanitize(string))
    fun d(string: String) = Log.d(TAG, HLogSanitizer.sanitize(string))
    fun w(tag: String, string: String) = Log.w(tag, HLogSanitizer.sanitize(string))
    fun e(string: String, t: Throwable) =
        Log.e(TAG, HLogSanitizer.sanitize(string) + "\n" + sanitizeTrace(t))
}
