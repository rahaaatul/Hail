# Hail devcontainer
#
# Why this base: mcr.microsoft.com/devcontainers/base:bookworm is a Debian
# bookworm image with no JDK. The previous FROM line used
# devcontainers/java:26-bookworm, which does not exist - a GET of
# https://mcr.microsoft.com/v2/devcontainers/java/tags/list returns no "26"
# tag (newest is 25) and the manifest at
# https://mcr.microsoft.com/v2/devcontainers/java/manifests/26-bookworm
# returns HTTP 404, so that image could never build.
FROM mcr.microsoft.com/devcontainers/base:bookworm

# Temurin 27 install. The project requires JDK 27: app/build.gradle.kts sets
# java.toolchain to JavaLanguageVersion.of(27) and kotlin.jvmToolchain(27),
# .github/.java-version is 27, and CI uses actions/setup-java with
# distribution temurin. JDK 27 GA (jdk-27+35) was released September 15, 2026;
# Gradle 9.8.0 (released September 29, 2026) added explicit Java 27 support,
# which is why the wrapper is bumped to 9.8.0 alongside this JDK. No JDK 27
# image ships on MCR, so install Temurin 27 from Adoptium - the same vendor
# CI uses - into a path that does not change when the patch version bumps:
# /usr/lib/jvm/temurin-27.
#
# Re-derive these values from
# https://api.adoptium.net/v3/assets/latest/27/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse
# (and the aarch64 variant) when bumping the JDK. The release printed by that
# API as of 2026-09-21 is 27+35.
ARG JDK_RELEASE=27+35
ARG JDK_SHA256_AMD64=1cf69a4848ffb728b3b260dfd45206a51566ab571a02a30092271d4c580bccbc
ARG JDK_SHA256_ARM64=e4ec5c7276290c7bde0baecba9a29be4962f5388684296406a9799d465936087
ARG TARGETARCH

# JAVA_HOME is exported before the install RUN so the same step can use it as
# the extraction destination, instead of leaving the tarball in /usr/lib/jvm
# while JAVA_HOME points one level deeper at a never-created directory.
ENV JAVA_HOME=/usr/lib/jvm/temurin-27
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Lean apt install. The repo's helper scripts (build.sh, version.sh,
# upload.py, test_scripts.sh) invoke only bash/coreutils, python3, curl and
# unzip; there is no NDK/CMake usage anywhere - a grep for externalNativeBuild,
# ndkVersion and CMakeLists across the tree returned nothing - so the old
# libc6-dev/libstdc++6/zlib1g entries are dropped.
#
# Run before the JDK download so the install RUN can rely on curl being
# present instead of on whatever the base image happens to ship.
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        python3 \
        unzip \
    && rm -rf /var/lib/apt/lists/*

# TARGETARCH is supplied by BuildKit; map it to the names Adoptium uses and
# fail loudly on anything else so Apple Silicon (arm64) is supported while an
# unknown arch does not silently fall through to a wrong download.
RUN set -eux; \
    case "${TARGETARCH}" in \
        amd64) ARCH=x64;   SHA="${JDK_SHA256_AMD64}" ;; \
        arm64) ARCH=aarch64; SHA="${JDK_SHA256_ARM64}" ;; \
        *) echo "Unsupported architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    # The URL encodes "+" as %2B (many servers decode a literal "+" as a space)
    # while the filename uses "_" instead of "+", so derive both forms from the
    # same release string rather than hardcoding the literal URL and drifting
    # out of sync.
    FILE="OpenJDK27U-jdk_${ARCH}_linux_hotspot_$(printf '%s' "${JDK_RELEASE}" | tr '+' '_').tar.gz"; \
    RELEASE_PATH="$(printf '%s' "${JDK_RELEASE}" | sed 's/+/%2B/g')"; \
    curl -fsSL -O "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-${RELEASE_PATH}/${FILE}"; \
    echo "${SHA}  ${FILE}" | sha256sum -c; \
    mkdir -p "${JAVA_HOME}"; \
    tar -xzf "${FILE}" -C "${JAVA_HOME}" --strip-components=1; \
    rm "${FILE}"

# Smoke-test the JDK in the same RUN so a bad download or a corrupt tarball
# fails the build instead of surfacing later inside the devcontainer.
RUN "${JAVA_HOME}/bin/java" -version

# Android SDK root. ANDROID_SDK_ROOT stays for older tooling that reads it.
ENV ANDROID_HOME=/usr/local/lib/android/sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}

# PATH: cmdline-tools and platform-tools are needed; no build-tools directory is
# appended here because AGP resolves the build-tools version itself and a
# hardcoded entry is the drift trap that previously pointed at a nonexistent
# directory.
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Android SDK command-line tools. Pinned to 23.0 (zip 16111833): verified by
# reading source.properties inside the archive, which reports
# Pkg.Revision=23.0. The previous pin was cmdline-tools v12 from 2024.
RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" && \
    curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools" && \
    mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" && \
    rm /tmp/cmdline-tools.zip

# Install SDK packages. One invocation per package: `android sdk install`
# accepts exactly one positional package, so each gets its own call. Paths
# use the slash form that `android sdk list` prints; the previous Dockerfile
# used semicolon paths that no longer exist - platforms;android-37 and
# build-tools;37.2.0 are both absent from repository2-3.xml.
#
# platforms/android-37.2 is the latest revision of the android-37 platform,
# which satisfies the compileSdk/targetSdk 37 requirement; build-tools and
# platforms are separate packages.
#
# The old `yes | sdkmanager --licenses` step is gone: cmdline-tools v23.0's
# `android sdk` command auto-accepts licenses on unattended install, writing
# ${ANDROID_HOME}/licenses/android-sdk-license on its own. The deprecated
# `sdkmanager` binary still exists in cmdline-tools but prints a deprecation
# warning; `android` (at cmdline-tools/latest/bin/android) is its replacement.
RUN set -eux; \
    "${ANDROID_HOME}/cmdline-tools/latest/bin/android" --sdk="${ANDROID_HOME}" sdk install "platforms/android-37.2"; \
    "${ANDROID_HOME}/cmdline-tools/latest/bin/android" --sdk="${ANDROID_HOME}" sdk install "build-tools/36.0.0"; \
    "${ANDROID_HOME}/cmdline-tools/latest/bin/android" --sdk="${ANDROID_HOME}" sdk install "platform-tools"

# The SDK is installed as root at image build time, but Gradle runs as the
# non-root vscode user and writes licenses and build caches into the SDK
# tree. chown the whole tree to vscode so it works without a runtime fix.
RUN chown -R vscode:vscode "${ANDROID_HOME}"
