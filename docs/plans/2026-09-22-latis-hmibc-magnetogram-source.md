# LaTiS HMIBC Magnetogram Source Implementation Plan

**Date:** 2026-09-22  
**Design doc:** `docs/specs/2026-09-22-latis-hmibc-magnetogram-source-design.md`  
**Status:** Ready for review

## Overview

Replace only the Android HMI magnetogram acquisition path with a direct LASP LaTiS lookup of the newest `iswa_sdo_aia_hmic_files` record. The repository will parse LaTiS's `timestamp` and ISWA `url`, persist that 2048×2048 HMIBC image through the existing `SolarImage`/Room path, and preserve the current cached-data semantics on failure. The generic fixed-image path remains for LASCO, the existing HMI data-source key remains unchanged so status history is not migrated, and no Cloudflare worker route or other working source is modified.

## Tasks

### Task 1: Define the direct LaTiS HMIBC metadata endpoint

**Files:**  
`core/data/src/main/java/ca/stewark/helioflux/core/data/network/HelioFluxEndpoints.kt`  
`core/data/src/test/java/ca/stewark/helioflux/core/data/network/HelioFluxEndpointsTest.kt`

**Test first:**

Update `HelioFluxEndpointsTest` before production code:

- remove HMI from `protected endpoints use existing worker host`;
- add a focused test named `hmi metadata uses direct LaTiS latest color magnetogram query`;
- assert the new HMI metadata endpoint:
  - starts with `https://lasp.colorado.edu/space-weather-portal/latis/dap/iswa_sdo_aia_hmic_files.json?`;
  - contains `takeRight(1)`;
  - contains `project(time,url)`;
  - does not contain the Cloudflare worker host.

Example assertions:

```kotlin
@Test
fun `hmi metadata uses direct LaTiS latest color magnetogram query`() {
    assertTrue(
        HelioFluxEndpoints.hmiMetadata.startsWith(
            "https://lasp.colorado.edu/space-weather-portal/latis/dap/" +
                "iswa_sdo_aia_hmic_files.json?",
        ),
    )
    assertTrue(HelioFluxEndpoints.hmiMetadata.contains("takeRight(1)"))
    assertTrue(HelioFluxEndpoints.hmiMetadata.contains("project(time,url)"))
    assertFalse(HelioFluxEndpoints.hmiMetadata.contains("workers.dev"))
}
```

Run the focused test and confirm it fails because `hmiMetadata` does not exist yet.

**Implementation:**

In `HelioFluxEndpoints.kt`:

- keep the existing `LASP` base;
- add `val hmiMetadata = "$LASP/iswa_sdo_aia_hmic_files.json?takeRight(1)&project(time,url)"`;
- remove the old Android `val hmi = "$WORKER/hmi/"` because no production caller should use the legacy black-and-white image after this change;
- do not change `donki`, `helioviewer`, `hek`, `lasco`, or `enlil`.

Do not rename or modify the repository's existing HMI `DataSourceKey("worker-hmi")`; retaining that key preserves current status/cache continuity.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.network.HelioFluxEndpointsTest"
```

All endpoint tests pass.

---

### Task 2: Add a minimal LaTiS HMIBC metadata parser

**Files:**  
`core/data/src/main/java/ca/stewark/helioflux/core/data/parser/HmibcMetadataParser.kt`  
`core/data/src/test/java/ca/stewark/helioflux/core/data/parser/HmibcMetadataParserTest.kt`

**Test first:**

Create `HmibcMetadataParserTest.kt` with the observed LaTiS payload:

```json
{
  "iswa_sdo_aia_hmic_files": {
    "samples": [
      {
        "timestamp": "2026-09-22 11:15:00.0",
        "url": "https://iswa.ccmc.gsfc.nasa.gov/iswa_data_tree/observation/solar/sdo/hmi-magnetogram-color_2048x2048/2026/09/20260922_111500_2048_HMIBC.jpg"
      }
    ]
  }
}
```

Add focused tests that require:

1. `parseValidLatestHmibcSample`
   - returns a non-null record;
   - timestamp equals the UTC epoch milliseconds for `2026-09-22T11:15:00Z`;
   - URL exactly equals the returned ISWA HMIBC URL.
2. `emptySamplesReturnNull`
   - `"samples": []` returns null.
3. `invalidTimestampReturnsNull`
   - a sample with an invalid timestamp returns null.
4. `nonHttpsImageUrlReturnsNull`
   - a sample whose URL is not HTTPS returns null.

Run the parser test and confirm it fails because the parser does not exist.

**Implementation:**

Create `HmibcMetadataParser.kt` using the project's existing `kotlinx.serialization.json` parser style.

Keep the helper local to `core:data`, for example:

```kotlin
internal data class HmibcImageRecord(
    val timestampMillis: Long,
    val url: String,
)
```

Implement `HmibcMetadataParser.parse(json: String): HmibcImageRecord?` so it:

- parses the root object;
- reads `iswa_sdo_aia_hmic_files.samples`;
- accepts exactly one sample from the latest-record response;
- reads `timestamp` and `url`;
- normalizes the LaTiS timestamp from `yyyy-MM-dd HH:mm:ss.S` to an ISO local datetime by replacing the date/time separator with `T`;
- interprets that timestamp as UTC;
- rejects malformed timestamps;
- rejects blank or non-HTTPS URLs;
- returns null for an empty/missing sample set.

Do not add a general LaTiS framework or a new core-model type.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.parser.HmibcMetadataParserTest"
```

