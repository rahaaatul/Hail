package com.aistra.hail.utils

import android.util.Log

object HLog {
    private const val TAG = "Hail"
    fun i(tag: String, string: String) = Log.i(tag, string)
    fun e(t: Throwable) = Log.e(TAG, t.stackTraceToString())
    fun d(string: String) = Log.d(TAG, string)
    fun e(string: String, t: Throwable) = Log.e(TAG, string, t)
}