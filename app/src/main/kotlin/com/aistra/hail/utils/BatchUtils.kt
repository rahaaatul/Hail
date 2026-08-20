package com.aistra.hail.utils

object BatchUtils {
    // Safety cap: Android ARG_MAX ~128KB, keep well under with 50 commands max
    const val MAX_COMMANDS_PER_SCRIPT = 50
    const val MAX_SCRIPT_LENGTH = 100_000 // ~100KB safety margin

    private const val EXIT_CODE_MARKER = "EXIT_CODE:"

    fun parseBatchOutput(output: String, expectedCount: Int): List<Pair<Int, String?>> {
        val results = mutableListOf<Pair<Int, String?>>()
        val parts = output.split(EXIT_CODE_MARKER)
        // parts[0] is content before first marker (usually empty), skip it
        for (i in 1 until parts.size) {
            if (results.size >= expectedCount) break
            val chunk = parts[i]
            val lines = chunk.split("\n", limit = 2)
            val exitCode = lines[0].toIntOrNull() ?: 1
            val cmdOutput = if (lines.size > 1) lines[1].trim().takeIf { it.isNotEmpty() } else null
            results.add(exitCode to cmdOutput)
        }
        // Pad if fewer results than expected (defensive)
        while (results.size < expectedCount) {
            results.add(1 to "No output parsed")
        }
        return results
    }

    fun buildBatchScript(commands: List<String>): String {
        val script = commands.joinToString("; ") { "$it; echo \"$EXIT_CODE_MARKER\$?\"" }
        require(script.length <= MAX_SCRIPT_LENGTH) { "Batch script exceeds MAX_SCRIPT_LENGTH" }
        return script
    }

    fun chunkCommands(commands: List<String>, batchSize: Int): List<List<String>> {
        val effectiveBatchSize = batchSize.coerceAtMost(MAX_COMMANDS_PER_SCRIPT)
        return commands.chunked(effectiveBatchSize)
    }
}