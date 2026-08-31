#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q 'version "26'; then
    sudo apt-get update -qq
    sudo apt-get install -y -qq openjdk-26-jdk-headless
fi

readonly JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
export JAVA_HOME="${JAVA_HOME:-${JAVA_HOME_DIR}}"

readonly ANDROID_SDK_DIR="/opt/android-sdk"
sudo mkdir -p "${ANDROID_SDK_DIR}"
sudo chown -R "$(whoami)":"$(whoami)" "${ANDROID_SDK_DIR}"

if [[ ! -d "${ANDROID_SDK_DIR}/cmdline-tools/latest" ]]; then
    mkdir -p "${ANDROID_SDK_DIR}/cmdline-tools"
    cd /tmp
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
    unzip -q -o cmdline-tools.zip -d /tmp/cmdline-temp
    mv /tmp/cmdline-temp/cmdline-tools "${ANDROID_SDK_DIR}/cmdline-tools/latest"
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-temp
fi

export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_DIR}}"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0" >/dev/null

if [[ -f /workspaces/Hail/local.properties ]]; then
    sed -i "s|^sdk.dir=.*|sdk.dir=${ANDROID_HOME}|" /workspaces/Hail/local.properties
else
    echo "sdk.dir=${ANDROID_HOME}" > /workspaces/Hail/local.properties
fi

cat > ~/.android_env << EOF
export JAVA_HOME="${JAVA_HOME}"
export ANDROID_HOME="${ANDROID_HOME}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"
EOF

grep -q "source ~/.android_env" ~/.bashrc || echo 'source ~/.android_env' >> ~/.bashrc

chmod +x /workspaces/Hail/gradlew
