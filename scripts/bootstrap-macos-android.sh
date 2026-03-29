#!/bin/zsh

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BREW_BIN="${BREW_BIN:-/opt/homebrew/bin/brew}"
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
JAVA_PACKAGE="${JAVA_PACKAGE:-${JAVA_FORMULA:-temurin@17}}"
JAVA_INSTALL_KIND="${JAVA_INSTALL_KIND:-cask}"
JAVA_VERSION="${JAVA_VERSION:-17}"
ANDROID_API_LEVEL="${ANDROID_API_LEVEL:-36}"
ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-36.0.0}"
ANDROID_NDK_VERSION="${ANDROID_NDK_VERSION:-25.1.8937393}"
ANDROID_CMAKE_VERSION="${ANDROID_CMAKE_VERSION:-3.31.6}"
ANDROID_CMDLINE_TOOLS_VERSION="${ANDROID_CMDLINE_TOOLS_VERSION:-19.0}"
ZPROFILE_FILE="${ZPROFILE_FILE:-$HOME/.zprofile}"

say() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
}

fail() {
  printf '\nERROR: %s\n' "$1" >&2
  exit 1
}

ensure_macos_arm64() {
  [[ "$(uname -s)" == "Darwin" ]] || fail "This bootstrap script only supports macOS."
  [[ "$(uname -m)" == "arm64" ]] || fail "This script is tailored for Apple Silicon (arm64)."
}

ensure_xcode_tools() {
  if ! xcode-select -p >/dev/null 2>&1; then
    fail "Xcode Command Line Tools are missing. Run 'xcode-select --install' and rerun."
  fi
}

ensure_homebrew() {
  if [[ ! -x "$BREW_BIN" ]]; then
    BREW_BIN="$(command -v brew || true)"
  fi
  [[ -x "$BREW_BIN" ]] || fail "Homebrew is not installed. Install it from https://brew.sh first."
}

install_brew_packages() {
  say "Updating Homebrew metadata"
  "$BREW_BIN" update

  say "Installing Java 17 and Android tools"
  if [[ "$JAVA_INSTALL_KIND" == "formula" ]]; then
    "$BREW_BIN" install "$JAVA_PACKAGE"
  else
    "$BREW_BIN" install --cask "$JAVA_PACKAGE"
  fi
  "$BREW_BIN" install android-platform-tools
  "$BREW_BIN" install --cask android-commandlinetools android-studio
}

resolve_java_home() {
  local prefix

  if /usr/libexec/java_home -v "$JAVA_VERSION" >/dev/null 2>&1; then
    /usr/libexec/java_home -v "$JAVA_VERSION"
    return 0
  fi

  if prefix="$("$BREW_BIN" --prefix "$JAVA_PACKAGE" 2>/dev/null)"; then
    if [[ -d "$prefix/libexec/openjdk.jdk/Contents/Home" ]]; then
      printf '%s\n' "$prefix/libexec/openjdk.jdk/Contents/Home"
      return 0
    fi
  fi

  fail "Unable to locate a Java ${JAVA_VERSION} installation after brew install."
}