All parser tests pass.

---

### Task 3: Make magnetogram refresh persist the LaTiS HMIBC URL and timestamp

**Files:**  
`core/data/src/main/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepository.kt`  
`core/data/src/test/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepositoryTest.kt`

**Test first:**

Add a repository test named `magnetogramRefreshPersistsLatestLaTiSHmibcSample`.

Arrange the fake transport so `GET HelioFluxEndpoints.hmiMetadata` returns HTTP 200 with the known-good LaTiS JSON.

Assert after the magnetogram refresh that:

- the stored type is `SolarImageType.Magnetogram`;
- `sourceTimestampMillis` equals `2026-09-22T11:15:00Z`;
- `url` equals the exact 2048×2048 ISWA `HMIBC.jpg` URL;
- the HMI source status records the same observation timestamp as a success.

Extend the fake HTTP transport with a `getStatus` map if needed so GET status can be controlled independently.

Run the repository test and confirm it fails against the current fixed-image/HEAD implementation.

**Implementation:**

In `SolarImageryRepository.kt`:

- import `HmibcMetadataParser`;
- add a focused `suspend fun refreshMagnetogram()`;
- use the existing HMI source key and existing `attempt`, `success`, and `fail` helpers;
- GET `HelioFluxEndpoints.hmiMetadata`;
- require an HTTP 2xx response;
- parse the body with `HmibcMetadataParser`;
- require a non-null record;
- persist:
  `SolarImage(SolarImageType.Magnetogram, record.timestampMillis, record.url).toEntity()`;
- call `success(HMI, record.timestampMillis, now)`;
- on any exception, call the existing `fail(HMI, now, e)`.

Do not HEAD the returned image URL as part of this task; LaTiS metadata is the source of the observation timestamp and the UI's existing image loader remains responsible for loading the returned JPEG.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest.magnetogramRefreshPersistsLatestLaTiSHmibcSample"
```

The focused repository success test passes.

---

### Task 4: Preserve the last successful HMIBC image as Cached on refresh failure

**Files:**  
`core/data/src/test/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepositoryTest.kt`

**Test first:**

Add `failedMagnetogramRefreshRetainsPreviousHmibcAsCached`.

Test sequence:

1. Return a valid LaTiS response and call `refreshMagnetogram()`.
2. Capture the persisted HMIBC image.
3. Change fake GET status for `hmiMetadata` to 500.
4. Call `refreshMagnetogram()` again.
5. Read `repository.image(SolarImageType.Magnetogram).first()`.

Assert:

- state is `RepositoryState.Failure`;
- `retainedData` is the same HMIBC URL/timestamp from step 1;
- freshness is `DataFreshness.Cached`;
- the DAO still contains the successful HMIBC image;
- there was no request to any legacy worker HMI URL.

The test should fail until Task 3's failure path is correct.

**Implementation:**

No new production abstraction is planned. If Task 3's implementation already satisfies this test, make no production change.

If a correction is necessary, change only the magnetogram refresh path so an unsuccessful metadata lookup records status failure without modifying/deleting the existing `SolarImageEntity`.

Do not add a fallback image request.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest.failedMagnetogramRefreshRetainsPreviousHmibcAsCached"
```

The retained-data/Cached regression test passes.

---

### Task 5: Route refreshAll to HMIBC while preserving the LASCO fixed-image path

**Files:**  
`core/data/src/main/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepository.kt`  
`core/data/src/test/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepositoryTest.kt`

**Test first:**

Update/add repository coverage so `refreshAll()` requires:

- one GET of `HelioFluxEndpoints.hmiMetadata`;
- HMI is persisted from the LaTiS response;
- LASCO C2 still persists `HelioFluxEndpoints.lasco + "LATEST/current_c2.gif"`;
- LASCO C3 still persists `HelioFluxEndpoints.lasco + "LATEST/current_c3.gif"`;
- HEK and ENLIL behavior remain as currently tested.

Move the existing `headLastModifiedBecomesNormalizedTimestamp` coverage away from Magnetogram and onto a fixed LASCO image so it continues to verify the generic `refreshImage(type, url)` path without implying HMI still uses HEAD/Last-Modified.

Run `SolarImageryRepositoryTest` and confirm the new `refreshAll()` expectation fails before production routing is changed.

**Implementation:**

Change only the first operation in `refreshAll()`:

```kotlin
refreshMagnetogram()
```

instead of:

```kotlin
refreshImage(SolarImageType.Magnetogram, HelioFluxEndpoints.hmi)
```

