**Accept** — `4850afd`. Both consequences are real and I had only addressed the lifetime, not the name.

`Files.createTempDirectory("hail-unit-test-files")` replaces `File(tmpdir, "hail-unit-test-files").apply { mkdirs() }`:

- **Unique per JVM**, so a leftover `v1/apps.json` or `v1/tags.json` from a previous run is not this run's state, and two Gradle forks on one machine do not share it. The old fixed name was the same path for every checkout and every fork, which is exactly the order- and history-dependence the `@Rule` had been preventing.
- **Throws** rather than returning a path that is not a directory. `mkdirs()`'s `false` was being discarded, so a pre-existing directory owned by another local user — or a machine that simply would not create one — was accepted silently, and `filesDir` then pointed at something that is not a directory at all.
- **Removed at JVM exit** by a shutdown hook. Long enough for the frozen `val` to stay valid for every test that follows, short enough that it does not outlive the run. A `@Rule` cannot do this, and neither can a per-class cleanup: the directory has to survive whichever class is second.

The helper is shared by both classes now, so the same reasoning covers the thread about `HBackupTest` winning the race — the directory is created once per JVM rather than once per class.
