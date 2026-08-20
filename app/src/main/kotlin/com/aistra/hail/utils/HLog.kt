package com.aistra.hail.utils

import android.util.Log

object HLog {
    private const val TAG = "Hail"
    fun i(tag: String, string: String) = Log.i(tag, string)
    fun e(t: Throwable) = Log.e(TAG, t.stackTraceToString())
    fun e(message: String) = Log.e(TAG, message)
    fun e(message: String, t: Throwable) = Log.e(TAG, message, t)
}