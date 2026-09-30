package com.aistra.hail

import com.aistra.hail.app.HailData
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files

/**
 * Installs a mock app whose files directory outlives the whole JVM, and touches [HailData]
 * while that app is installed.
 *
 * This exists because of one line of production code. `HailData.dir` is an ordinary `val`
 * built from `app.filesDir` (HailData.kt:226), so the first test in the JVM to touch the
 * object freezes the directory every later test in that JVM will see - and installing a
 * different app afterwards cannot move it. A class that freezes it onto a directory its own
 * `@Rule` then deletes leaves the object pointing at a path that no longer exists, which
 * costs nothing until the first test that reads or writes those paths for real, and then
 * costs a failure that names `HFiles` rather than the test that caused it.
 *
 * So the directory is created once per JVM, not once per test or once per checkout: unique
 * so a leftover from another run cannot be read as this run's state, and removed by a
 * shutdown hook so it does not outlive the JVM either. `Files.createTempDirectory` throws
 * rather than returning a path that is not a directory, which is the right way for a test to
 * fail if the machine will not give it one.
 *
 * Call this from `@BeforeClass`, before anything else in the class can touch [HailData].
 * A test class that needs per-test isolation should install its own app afterwards in
 * `@Before`; that is safe precisely because the object is already frozen by then.
 */
fun installHailDataFilesDirForTests() {
    val app = mockk<HailApp>(relaxed = true)
    every { app.filesDir } returns filesDirForTheLengthOfThisJvm
    HailApp.setAppForTest(app)
    // Any read of a non-const member runs the static initializer, and the statement has to
    // mean something so that nobody "cleans it up" as a no-op later.
    check(HailData.FLOAT_PREFERENCES.isNotEmpty())
}

private val filesDirForTheLengthOfThisJvm: File by lazy {
    Files.createTempDirectory("hail-unit-test-files").toFile().also { dir ->
        Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
    }
}
