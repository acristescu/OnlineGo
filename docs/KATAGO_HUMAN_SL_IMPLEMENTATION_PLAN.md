# KataGo Human SL AI - decision log

## Goal

Replace the home-grown weakening heuristic with KataGo's Human SL network (imitates human
play at a given rank) via the analysis engine's `overrideSettings.humanSLProfile` +
`humanPolicy` sampling.

## Key decisions

- Bundle `katago_human.net` in the APK (~94.5 MB, like the existing net) - no on-demand download.
- Replaces the old sampling logic outright, not an additive toggle - no New Game UI changes.
- Exactly two move-selection strategies (`AiGameViewModel.kt`): `selectHumanMove` (every
  tier but the top) samples `humanPolicy` directly; `selectBestMove` (top tier only) just
  plays `moveInfos[0]`.
- Main net stays `g170e-b15c192` - smaller nets (`b10c128` and below) have documented
  degenerate-opening issues per direct feedback from the KataGo project; no vetted small
  "nbt"-architecture net exists as an alternative.
- DAN_4 uses `rank_4d`, not a higher rank - KataGo's docs warn high-dan Human SL profiles
  aren't backed by real search.
- Native binary self-built for both ABIs from a patched PaooGo/karino2-KataGo build script
  (their fork builds a JNI library, not a subprocess executable - fixed `CMakeLists.txt` to
  build a real executable instead). Verified live on a Pixel 8 Pro for `arm64-v8a`;
  `armeabi-v7a` builds cleanly but is untestable on that (64-bit-only) device.
- `humanPolicy` schema verified empirically against a real running engine: flat array,
  `boardWidth*boardHeight+1` (pass last), row-major, illegal points exactly `-1.0`.

## Follow-up fixes (from real play-testing, after initial implementation)

- AI wasn't passing at game end - added a guard: if the real search already ranks pass
  first, play it outright instead of sampling `humanPolicy`.
- Move generation felt slow - `maxVisits` is now raised dynamically to at least
  `KataGoAnalysisEngine.searchThreads` (the device's CPU core count), which is also baked
  into `katago.cfg` at each engine start (thread pool size is startup-only, not overridable
  per query) so no search thread goes idle.
- AI kept dame-filling instead of passing in clearly hopeless (~0% winrate) positions, even
  with more visits, because pass rarely won the thread-scheduling race to be `moveInfos[0]`
    - added `hopelessPassMove`: below 1% winrate, play pass if it's anywhere in `moveInfos`,
      without needing it to rank first.
- Added an AI resignation offer: below 5% winrate for 3 consecutive turns, ask the user to
  accept/decline instead of continuing to play a lost game silently.
- Reader-thread crash fix: KataGo warning responses weren't always recognized, causing an
  uncaught JSON-parse crash; broadened detection and wrapped parsing in try/catch.

## Rebuilding `libkatago.so`

`app/src/main/jniLibs/{arm64-v8a,armeabi-v7a}/libkatago.so` are self-built subprocess
executables (not KataGo's usual JNI-library form) - needed later, for a newer KataGo
version or a new ABI. Steps:

1. Clone [`acristescu/PaooGo`](https://github.com/acristescu/PaooGo), branch
   `251101a_humansl`. It contains `android/src/main/cpp/katago-executable-patch/CMakeLists.txt`
   and a README section ("Standalone KataGo executable (for OnlineGo)") documenting exactly
   what to change and why.
2. Clone `kaorahi/KataGo`, branch `paoo_251025a`, into `cpp/`.
3. Copy `katago-executable-patch/CMakeLists.txt` over `cpp/CMakeLists.txt`.
4. In `cpp/main.cpp`'s `handleSubcommand()`, remove the `contribute`, `gtp`, and `selfplay`
   dispatch branches (see the PaooGo README for the exact diff).
5. Build with the Android NDK, Eigen (CPU) backend, for each target ABI - produces
   `libkatago.so` as a real executable (entry point set, no JNI exports).
6. Copy the resulting binaries into `app/src/main/jniLibs/<abi>/libkatago.so` here.

## Status

Implemented, tested (unit tests + on-device), shipped.