resolve_sdkmanager() {
  local candidates=(
    "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
    "$SDK_ROOT/cmdline-tools/$ANDROID_CMDLINE_TOOLS_VERSION/bin/sdkmanager"
    "/opt/homebrew/share/android-commandlinetools/cmdline-tools/latest/bin/sdkmanager"
    "/opt/homebrew/share/android-commandlinetools/cmdline-tools/bin/sdkmanager"
    "/usr/local/share/android-commandlinetools/cmdline-tools/latest/bin/sdkmanager"
    "/usr/local/share/android-commandlinetools/cmdline-tools/bin/sdkmanager"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  fail "sdkmanager not found. Open Android Studio once or verify android-commandlinetools installation."
}

install_android_sdk_packages() {
  local java_home sdkmanager
  java_home="$1"
  sdkmanager="$2"

  mkdir -p "$SDK_ROOT"

  say "Accepting Android SDK licenses"
  yes | env JAVA_HOME="$java_home" ANDROID_SDK_ROOT="$SDK_ROOT" "$sdkmanager" --sdk_root="$SDK_ROOT" --licenses >/dev/null

  say "Installing Android SDK packages"
  env JAVA_HOME="$java_home" ANDROID_SDK_ROOT="$SDK_ROOT" "$sdkmanager" --sdk_root="$SDK_ROOT" \
    "platform-tools" \
    "emulator" \
    "platforms;android-${ANDROID_API_LEVEL}" \
    "build-tools;${ANDROID_BUILD_TOOLS}" \
    "cmdline-tools;latest" \
    "ndk;${ANDROID_NDK_VERSION}" \
    "cmake;${ANDROID_CMAKE_VERSION}" \
    "system-images;android-${ANDROID_API_LEVEL};google_apis_playstore;arm64-v8a"
}

ensure_avdmanager() {
  local avdmanager="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
  [[ -x "$avdmanager" ]] || fail "avdmanager not found after cmdline-tools installation."
  printf '%s\n' "$avdmanager"
}

create_debug_avd() {
  local avdmanager="$1"
  local device_name="${ANDROID_AVD_DEVICE:-pixel_8}"
  local avd_name="${ANDROID_AVD_NAME:-OnlineGo-API-${ANDROID_API_LEVEL}}"
  local image="system-images;android-${ANDROID_API_LEVEL};google_apis_playstore;arm64-v8a"

  if "$avdmanager" list avd | grep -q "Name: ${avd_name}\$"; then
    say "AVD '${avd_name}' already exists"
    return 0
  fi

  say "Creating ARM64 emulator '${avd_name}'"
  printf 'no\n' | "$avdmanager" create avd -n "$avd_name" -k "$image" -d "$device_name"
}

write_local_properties() {
  cat > "$PROJECT_ROOT/local.properties" <<EOF
sdk.dir=${SDK_ROOT}
EOF
}

update_zprofile() {
  local java_home="$1"
  local start="# >>> OnlineGo Android env >>>"
  local end="# <<< OnlineGo Android env <<<"
  local tmp
  tmp="$(mktemp)"

  if [[ -f "$ZPROFILE_FILE" ]]; then
    awk -v start="$start" -v end="$end" '
      $0 == start { skip=1; next }
      $0 == end { skip=0; next }
      skip != 1 { print }
    ' "$ZPROFILE_FILE" > "$tmp"
  fi

  {
    cat "$tmp"
    printf '%s\n' "$start"
    printf 'export JAVA_HOME="%s"\n' "$java_home"
    printf 'export ANDROID_SDK_ROOT="%s"\n' "$SDK_ROOT"
    printf 'export ANDROID_HOME="%s"\n' "$SDK_ROOT"
    printf 'export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"\n'
    printf '%s\n' "$end"
  } > "$ZPROFILE_FILE"

  rm -f "$tmp"
}

print_next_steps() {
  cat <<EOF

Bootstrap finished.

Next steps:
  1. Open a new shell or run:
     source "$ZPROFILE_FILE"
  2. If needed, confirm Java:
     java -version
  3. Start Android Studio once so it indexes the SDK:
     open -a "Android Studio"
  4. Optional: start the emulator from terminal:
     emulator -avd "OnlineGo-API-${ANDROID_API_LEVEL}"
  5. Build debug APK:
     ./gradlew assembleDebug
  6. Install/run on a device or emulator:
     ./gradlew installDebug

Notes:
  - This repo already contains app/src/debug/google-services.json for debug builds.
  - Release builds still require app/google-services.json and app/src/release/google-services.json.
EOF
}

main() {
  ensure_macos_arm64
  ensure_xcode_tools
  ensure_homebrew
  install_brew_packages

  local java_home sdkmanager avdmanager
  java_home="$(resolve_java_home)"
  sdkmanager="$(resolve_sdkmanager)"
  install_android_sdk_packages "$java_home" "$sdkmanager"
  avdmanager="$(ensure_avdmanager)"
  create_debug_avd "$avdmanager"
  write_local_properties
  update_zprofile "$java_home"
  print_next_steps
}

main "$@"
