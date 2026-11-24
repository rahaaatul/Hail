package com.aistra.hail.app

data class BatchResult(
    val total: Int,
    val succeeded: Int,
    val failed: List<String>,
) {
    val allSucceeded: Boolean get() = failed.isEmpty()
}