Leave the existing generic `refreshImage(type, url)` implementation intact for LASCO, including its HEAD/Last-Modified behavior.

Do not change `refreshRegions()`, `refreshEnlil()`, source ordering, source keys, or failure isolation.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest"
```

All Solar Imagery repository tests pass, including the unchanged LASCO contracts.

---

### Task 6: Run the focused and full local verification gates

**Files:** No planned production changes.

**Test first:**

No new test exists solely for this gate. If verification exposes a defect, add the narrowest failing test before changing production code.

**Implementation / verification:**

Run the focused data tests first:

```bash
./gradlew :core:data:testDebugUnitTest   --tests "ca.stewark.helioflux.core.data.network.HelioFluxEndpointsTest"   --tests "ca.stewark.helioflux.core.data.parser.HmibcMetadataParserTest"   --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest"
```

Then run the repository-wide required gates:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Inspect scope:

```bash
git status --short
git diff --stat
```

Expected implementation files are limited to:

```text
core/data/src/main/java/ca/stewark/helioflux/core/data/network/HelioFluxEndpoints.kt
core/data/src/main/java/ca/stewark/helioflux/core/data/parser/HmibcMetadataParser.kt
core/data/src/main/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepository.kt
core/data/src/test/java/ca/stewark/helioflux/core/data/network/HelioFluxEndpointsTest.kt
core/data/src/test/java/ca/stewark/helioflux/core/data/parser/HmibcMetadataParserTest.kt
core/data/src/test/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepositoryTest.kt
```

No app UI file, worker source, HEK code, active-region code, or unrelated endpoint should be modified.

**Verify:**

All Gradle commands pass and the diff contains only planned files.

---

### Task 7: Commit one implementation batch, push, and monitor Android CI

**Files:** No additional planned source changes.

**Test first:**

No test exists for the push itself. If CI exposes a defect, reproduce it with the narrowest relevant failing test before making a correction.

**Implementation / verification:**

Commit Tasks 1–6 together to minimize GitHub Actions usage.

Suggested commit message:

```text
fix: load current HMIBC magnetogram from LaTiS
```

Push once to `main`.

Actively monitor the resulting Android CI run during the same response, re-checking roughly every 45 seconds while pending.

If CI fails:

1. inspect the failing job/log;
2. reproduce the failure locally where possible;
3. add/fix focused regression coverage first;
4. make the minimum correction;
5. push the correction;
6. continue monitoring until green.

**Verify:**

Android CI completes successfully for the final implementation SHA.

---

### Task 8: Perform the physical Pixel acceptance pass

**Files:** No planned source changes. Any newly observed defect starts the project's bug workflow before production changes.

**Test first:**

No automated test is added solely for manual acceptance.

**Implementation / verification:**

On the physical Pixel:

1. Open Solar Activity with HMI selected.
2. Confirm the base image is the colorized HMIBC product rather than the previous black-and-white HMIB.
3. Confirm the image is current enough for the existing imagery freshness policy.
4. Confirm the image opens in the existing full-screen viewer and pinch/pan behavior is unchanged.
5. Pull to refresh and confirm a newer LaTiS sample can replace the image when available.
6. Exercise a failed/offline refresh and confirm the last successful HMIBC remains visible with `Cached` freshness.
7. Confirm LASCO C2, LASCO C3, ENLIL, charts, and other Solar Activity data still behave as before.
8. Note whether active-region labels are still absent; do not fix them in this task. Route that separately through `$debugprompt` after HMIBC is accepted.

**Verify:**

The HMIBC source behaves as designed on-device with no regression to other Solar Activity sources.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production change was preceded by focused failing coverage
- [ ] HMI metadata is queried directly from LASP LaTiS
- [ ] The dataset is `iswa_sdo_aia_hmic_files`
- [ ] The request returns only the newest `time,url` record
- [ ] The returned ISWA 2048×2048 HMIBC URL is persisted
- [ ] The LaTiS timestamp is persisted as the magnetogram source/observation timestamp
- [ ] Failed/empty/malformed metadata refreshes do not overwrite the previous HMIBC
- [ ] A retained HMIBC is reported as `Cached` after refresh failure
- [ ] No fallback to the legacy black-and-white HMIB image exists
- [ ] Existing HMI source-key continuity is preserved
- [ ] LASCO's existing fixed-image HEAD/Last-Modified path remains functional
- [ ] No Cloudflare worker code is modified
- [ ] No DONKI, Helioviewer, HEK, LASCO, ENLIL, NOAA, or ACE EPAM endpoint behavior is changed
- [ ] No Solar Activity UI, zoom/pan, gallery, full-screen, chart, or active-region overlay behavior is changed
- [ ] Focused core:data tests pass
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:compileDebugAndroidTestKotlin` passes
- [ ] `./gradlew :app:lintDebug :app:assembleDebug` passes
- [ ] No unplanned files are modified
- [ ] Final Android CI is green
- [ ] Physical Pixel acceptance passes
- [ ] Missing active-region labels remain a separate follow-up bug
