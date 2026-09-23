# HelioFlux Flux Ribbon Launcher Branding Implementation Plan

**Date:** 2026-09-22  
**Design:** `docs/specs/2026-09-22-helioflux-flux-ribbon-launcher-branding-design.md`  
**Scope:** Replace the current Earth/Aurora launcher artwork with the approved **5A — Clean Flow** mark while preserving existing launcher wiring and the pure-black Android system splash.

## Constraints

- Work only in `godwept/HelioFlux-Android`.
- Do not modify the PWA.
- Do not add a custom splash screen, splash animation, startup delay, or startup-state logic.
- Do not change Home UI or the general app theme.
- Keep the existing manifest launcher references and adaptive-icon resource structure unless a verified implementation need requires otherwise.
- Preserve the black `#000000` system splash.
- Follow strict TDD.
- Keep the implementation to one focused code/resource batch and one CI-triggering push where practical.

## Current implementation

The existing launcher infrastructure is already present:

- `AndroidManifest.xml` points to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
- API 26 adaptive icon XML uses `@color/launcher_background` plus `@drawable/ic_launcher_foreground`.
- API 33 adaptive icon XML additionally uses `@drawable/ic_launcher_monochrome`.
- Android 12+ `values-v31/styles.xml` already uses `#000000` for the system splash and `@mipmap/ic_launcher` for its icon.
- `LauncherBrandingTest` already protects the structural launcher and splash wiring.
- The app supports API 24+, so the existing pre-26 launcher fallback must remain valid.

The change should therefore replace artwork, not redesign launcher architecture.

## Task 1 — Strengthen the branding regression contract

**Files:**
- Modify: `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingTest.kt`

### Red

Add focused assertions that distinguish the approved Clean Flow branding from the rejected Earth/Aurora artwork.

The test should verify, without coupling to every vector coordinate:

- foreground resource contains the three approved color families: green/teal, cyan/electric blue, and blue/violet;
- foreground does not contain known Earth/Aurora-specific colors or Earth-style full-disk geometry from the current asset;
- monochrome resource contains the Clean Flow ribbon silhouette structure and no Earth disk;
- launcher background remains pure black;
- existing manifest, adaptive-icon, themed-icon, and system-splash assertions remain intact.

Run:

`./gradlew :app:testDebugUnitTest --tests ca.stewark.helioflux.branding.LauncherBrandingTest`

Confirm the new assertions fail against the current Earth/Aurora resources.

### Green

Do not change production resources in this task. The red result establishes the regression contract for Task 2.

## Task 2 — Replace the launcher foreground with 5A Clean Flow

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify only if needed for faithful vector rendering: `app/src/main/res/values/colors.xml`

Rebuild the approved 5A mark as Android vector artwork suitable for an adaptive icon:

- three interlocking flowing ribbon forms around central negative space;
- upper flow green/teal → cyan;
- lower-left flow cyan → electric blue;
- right flow blue → violet;
- pure black remains the separate adaptive background rather than being baked into the foreground;
- no Earth, Sun, stars, text, border, or enclosing circle;
- keep the full silhouette inside the adaptive-icon safe region;
- use a restrained number of paths/gradients so the mark remains crisp and maintainable.

Prefer native vector gradients where they reproduce the approved appearance reliably. Do not introduce a raster asset unless vector rendering demonstrably cannot preserve the approved mark.

Run the focused branding test and confirm the foreground-related assertions pass.

## Task 3 — Replace the themed-icon silhouette

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_monochrome.xml`

Create a dedicated single-color Clean Flow mask:

- same recognizable three-flow silhouette as the colored mark;
- no grayscale approximation of the colored asset;
- no Earth/disk geometry;
- preserve central negative space and separation between flows sufficiently for Android's themed tinting.

Do not change the existing API 33 `<monochrome>` resource wiring.

Run the focused branding test and confirm all launcher-artwork assertions pass.

## Task 4 — Verify legacy, adaptive, round, themed, and splash wiring

**Files expected to remain unchanged unless verification exposes a defect:**
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml`
- `app/src/main/res/values-v31/styles.xml`

Verify:

- API 24–25 fallback resolves to the new foreground on black;
- API 26+ adaptive icon uses black background + Clean Flow foreground;
- round launcher variant resolves identically through Android's round mask;
- API 33+ themed icon uses the dedicated Clean Flow monochrome drawable;
- Android 12+ splash remains `#000000` and uses the launcher icon;
- no custom splash or app startup behavior was introduced.

Run:

`./gradlew :app:testDebugUnitTest --tests ca.stewark.helioflux.branding.LauncherBrandingTest`

## Task 5 — Full local verification

Run the same checks used by Android CI:

`./gradlew projects`

`./gradlew testDebugUnitTest`

`./gradlew assembleDebugAndroidTest`

`./gradlew lintDebug assembleDebug`

If a connected device is available, also run:

`./gradlew connectedDebugAndroidTest`

Inspect the built icon at launcher scale where practical, paying particular attention to central negative space, clipping under adaptive masks, and themed-icon readability.

Do not make unrelated visual changes during this verification pass.

## Task 6 — Commit, push, and monitor CI

Review the diff and confirm only the planned branding/test resources changed.

Create one focused implementation commit, for example:

`feat: replace launcher icon with Clean Flow branding`

Push once local verification is green.

Actively monitor the resulting Android CI run. Re-check while pending; if it fails, inspect the failure, make the minimum corrective change, push, and continue monitoring until green or until user input is required.

## Physical-device acceptance

After CI is green, verify on the Pixel:

1. Normal launcher icon matches the approved 5A Clean Flow design.
2. The mark is centered and not clipped by the Pixel launcher mask.
3. The icon remains readable at normal launcher size.
4. Themed icons show the intended monochrome Clean Flow silhouette.
5. A cold launch shows the icon on a pure-black Android system splash.
6. Startup transitions directly into the app with no custom splash or added delay.

Physical-device aesthetic approval remains the final check for the launcher artwork.
