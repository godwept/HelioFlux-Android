# HelioFlux Native Android Implementation Plan

**Date:** 2026-09-19  
**Design doc:** docs/specs/2026-09-19-helioflux-android-native-conversion-design.md  
**Status:** Ready for review

## Overview

Build HelioFlux as a fully native Android application in Kotlin, Jetpack Compose, and Material 3 while leaving `godwept/HelioFlux-web` unchanged as the reference implementation. The work starts with a small multi-module project and tested data/parsing foundations, then adds rolling Room-backed caching, cached-first repositories, adaptive Compose screens, a native SceneView/Filament Aurora Globe, conservative WorkManager alerts, and three home-screen widgets. Every implementation task below is intentionally small enough to complete independently and follows test-first development.

## Planning Baseline

Use these implementation choices unless the plan is revised before execution:

- Application ID / namespace: `ca.stewark.helioflux`.
- Minimum SDK: 24.
- Compile SDK / target SDK: 36.
- Android Gradle Plugin: 9.4.0.
- Kotlin: 2.4.10.
- Compose BOM: 2026.09.00.
- Material 3: stable release supplied by the BOM.
- Material 3 Adaptive: 1.3.0.
- Networking: OkHttp 5.5.0 plus kotlinx.serialization 1.11.0; use raw text parsing for NOAA text/listing formats.
- Persistence: Room 2.8.5.
- Image loading/cache: Coil 3.6.3.
- Charts: Vico 3.1.x Compose/Material 3.
- 3D globe: SceneView 4.37.0 backed by Filament.
- Background work: WorkManager 2.11.2.
- Widgets: Glance 1.2.0 for data widgets; Sun Hero uses a static Glance baseline plus a separately validated RemoteViews animation enhancement.
- Dependency injection: manual `AppContainer`; do not add Hilt/Koin unless later complexity proves it necessary.
- Initial geomagnetic alert threshold: entering Kp >= 5, with a new alert on escalation to a higher integer Kp band.
- Initial flare alert threshold: new M-class or X-class flare events.
- NOAA direct base: `https://services.swpc.noaa.gov`.
- Existing Cloudflare base retained where needed: `https://helioflux-api-proxy.mathew-stewart.workers.dev/api`.

## Reference PWA Sources

Use these files from `godwept/HelioFlux-web` as behavioral references while implementing, but do not modify them:

- `src/App.jsx`
- `src/components/SolarHero.jsx`
- `src/components/Carousel.jsx`
- `src/components/SpaceWeather.jsx`
- `src/components/SolarActivity.jsx`
- `src/components/AuroraGlobe.jsx`
- `src/components/LineChart.jsx`
- `src/components/BarChart.jsx`
- `src/services/spaceWeather.js`
- `src/services/solarActivity.js`
- `src/services/helioviewer.js`
- `src/services/news.js`
- `workers/api-proxy.js`
- `AI_REF.txt`

## Tasks

### Phase 1 — Project Bootstrap

### Task 1: Create root Gradle project
**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`

**Test first:** Add a minimal Gradle configuration check by running `./gradlew tasks`; it should fail before the wrapper/project exists.

**Implementation:** Create the Kotlin DSL root project named `HelioFlux`, repositories `google()` and `mavenCentral()`, version catalog entries for the baseline libraries above, and Android/Kotlin plugin aliases. Enable AndroidX and Kotlin code style in `gradle.properties`.

**Verify:** Run `./gradlew tasks`; Gradle configures successfully.

---

### Task 2: Add Gradle wrapper
**Files:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`

**Test first:** Run `./gradlew --version`; it should fail because the wrapper is absent.

**Implementation:** Generate a wrapper compatible with AGP 9.4.0 and commit only wrapper files.

**Verify:** Run `./gradlew --version`; wrapper starts successfully.

---

### Task 3: Declare project modules
**Files:** `settings.gradle.kts`

**Test first:** Add includes for `:app`, `:core:model`, `:core:data`, `:core:database`, `:feature:globe`, `:feature:widgets`, and `:feature:alerts`; Gradle should fail until modules exist.

**Implementation:** Add the seven module includes exactly as designed.

**Verify:** Run `./gradlew projects` after Task 4; all seven modules appear.

---

### Task 4: Create empty module build files
**Files:** `app/build.gradle.kts`, `core/model/build.gradle.kts`, `core/data/build.gradle.kts`, `core/database/build.gradle.kts`, `feature/globe/build.gradle.kts`, `feature/widgets/build.gradle.kts`, `feature/alerts/build.gradle.kts`

**Test first:** Run `./gradlew projects`; it should fail because included module directories/build files do not exist.

**Implementation:** Create Android application/library modules with namespace prefixes under `ca.stewark.helioflux`, minSdk 24, compileSdk 36, Java/Kotlin 17, and Compose only where UI is required.

**Verify:** Run `./gradlew projects`; all modules configure successfully.

---

### Task 5: Create app manifest and launch activity shell
**Files:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/ca/stewark/helioflux/MainActivity.kt`

**Test first:** Add `app/src/test/java/ca/stewark/helioflux/MainActivityPackageTest.kt` asserting the expected package constant or class package is `ca.stewark.helioflux`.

**Implementation:** Add INTERNET permission, launcher activity, edge-to-edge setup, and a minimal Compose root that renders `HelioFlux`. Do not add notification or location permissions yet.

**Verify:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

---

### Task 6: Add base test dependencies
**Files:** `gradle/libs.versions.toml`, each module `build.gradle.kts`

**Test first:** Add an empty JUnit test in each non-UI module so test compilation initially fails for missing dependencies.

**Implementation:** Add JUnit, kotlinx-coroutines-test, Room testing, MockWebServer, Compose UI testing, and Robolectric only where required later.

**Verify:** Run `./gradlew testDebugUnitTest`; all placeholder tests compile and pass.

---

## Phase 2 — Shared Domain Models

### Task 7: Add source/freshness models
**Files:** `core/model/src/main/java/ca/stewark/helioflux/core/model/DataFreshness.kt`, `DataSourceKey.kt`, `SourceMetadata.kt`; test counterparts

**Test first:** Test that `DataFreshness` supports `Fresh`, `Delayed`, and `Cached`, and that `SourceMetadata` keeps observation and fetched timestamps separate.

**Implementation:** Add immutable data classes/enums only; no freshness calculation yet.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 8: Add solar-wind models
**Files:** `core/model/.../SolarWindMag.kt`, `SolarWindPlasma.kt`; tests

**Test first:** Construct representative magnetic/plasma samples and assert nullable/number fields preserve values.

**Implementation:** Add timestamped domain models for Bx, By, Bz, Bt, density, speed, and temperature.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 9: Add Kp, GOES magnetometer, and hemispheric-power models
**Files:** `core/model/.../KpSample.kt`, `GoesMagSample.kt`, `HemisphericPowerSample.kt`; tests

**Test first:** Verify timestamps and nullable primary/secondary GOES values can represent arcjet gaps.

**Implementation:** Add the three immutable domain models.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 10: Add aurora models
**Files:** `core/model/.../AuroraPoint.kt`, `AuroraSnapshot.kt`; tests

**Test first:** Verify a snapshot holds observation time, forecast time, and filtered points.

**Implementation:** Add `AuroraPoint(latitude, longitude, intensity)` and snapshot metadata.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 11: Add forecast models
**Files:** `core/model/.../ForecastSection.kt`; tests

**Test first:** Verify section key/title/summary/forecast/issue text are represented exactly.

**Implementation:** Add a model matching the four PWA forecast sections.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 12: Add solar-activity event models
**Files:** `core/model/.../FlareEvent.kt`, `CmeEvent.kt`, `FlareProbabilities.kt`; tests

**Test first:** Verify optional region/location/direction/angle fields can be absent without sentinel values.

**Implementation:** Add event/probability data classes.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 13: Add X-ray and ACE EPAM models
**Files:** `core/model/.../XrayFluxSample.kt`, `AceEpamSample.kt`; tests

**Test first:** Verify nullable channels are preserved so missing data creates chart gaps rather than zeroes.

**Implementation:** Add four GOES X-ray channels and five ACE EPAM channels matching the PWA.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

### Task 14: Add imagery and active-region models
**Files:** `core/model/.../SolarImageType.kt`, `SolarImage.kt`, `ActiveRegion.kt`, `EnlilFrame.kt`; tests

**Test first:** Verify image type, source timestamp, URL, active-region helioprojective coordinates, and ENLIL run/frame timestamps.

**Implementation:** Add immutable models without Android graphics types.

**Verify:** Run `./gradlew :core:model:testDebugUnitTest`.

---

## Phase 3 — Network Foundation and Pure Parsers

### Task 15: Add endpoint configuration
**Files:** `core/data/src/main/java/ca/stewark/helioflux/core/data/network/HelioFluxEndpoints.kt`; test

**Test first:** Assert direct NOAA URLs resolve under `services.swpc.noaa.gov` and protected/proxy URLs resolve under the existing Cloudflare worker.

**Implementation:** Define direct NOAA endpoints for RTSW magnetic/plasma, Kp, GOES magnetometer, GOES X-ray, OVATION, hemispheric power, ACE EPAM and forecast discussion where direct access is sufficient; define Cloudflare endpoints for DONKI, Helioviewer, HEK, HMI, LASCO, and ENLIL.

**Verify:** Run `./gradlew :core:data:testDebugUnitTest --tests "*HelioFluxEndpointsTest"`.

---

### Task 16: Add HTTP transport abstraction
**Files:** `core/data/.../network/HttpTransport.kt`, `OkHttpTransport.kt`; tests

**Test first:** With MockWebServer, assert GET returns status/body/headers and HEAD exposes `Last-Modified`.

**Implementation:** Add a small suspend-based transport abstraction around OkHttp. Do not leak OkHttp response types outside the network package.

**Verify:** Run `./gradlew :core:data:testDebugUnitTest --tests "*OkHttpTransportTest"`.

---

### Task 17: Configure shared JSON parser
**Files:** `core/data/.../network/JsonConfig.kt`; test

**Test first:** Verify unknown fields are ignored and numeric JSON remains accessible for mixed NOAA payloads.

**Implementation:** Configure kotlinx.serialization `Json` with `ignoreUnknownKeys = true` and no lenient coercions that would hide malformed required values.

**Verify:** Run `./gradlew :core:data:testDebugUnitTest --tests "*JsonConfigTest"`.

---

### Task 18: Port magnetic-field parser
**Files:** `core/data/.../parser/MagneticFieldParser.kt`, `src/test/resources/fixtures/rtsw_mag_array.json`, `rtsw_mag_object.json`, test

**Test first:** Cover both array-of-arrays and object formats from the PWA, valid timestamp conversion, and invalid timestamp rejection.

**Implementation:** Port `parseMagneticFieldData` behavior to pure Kotlin.

**Verify:** Run `./gradlew :core:data:testDebugUnitTest --tests "*MagneticFieldParserTest"`.

---

### Task 19: Port plasma parser
**Files:** `core/data/.../parser/PlasmaParser.kt`, fixtures, test

**Test first:** Verify array/object formats, invalid timestamps, and rejection of rows where both speed and density are zero.

**Implementation:** Port `parsePlasmaData` behavior.

**Verify:** Run the parser test class.

---

### Task 20: Port planetary Kp parser and status mapping
**Files:** `core/data/.../parser/KpParser.kt`, `core/model/.../KpStatus.kt`, tests

**Test first:** Cover PWA threshold boundaries: quiet below 3, unsettled from 3, minor from 5, moderate from 6, strong from 7, severe from 9.

**Implementation:** Parse `time_tag` and `Kp`; add a pure status mapper used by UI and alerts.

**Verify:** Run Kp parser/model tests.

---

### Task 21: Port GOES magnetometer parser
**Files:** `core/data/.../parser/GoesMagnetometerParser.kt`, fixtures, test

**Test first:** Verify satellite labels come from instrument-sources, secondary data merges by `time_tag`, and `arcjet_flag` produces null chart gaps.

**Implementation:** Port the PWA merge behavior exactly.

**Verify:** Run `*GoesMagnetometerParserTest`.

---

### Task 22: Port OVATION parser/filter
**Files:** `core/data/.../parser/OvationParser.kt`, fixture, test

**Test first:** Verify observation/forecast times, intensity floor >=5, and thinning of intensity 5–8 points to even longitudes.

**Implementation:** Port the PWA reduction pipeline and return `AuroraSnapshot`.

**Verify:** Run `*OvationParserTest`.

---

### Task 23: Port hemispheric-power parser
**Files:** `core/data/.../parser/HemisphericPowerParser.kt`, fixture, test

**Test first:** Cover comments/blanks, timestamp shape validation, invalid numeric values, and North/South values.

**Implementation:** Port the text parser as a pure function.

**Verify:** Run `*HemisphericPowerParserTest`.

---

### Task 24: Port forecast-discussion parser
**Files:** `core/data/.../parser/ForecastDiscussionParser.kt`, fixture, test

**Test first:** Verify `:Issued:`, comment stripping, soft-line joining, and extraction of Solar Activity, Energetic Particle, Solar Wind, and Geospace summary/forecast blocks.

**Implementation:** Port `normalizeDiscussionText` and `parseForecastDiscussion`.

**Verify:** Run `*ForecastDiscussionParserTest`.

---

### Task 25: Port flare-probability parser
**Files:** `core/data/.../parser/FlareProbabilityParser.kt`, fixture, test

**Test first:** Verify it enters the Class C table and takes maximum C/M/X percentages across regions.

**Implementation:** Port PWA logic without adding new interpretation.

**Verify:** Run `*FlareProbabilityParserTest`.

---

### Task 26: Port GOES X-ray parser
**Files:** `core/data/.../parser/XrayFluxParser.kt`, fixture, test

**Test first:** Verify GOES-18/19 short and long channels merge by timestamp, non-positive flux becomes null, and results sort ascending.

**Implementation:** Port `parseXraySeries`.

**Verify:** Run `*XrayFluxParserTest`.

---

### Task 27: Port DONKI flare parser
**Files:** `core/data/.../parser/DonkiFlareParser.kt`, fixture, test

**Test first:** Verify peak/begin time fallback, region modulo 10000 behavior, GOES observatory extraction, location, ordering, and malformed-event rejection.

**Implementation:** Normalize DONKI FLR JSON into `FlareEvent`.

**Verify:** Run `*DonkiFlareParserTest`.

---

### Task 28: Port DONKI CME parser
**Files:** `core/data/.../parser/DonkiCmeParser.kt`, fixture, test

**Test first:** Verify timestamp fallback, speed, half-angle, N/S E/W direction formatting, link, type, ordering, and malformed-event rejection.

**Implementation:** Normalize DONKI CMEAnalysis JSON.

**Verify:** Run `*DonkiCmeParserTest`.

---

### Task 29: Port ACE EPAM parser
**Files:** `core/data/.../parser/AceEpamParser.kt`, fixture, test

**Test first:** Verify comments are skipped, date/time is parsed, required columns are read, and values <= -1.0e5 become null.

**Implementation:** Port the PWA parser and preserve the 72-hour filtering as a separate function.

**Verify:** Run `*AceEpamParserTest`.

---

### Task 30: Port HEK active-region parser
**Files:** `core/data/.../parser/ActiveRegionParser.kt`, fixture, test

**Test first:** Verify invalid/missing NOAA numbers and coordinates are ignored, points outside +/-1000 arcsec are ignored, duplicates use the last region, and leading `1` in 5+ digit labels is trimmed.

**Implementation:** Port HEK normalization into `ActiveRegion`.

**Verify:** Run `*ActiveRegionParserTest`.

---

### Task 31: Port ENLIL listing parser
**Files:** `core/data/.../parser/EnlilListingParser.kt`, fixture, test

**Test first:** Verify file extraction, run grouping, timestamp parsing, newest-run selection, sorting, and downsampling to at most about 48 frames.

**Implementation:** Port `fetchEnlilFrames` parsing as pure functions that accept directory HTML/text.

**Verify:** Run `*EnlilListingParserTest`.

---

### Task 32: Port Helioviewer frame-selection logic
**Files:** `core/data/.../helioviewer/HelioviewerFramePlanner.kt`; test

**Test first:** Verify 60 requested sample times span 15 hours at 15-minute intervals, duplicate image IDs are removed, and latest imagery older than six hours is rejected.

**Implementation:** Separate frame-time planning/deduplication/staleness rules from network calls.

**Verify:** Run `*HelioviewerFramePlannerTest`.

---

### Task 33: Add Helioviewer API client
**Files:** `core/data/.../helioviewer/HelioviewerApi.kt`; MockWebServer test

**Test first:** Verify getClosestImage query parameters and download-image URL width=512/type=png.

**Implementation:** Call the existing Cloudflare `/api/helioviewer` routes and deserialize image id/date metadata.

**Verify:** Run `*HelioviewerApiTest`.

---

## Phase 4 — Room Cache and Freshness Metadata

### Task 34: Create Room database shell
**Files:** `core/database/.../HelioFluxDatabase.kt`, `DatabaseConverters.kt`; instrumented/in-memory test

**Test first:** Open an in-memory database and assert it initializes/closes.

**Implementation:** Add Room database version 1 and type conversion required for timestamps/enums.

**Verify:** Run `./gradlew :core:database:testDebugUnitTest` or the module's Robolectric database test.

---

### Task 35: Add source-status entity and DAO
**Files:** `core/database/.../entity/DataSourceStatusEntity.kt`, `dao/DataSourceStatusDao.kt`; test

**Test first:** Insert/upsert/read status by `DataSourceKey`.

**Implementation:** Store observation, fetch, last-success, last-attempt, and last-error flags/timestamps.

**Verify:** Run `*DataSourceStatusDaoTest`.

---

### Task 36: Add magnetic and plasma entities/DAOs
**Files:** `SolarWindMagEntity.kt`, `SolarWindPlasmaEntity.kt`, corresponding DAOs; tests

**Test first:** Insert samples, query ascending over a time window, and delete rows older than cutoff.

**Implementation:** Use timestamp primary keys and indexed timestamp columns.

**Verify:** Run DAO tests.

---

### Task 37: Add Kp, GOES magnetometer, and hemispheric-power entities/DAOs
**Files:** three entities, three DAOs; tests

**Test first:** Cover upsert, range query, nullable GOES channels, and retention deletion.

**Implementation:** Map directly to corresponding domain samples.

**Verify:** Run DAO tests.

---

### Task 38: Add X-ray and ACE EPAM entities/DAOs
**Files:** `XrayFluxEntity.kt`, `AceEpamEntity.kt`, DAOs; tests

**Test first:** Verify nullable channel persistence and 72-hour range query.

**Implementation:** Add timestamp-indexed time-series tables.

**Verify:** Run DAO tests.

---

### Task 39: Add flare/CME event entities and DAOs
**Files:** `FlareEventEntity.kt`, `CmeEventEntity.kt`, DAOs; tests

**Test first:** Upsert by stable event id, query newest-first, and purge older than retention cutoff.

**Implementation:** Preserve optional metadata.

**Verify:** Run DAO tests.

---

### Task 40: Add forecast entity and DAO
**Files:** `ForecastSectionEntity.kt`, DAO; test

**Test first:** Upsert/read the four sections and keep issue text.

**Implementation:** Key by section key so a refresh replaces each section atomically.

**Verify:** Run `*ForecastSectionDaoTest`.

---

### Task 41: Add aurora snapshot entities and DAO
**Files:** `AuroraSnapshotEntity.kt`, `AuroraPointEntity.kt`, DAO; test

**Test first:** Store a snapshot plus points transactionally, read latest complete snapshot, and delete older snapshots without deleting current data mid-transaction.

**Implementation:** Use observation timestamp as snapshot key and child rows keyed by snapshot+lat+lon.

**Verify:** Run `*AuroraDaoTest`.

---

### Task 42: Add imagery metadata/active-region/ENLIL entities and DAOs
**Files:** `SolarImageEntity.kt`, `ActiveRegionEntity.kt`, `EnlilFrameEntity.kt`, DAOs; tests

**Test first:** Verify latest image metadata, current active-region replacement, and ordered ENLIL frames.

**Implementation:** Store metadata only; image bytes remain in Coil/disk cache.

**Verify:** Run DAO tests.

---

### Task 43: Add database domain mappers
**Files:** `core/database/.../mapper/*.kt`; tests

**Test first:** Round-trip one representative row for each entity/domain pair and assert nullable fields/timestamps survive.

**Implementation:** Keep Room entities out of `core:model` and expose pure mapping extensions.

**Verify:** Run `./gradlew :core:database:testDebugUnitTest`.

---

### Task 44: Add retention policy
**Files:** `core/database/.../RetentionPolicy.kt`; test

**Test first:** Verify normal space-weather series keep at least 48 hours plus safety margin, 72-hour Solar Activity series keep at least 72 hours plus safety margin, event data is retained long enough to support the displayed 72-hour lists, and only the latest few OVATION snapshots are kept.

**Implementation:** Define explicit cutoff durations used by repository cleanup calls.

**Verify:** Run `*RetentionPolicyTest`.

---

### Task 45: Add freshness policy
**Files:** `core/data/.../freshness/FreshnessPolicy.kt`, `FreshnessEvaluator.kt`; tests

**Test first:** Verify: successful recent fetch = Fresh; age beyond source-specific fresh window = Delayed; failed/offline refresh with cache = Cached; no cache remains a separate unavailable condition. Include explicit policies for realtime NOAA, OVATION, Helioviewer, forecast discussion, DONKI/events, and imagery metadata.

**Implementation:** Base policies on actual source/update cadence and PWA refresh/staleness behavior; keep the table centralized and testable.

**Verify:** Run `*FreshnessEvaluatorTest`.

---

## Phase 5 — Cached-First Repositories

### Task 46: Add repository result contract
**Files:** `core/data/.../repository/RepositoryState.kt`; test

**Test first:** Verify states can represent cached data plus freshness, loading with no data, source-specific failure with retained data, and empty-valid data.

**Implementation:** Add a small generic state model; do not embed UI strings.

**Verify:** Run `*RepositoryStateTest`.

---

### Task 47: Add SpaceWeatherRepository magnetic refresh
**Files:** `core/data/.../repository/SpaceWeatherRepository.kt`, implementation/test

**Test first:** Fake DAO emits cached magnetic samples immediately; refresh fetches/parses/validates and replaces/upserts; failed parse leaves cache unchanged and marks source failed.

**Implementation:** Implement only magnetic data in this task.

**Verify:** Run `*SpaceWeatherRepositoryMagTest`.

---

### Task 48: Add plasma refresh
**Files:** same repository/test

**Test first:** Mirror Task 47 for plasma, including zero-row validation.

**Implementation:** Add plasma flow/refresh without touching other sources.

**Verify:** Run plasma-focused repository tests.

---

### Task 49: Add Kp refresh
**Files:** same repository/test

**Test first:** Cached Kp emits first; valid refresh persists; malformed payload preserves cache.

**Implementation:** Add Kp flow/refresh.

**Verify:** Run Kp repository tests.

---

### Task 50: Add GOES magnetometer refresh
**Files:** same repository/test

**Test first:** Verify labels and null arcjet gaps survive persistence.

**Implementation:** Fetch instrument sources plus primary/secondary feeds and store normalized series.

**Verify:** Run GOES repository tests.

---

### Task 51: Add hemispheric-power refresh
**Files:** same repository/test

**Test first:** Verify invalid response does not wipe cached power history.

**Implementation:** Add direct NOAA text fetch and persistence.

**Verify:** Run hemispheric repository tests.

---

### Task 52: Add AuroraRepository
**Files:** `core/data/.../repository/AuroraRepository.kt`, implementation/test

**Test first:** Latest cached snapshot emits immediately; valid OVATION refresh stores a complete new snapshot; failure preserves last snapshot and changes freshness only.

**Implementation:** Add OVATION fetch/parse/persist transaction.

**Verify:** Run `*AuroraRepositoryTest`.

---

### Task 53: Add ForecastRepository
**Files:** `core/data/.../repository/ForecastRepository.kt`, implementation/test

**Test first:** Cached four sections emit first; valid refresh replaces them; malformed response does not wipe cache.

**Implementation:** Fetch NOAA discussion text and persist parsed sections.

**Verify:** Run `*ForecastRepositoryTest`.

---

### Task 54: Add SolarActivityRepository flare probabilities
**Files:** `core/data/.../repository/SolarActivityRepository.kt`, implementation/test

**Test first:** Verify parsed probabilities are exposed with source metadata and failure leaves previous data.

**Implementation:** Add probability source only.

**Verify:** Run focused test.

---

### Task 55: Add GOES X-ray repository path
**Files:** same repository/test

**Test first:** Cached 72-hour series emits first; combined primary/secondary refresh persists normalized samples.

**Implementation:** Add X-ray refresh and retention.

**Verify:** Run focused test.

---

### Task 56: Add DONKI flare repository path
**Files:** same repository/test

**Test first:** Verify 72-hour start/end query, stable-id upsert, newest-first read, and empty array is treated as valid empty data.

**Implementation:** Use Cloudflare DONKI endpoint so API key remains server-side.

**Verify:** Run focused test.

---

### Task 57: Add DONKI CME repository path
**Files:** same repository/test

**Test first:** Verify `mostAccurateOnly=true`, persistence, ordering, and valid empty result.

**Implementation:** Add CME refresh.

**Verify:** Run focused test.

---

### Task 58: Add ACE EPAM repository path
**Files:** same repository/test

**Test first:** Verify 72-hour filtering, null sentinel preservation, and cache retention.

**Implementation:** Fetch the NOAA ACE text endpoint selected in endpoint config.

**Verify:** Run focused test.

---

### Task 59: Add SolarImageryRepository metadata
**Files:** `core/data/.../repository/SolarImageryRepository.kt`, implementation/test

**Test first:** Mock HEAD requests for HMI/LASCO timestamps, HEK JSON, and ENLIL listing; verify normalized metadata/regions/frames persist independently when one source fails.

**Implementation:** Use Cloudflare for HMI/LASCO/HEK/ENLIL.

**Verify:** Run `*SolarImageryRepositoryTest`.

---

### Task 60: Add SolarHeroRepository
**Files:** `core/data/.../repository/SolarHeroRepository.kt`, implementation/test

**Test first:** Verify cached frame metadata emits first, latest-image staleness >6h returns a source error without clearing usable prior cache, and deduplicated frame URLs are persisted.

**Implementation:** Use `HelioviewerApi` and `HelioviewerFramePlanner`.

**Verify:** Run `*SolarHeroRepositoryTest`.

---

### Task 61: Add repository-wide cleanup calls
**Files:** repository implementations/tests

**Test first:** Seed rows older/newer than retention cutoffs and verify successful refresh triggers only relevant cleanup.

**Implementation:** Call DAO delete-before methods after successful persistence, never before.

**Verify:** Run repository test suite.

---

### Task 62: Add manual AppContainer
**Files:** `app/src/main/java/ca/stewark/helioflux/AppContainer.kt`, `HelioFluxApplication.kt`, manifest; test

**Test first:** Build a test container with fake transport/database and assert repositories are replaceable through interfaces.

**Implementation:** Construct OkHttp transport, Room database, repositories, Coil ImageLoader configuration, and expose interfaces. Register `HelioFluxApplication`.

**Verify:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

---

## Phase 6 — Shared UI Foundation

### Task 63: Implement HelioFlux Material theme
**Files:** `app/.../ui/theme/Color.kt`, `Type.kt`, `Theme.kt`; screenshot/Compose test

**Test first:** Compose test verifies dark theme root and semantic brand colors are available.

**Implementation:** Define near-black surfaces, solar orange, cyan/blue data colors, warning colors, and restrained typography. Do not enable Material dynamic color by default because it would dilute the fixed HelioFlux identity.

**Verify:** Run theme UI test.

---

### Task 64: Add navigation destinations
**Files:** `app/.../ui/navigation/HelioFluxDestination.kt`, test

**Test first:** Verify exactly Home, Space Weather, and Solar Activity destinations with stable routes and labels.

**Implementation:** Add sealed/enum destination model.

**Verify:** Run unit test.

---

### Task 65: Add adaptive navigation shell
**Files:** `app/.../ui/HelioFluxApp.kt`, navigation test

**Test first:** Under compact width assert bottom navigation is present; under expanded width assert navigation rail is present; selected destination survives recomposition.

**Implementation:** Use Material 3 adaptive window information and one shared navigation state.

**Verify:** Run `./gradlew :app:connectedDebugAndroidTest --tests "*AdaptiveNavigationTest"`.

---

### Task 66: Add freshness indicator component
**Files:** `app/.../ui/components/FreshnessIndicator.kt`, UI test

**Test first:** Verify dot is always visible, text appears on status change, text hides after the configured delay, tap reveals text again, and semantics contain full status even while text is hidden.

**Implementation:** Add animated text visibility; keep timing injectable/testable.

**Verify:** Run focused Compose UI test.

---

### Task 67: Add generic section state components
**Files:** `app/.../ui/components/SectionState.kt`; UI test

**Test first:** Verify loading-without-cache, inline error, empty-valid, and content states render independently.

**Implementation:** Add reusable small composables only; do not create a global full-screen error component.

**Verify:** Run focused UI test.

---

### Task 68: Add shared chart wrapper
**Files:** `app/.../ui/components/HelioFluxLineChart.kt`, `HelioFluxBarChart.kt`; UI/unit tests

**Test first:** Verify line wrapper accepts multiple nullable series and reference lines; bar wrapper maps Kp values/status labels without crashing on empty data.

**Implementation:** Wrap Vico so screens do not depend directly on detailed chart configuration.

**Verify:** Run app unit/UI tests.

---

### Task 69: Add timeframe filtering utility
**Files:** `app/.../ui/spaceweather/Timeframe.kt`, `TimeframeFilter.kt`; test

**Test first:** Given fixed `now`, verify 1h/3h/12h/2d filters include exact boundary samples and exclude older samples.

**Implementation:** Add pure filtering logic shared by Space Weather charts.

**Verify:** Run `*TimeframeFilterTest`.

---

## Phase 7 — Home Dashboard

### Task 70: Add HomeViewModel cached-first state
**Files:** `app/.../ui/home/HomeViewModel.kt`, test

**Test first:** Fake repositories provide cached Sun/conditions/forecast immediately; verify state updates independently as refreshes complete or fail.

**Implementation:** Combine repository flows without making network calls from the ViewModel.

**Verify:** Run `*HomeViewModelTest`.

---

### Task 71: Add native Sun frame player state
**Files:** `app/.../ui/home/SolarFramePlayerState.kt`; test

**Test first:** Verify frame advances at target cadence, wraps, pauses when disabled/not visible, and reset returns first frame.

**Implementation:** Keep timing/state logic separate from Compose drawing.

**Verify:** Run `*SolarFramePlayerStateTest`.

---

### Task 72: Add Sun Hero composable
**Files:** `app/.../ui/home/SolarHero.kt`; Compose test

**Test first:** Verify cached frame renders, loading/error overlays do not erase the last good frame, and a frame-index change swaps images.

**Implementation:** Use Coil-preloaded disk/memory cache and crossfade between image frames.

**Verify:** Run focused UI test.

---

### Task 73: Add Sun Hero pinch/pan/reset gestures
**Files:** `SolarHero.kt`; UI test

**Test first:** Verify scale clamps to the designed range, pan is disabled at scale 1, and double tap resets transform.

**Implementation:** Use Compose transformable/pointer input; keep gesture state local/saveable.

**Verify:** Run focused UI test.

---

### Task 74: Add current-conditions dashboard card
**Files:** `app/.../ui/home/CurrentConditions.kt`; UI test

**Test first:** Verify Kp, Bz, speed, flare activity, and geomagnetic/aurora summary appear from state and metric taps invoke the expected destination callback.

**Implementation:** Create compact HelioFlux-styled metric cards/chips.

**Verify:** Run focused UI test.

---

### Task 75: Add forecast summary cards
**Files:** `app/.../ui/home/ForecastCards.kt`; UI test

**Test first:** Verify all four parsed sections render, summary/forecast expansion works, and issue text is shown without requiring live data.

**Implementation:** Use native expandable cards, not the PWA DOM structure.

**Verify:** Run focused UI test.

---

### Task 76: Add adaptive Home layout
**Files:** `app/.../ui/home/HomeScreen.kt`; UI test

**Test first:** Compact width stacks Sun then conditions; expanded width places Sun and conditions side-by-side; forecasts remain below.

**Implementation:** Compose adaptive layout based on available width.

**Verify:** Run compact/expanded UI tests.

---

## Phase 8 — Space Weather Screen

### Task 77: Add SpaceWeatherViewModel
**Files:** `app/.../ui/spaceweather/SpaceWeatherViewModel.kt`; test

**Test first:** Verify combined cached state includes current Bz/speed/density, selected timeframe, chart series, Kp, hemispheric power, GOES magnetometer, Aurora snapshot, and per-source freshness.

**Implementation:** Combine repository flows and expose a single immutable screen state.

**Verify:** Run `*SpaceWeatherViewModelTest`.

---

### Task 78: Add timeframe selector
**Files:** `app/.../ui/spaceweather/TimeframeSelector.kt`; UI test

**Test first:** Verify four choices, active style, callback, and state restoration across recreation.

**Implementation:** Add Material segmented-style controls matching HelioFlux colors.

**Verify:** Run focused UI test.

---

### Task 79: Add solar-wind metric badges
**Files:** `app/.../ui/spaceweather/SolarWindMetrics.kt`; UI test

**Test first:** Verify latest non-empty Bz, speed, and density values and placeholder behavior.

**Implementation:** Native metric row/cards.

**Verify:** Run focused UI test.

---

### Task 80: Add Bz/Bt chart card
**Files:** `app/.../ui/spaceweather/SpaceWeatherCharts.kt`; UI test

**Test first:** Verify Bz and Bt series plus zero reference line receive filtered timeframe data.

**Implementation:** Add the first shared chart card.

**Verify:** Run focused UI test.

---

### Task 81: Add density/speed/temperature chart cards
**Files:** `SpaceWeatherCharts.kt`; UI test

**Test first:** Verify each card gets the correct field and does not substitute zero for missing data.

**Implementation:** Add three chart configurations using the shared wrapper.

**Verify:** Run focused UI test.

---

### Task 82: Add GOES magnetometer chart card
**Files:** `SpaceWeatherCharts.kt`; UI test

**Test first:** Verify dynamic primary/secondary labels and line gaps where values are null.

**Implementation:** Add Hp chart.

**Verify:** Run focused UI test.

---

### Task 83: Add Kp bar chart and status
**Files:** `app/.../ui/spaceweather/KpChart.kt`; tests

**Test first:** Verify bar classification at threshold boundaries and current status text uses the shared Kp mapper.

**Implementation:** Add 3-hour Kp presentation preserving PWA semantics.

**Verify:** Run unit/UI tests.

---

### Task 84: Add hemispheric-power chart
**Files:** `app/.../ui/spaceweather/HemisphericPowerChart.kt`; UI test

**Test first:** Verify North/South series and empty state.

**Implementation:** Add two-series chart.

**Verify:** Run focused UI test.

---

### Task 85: Assemble Space Weather adaptive layout without globe
**Files:** `app/.../ui/spaceweather/SpaceWeatherScreen.kt`; UI test

**Test first:** Compact layout stacks sections; expanded layout places related chart cards in two columns and leaves a reserved globe hero slot.

**Implementation:** Compose screen using existing components and freshness indicator.

**Verify:** Run compact/expanded UI tests.

---

## Phase 9 — Native Aurora Globe

### Task 86: Add SceneView dependency and render smoke test
**Files:** `feature/globe/build.gradle.kts`, `feature/globe/.../AuroraGlobe.kt`; instrumentation test

**Test first:** Instrumented test mounts/unmounts an empty SceneView without crashing.

**Implementation:** Add SceneView 4.37.0 and a minimal renderer composable.

**Verify:** Run `./gradlew :feature:globe:connectedDebugAndroidTest`.

---

### Task 87: Add coordinate-to-texture mapper
**Files:** `feature/globe/.../AuroraTextureMapper.kt`; unit test

**Test first:** Verify longitude -180/0/180 and latitude 90/0/-90 map to expected equirectangular pixel coordinates with wrapping/clamping.

**Implementation:** Pure math only; no Bitmap dependency.

**Verify:** Run `*AuroraTextureMapperTest`.

---

### Task 88: Add intensity palette mapper
**Files:** `feature/globe/.../AuroraPalette.kt`; unit test

**Test first:** Cover PWA bands: >=5 cyan, >=9 teal, >=16 green, >=26 orange, >=41 red; below 5 transparent.

**Implementation:** Return color/alpha values usable by texture generation.

**Verify:** Run `*AuroraPaletteTest`.

---

### Task 89: Generate aurora overlay bitmap
**Files:** `feature/globe/.../AuroraTextureGenerator.kt`; Robolectric test

**Test first:** Generate a small bitmap from known points and assert representative pixels are transparent/colored as expected.

**Implementation:** Draw intensity points onto a transparent equirectangular bitmap; avoid per-frame regeneration.

**Verify:** Run `*AuroraTextureGeneratorTest`.

---

### Task 90: Add bundled Earth texture asset
**Files:** `feature/globe/src/main/assets/textures/earth_night.jpg`, `NOTICE.md`

**Test first:** Add an asset existence test that opens the texture stream and verifies non-zero bytes.

**Implementation:** Add a suitably reduced NASA Earth-at-Night/Black-Marble equirectangular texture and attribution/source notice; do not bundle a needlessly huge source image.

**Verify:** Run asset test and `:feature:globe:assembleDebug`.

---

### Task 91: Render textured Earth sphere
**Files:** `feature/globe/.../EarthScene.kt`; instrumentation test

**Test first:** Renderer test creates Earth node/material with texture and survives lifecycle recreation.

**Implementation:** Add one sphere, night texture, basic lighting/environment, and initial Arctic/North Atlantic camera.

**Verify:** Run globe instrumentation test.

---

### Task 92: Add aurora shell
**Files:** `feature/globe/.../AuroraLayer.kt`; instrumentation test

**Test first:** With a generated overlay, assert an aurora shell node/material is present above Earth; with null overlay it is absent while Earth remains.

**Implementation:** Use a slightly larger transparent sphere and update its texture only when snapshot changes.

**Verify:** Run focused globe test.

---

### Task 93: Add atmosphere shell
**Files:** `feature/globe/.../AtmosphereLayer.kt`; instrumentation/smoke test

**Test first:** Assert atmosphere node can be created/destroyed through lifecycle without renderer errors.

**Implementation:** Add lightweight translucent blue shell; avoid physically based atmosphere simulation.

**Verify:** Run focused globe test.

---

### Task 94: Add camera rotation and gesture state
**Files:** `feature/globe/.../GlobeInteractionState.kt`; unit test

**Test first:** Verify auto-rotation advances while idle, manual interaction pauses it, idle timeout resumes it, and zoom clamps to limits.

**Implementation:** Keep interaction math/state outside SceneView callbacks.

**Verify:** Run `*GlobeInteractionStateTest`.

---

### Task 95: Wire gestures to SceneView camera
**Files:** `AuroraGlobe.kt`; instrumentation test

**Test first:** Simulated drag/scale events update interaction state without allowing invalid camera distance.

**Implementation:** Map Compose/SceneView gesture callbacks to controlled rotation/zoom.

**Verify:** Run globe instrumentation test.

---

### Task 96: Add globe no-data/failure fallback
**Files:** `AuroraGlobe.kt`; UI/instrumentation test

**Test first:** No OVATION snapshot still renders Earth; cached snapshot renders aurora with cached freshness; renderer exception invokes a non-3D fallback callback rather than crashing the screen.

**Implementation:** Keep renderer failure isolated.

**Verify:** Run focused test.

---

### Task 97: Integrate globe into Space Weather
**Files:** `app/.../ui/spaceweather/SpaceWeatherScreen.kt`, app/module dependencies; UI test

**Test first:** Verify screen supplies normalized snapshot only and still renders charts when globe state is unavailable.

**Implementation:** Replace reserved hero slot with `AuroraGlobe`.

**Verify:** Run app Space Weather UI tests.

---

## Phase 10 — Solar Activity Screen

### Task 98: Add SolarActivityViewModel
**Files:** `app/.../ui/solaractivity/SolarActivityViewModel.kt`; test

**Test first:** Verify independent states for probabilities, imagery, X-ray, flares, CMEs, and EPAM; one failure does not mark other sections failed.

**Implementation:** Combine repository flows into screen state.

**Verify:** Run `*SolarActivityViewModelTest`.

---

### Task 99: Add flare probability badges
**Files:** `app/.../ui/solaractivity/FlareProbabilityBadges.kt`; UI test

**Test first:** Verify C/M/X percentages and loading/error state.

**Implementation:** Recreate badge semantics with native components.

**Verify:** Run focused UI test.

---

### Task 100: Add imagery card component
**Files:** `app/.../ui/solaractivity/SolarImageryCard.kt`; UI test

**Test first:** Verify title/source/timestamp/image and click callback; image loading failure keeps card shell readable.

**Implementation:** Use Coil and HelioFlux card styling.

**Verify:** Run focused UI test.

---

### Task 101: Add magnetogram active-region overlay
**Files:** `app/.../ui/solaractivity/MagnetogramCard.kt`, coordinate mapper/test

**Test first:** Verify PWA mapping: x = 512 + xArcsec/1000*512; y = 512 - yArcsec/1000*512, converted to normalized percentages.

**Implementation:** Overlay active-region labels on the image.

**Verify:** Run unit/UI tests.

---

### Task 102: Add ENLIL frame player
**Files:** `app/.../ui/solaractivity/EnlilPlayerState.kt`, `EnlilCard.kt`; tests

**Test first:** Verify 200ms frame cadence, wraparound, pause when not visible, and graceful no-frame state.

**Implementation:** Reuse the Sun frame-player pattern where practical; do not duplicate timing logic if a shared abstraction naturally fits both after the pattern exists twice.

**Verify:** Run unit/UI tests.

---

### Task 103: Add adaptive imagery gallery
**Files:** `app/.../ui/solaractivity/SolarImageryGallery.kt`; UI test

**Test first:** Compact width exposes swipeable cards; expanded width lays out HMI, LASCO C2, LASCO C3, and ENLIL in a grid.

**Implementation:** Use Pager/LazyRow for compact and adaptive grid/rows for wider layouts.

**Verify:** Run compact/expanded UI tests.

---

### Task 104: Add fullscreen imagery viewer
**Files:** `app/.../ui/solaractivity/FullscreenImageryViewer.kt`; UI test

**Test first:** Verify tap opens, pinch zoom clamps, pan only when zoomed, back closes, and transform resets when changing image.

**Implementation:** Support HMI/LASCO/ENLIL; keep active-region overlay on HMI.

**Verify:** Run focused UI test.

---

### Task 105: Add GOES X-ray chart
**Files:** `app/.../ui/solaractivity/XrayChart.kt`; tests

**Test first:** Verify log-scale domain 1e-9 to 1e-2 and C/M/X reference lines at 1e-6, 1e-5, 1e-4.

**Implementation:** Configure shared Vico wrapper for four nullable series.

**Verify:** Run unit/UI tests.

---

### Task 106: Add recent flare list
**Files:** `app/.../ui/solaractivity/FlareList.kt`; UI test

**Test first:** Verify newest-first display, class color semantics A/B/C/M/X, timestamp, observatory, optional AR and location, plus valid-empty message.

**Implementation:** Native lazy list/card section.

**Verify:** Run focused UI test.

---

### Task 107: Add recent CME list
**Files:** `app/.../ui/solaractivity/CmeList.kt`; UI test

**Test first:** Verify speed, timestamp, direction, width, details action, speed emphasis thresholds, and valid-empty state.

**Implementation:** Native list matching PWA information.

**Verify:** Run focused UI test.

---

### Task 108: Add ACE EPAM chart
**Files:** `app/.../ui/solaractivity/AceEpamChart.kt`; UI test

**Test first:** Verify electron low, proton low, and proton mid series, exponential axis formatting, null gaps, and empty state.

**Implementation:** Add chart card.

**Verify:** Run focused UI test.

---

### Task 109: Assemble adaptive Solar Activity screen
**Files:** `app/.../ui/solaractivity/SolarActivityScreen.kt`; UI test

**Test first:** Verify compact/expanded structure, all independent section states, freshness indicator, and fullscreen viewer entry.

**Implementation:** Compose the completed components.

**Verify:** Run compact/expanded UI tests.

---

## Phase 11 — Background Alerts

### Task 110: Add alert policy
**Files:** `feature/alerts/.../AlertPolicy.kt`; unit test

**Test first:** Verify no alert below Kp5, alert on first Kp5+, alert on escalation to a higher integer Kp band, no duplicate for unchanged band, alert for new M/X flare, no alert for A/B/C flare.

**Implementation:** Pure policy code with no Android notification APIs.

**Verify:** Run `./gradlew :feature:alerts:testDebugUnitTest --tests "*AlertPolicyTest"`.

---

### Task 111: Add alert-state persistence
**Files:** `core/database/.../entity/AlertStateEntity.kt`, DAO; test

**Test first:** Save/read last notified Kp band and flare ids; update atomically.

**Implementation:** Add small Room table so process death/reboot does not repeat old alerts.

**Verify:** Run DAO test.

---

### Task 112: Add notification formatter
**Files:** `feature/alerts/.../SpaceWeatherNotificationFormatter.kt`; test

**Test first:** Verify concise titles/body for geomagnetic alert, escalation, M flare, and X flare.

**Implementation:** Keep strings/resources separate from policy.

**Verify:** Run formatter test.

---

### Task 113: Add notification channel
**Files:** `feature/alerts/.../NotificationChannels.kt`, resources; instrumentation/smoke test

**Test first:** Verify channel id/name/importance creation is idempotent.

**Implementation:** Create one space-weather alerts channel.

**Verify:** Run focused test.

---

### Task 114: Add Android 13+ notification permission flow
**Files:** `app/.../ui/notifications/NotificationPermission.kt`, manifest, UI test

**Test first:** On API 33+ permission request is only offered when alerts are enabled/introduced and not already granted; below API 33 no runtime request occurs.

**Implementation:** Add `POST_NOTIFICATIONS`; do not add location permissions.

**Verify:** Run unit/UI checks and inspect merged manifest.

---

### Task 115: Add background alert worker
**Files:** `feature/alerts/.../SpaceWeatherAlertWorker.kt`; WorkManager test

**Test first:** Fake repositories return conditions/events; verify worker evaluates policy, writes alert state, posts only new alerts, and returns retry on transient network failure without duplicating old alerts.

**Implementation:** Use the same repositories/data layer as the app.

**Verify:** Run WorkManager test.

---

### Task 116: Add alert work scheduling
**Files:** `feature/alerts/.../AlertScheduler.kt`; test

**Test first:** Verify one unique periodic work request exists, uses network-connected constraint, and duplicate scheduling calls do not create duplicate jobs.

**Implementation:** Schedule conservative periodic work; do not promise exact execution time.

**Verify:** Run scheduler test.

---

### Task 117: Start alert scheduler from application
**Files:** `app/.../HelioFluxApplication.kt`; test

**Test first:** Fake scheduler records one initialization call.

**Implementation:** Create notification channel and schedule periodic work during app initialization.

**Verify:** Run app unit test.

---

## Phase 12 — Home-Screen Widgets

### Task 118: Add shared widget presentation model
**Files:** `feature/widgets/.../WidgetModels.kt`, `WidgetModelMapper.kt`; unit test

**Test first:** Verify cached repository state maps to compact display strings plus Fresh/Delayed/Cached indicator state.

**Implementation:** Keep widgets independent from Room entity types.

**Verify:** Run `:feature:widgets:testDebugUnitTest`.

---

### Task 119: Add Space Weather Status widget
**Files:** `feature/widgets/.../SpaceWeatherStatusWidget.kt`, receiver/provider XML; test

**Test first:** Verify Kp, Bz, speed, flare status, and freshness render from a fixture widget model.

**Implementation:** Use Glance 1.2.0 with responsive sizes.

**Verify:** Run widget unit/Robolectric test and assemble.

---

### Task 120: Add Aurora Conditions widget
**Files:** `feature/widgets/.../AuroraConditionsWidget.kt`, receiver/provider XML; test

**Test first:** Verify Kp, OVATION summary, hemispheric power when present, and freshness.

**Implementation:** Use Glance with compact and larger responsive layouts.

**Verify:** Run focused widget test.

---

### Task 121: Add widget deep-link routing
**Files:** `app/.../ui/navigation/WidgetDeepLinks.kt`, widget actions/tests

**Test first:** Space Weather widget opens Space Weather; Aurora widget opens Space Weather focused at/near globe; Sun widget opens Home/Sun hero.

**Implementation:** Add stable intents/deep-link extras consumed by `MainActivity`.

**Verify:** Run navigation test.

---

### Task 122: Add static Sun Hero widget baseline
**Files:** `feature/widgets/.../SunHeroWidget.kt`, receiver/provider XML; test

**Test first:** Latest cached AIA 304 frame renders; missing frame shows a clean fallback; tapping opens Home.

**Implementation:** Use Glance image output as the guaranteed fallback behavior.

**Verify:** Run widget test and manual launcher smoke test.

---

### Task 123: Add Sun widget 30-minute refresh worker
**Files:** `feature/widgets/.../SunWidgetRefreshWorker.kt`, scheduler/test

**Test first:** Verify worker asks `SolarHeroRepository` for updates, does not refetch unchanged frame ids, refreshes widget after success, and is scheduled at approximately 30-minute periodic cadence with network constraint.

**Implementation:** Reuse repository/cache; do not download all 60 frames when only a new latest frame is needed for the widget.

**Verify:** Run WorkManager/widget tests.

---

### Task 124: Validate optional Sun widget animation
**Files:** `docs/decisions/2026-09-19-sun-widget-animation.md`, temporary test implementation under `feature/widgets`

**Test first:** Define manual acceptance checks before coding: no launcher crash, no runaway memory/battery use, frame cycling works on Pixel Launcher/emulator host, and static fallback remains valid.

**Implementation:** Build the smallest RemoteViews/ViewFlipper or AdapterViewFlipper proof using a small cached frame subset. Record pass/fail and measured constraints in the decision doc.

**Verify:** Perform the acceptance checks on at least the project emulator and one physical launcher if available.

---

### Task 125: Integrate Sun widget animation only if validated
**Files:** `feature/widgets/.../SunHeroWidgetReceiver.kt` or related RemoteViews files; tests

**Test first:** If Task 124 passed, verify animation path selects cached subset and fallback path always has latest static frame. If Task 124 failed, add a regression test documenting static-only behavior and do not add animation code.

**Implementation:** Apply the recorded decision only; do not invent a second animation mechanism.

**Verify:** Run widget tests plus manual launcher smoke test.

---

## Phase 13 — App-Level Offline/Refresh Behavior

### Task 126: Add app startup refresh coordinator
**Files:** `app/.../data/AppRefreshCoordinator.kt`; test

**Test first:** Verify cached state is not blocked while foreground refresh calls are launched once per app start/resume policy.

**Implementation:** Coordinate repository refreshes from app lifecycle; screens remain observers.

**Verify:** Run `*AppRefreshCoordinatorTest`.

---

### Task 127: Add source-isolated refresh failure handling
**Files:** coordinator/repository tests

**Test first:** Force Helioviewer failure while NOAA succeeds; verify Home retains cached Sun but live NOAA metrics become fresh. Repeat with OVATION failure while charts remain live.

**Implementation:** Only adjust coordination/state mapping needed to satisfy isolation.

**Verify:** Run failure matrix tests.

---

### Task 128: Verify cached-first cold start
**Files:** `app/src/androidTest/.../CachedColdStartTest.kt`

**Test first:** Seed Room before activity launch, block network, and assert cached Home/Space Weather/Solar Activity content appears with Cached freshness.

**Implementation:** Fix only wiring exposed by the test.

**Verify:** Run connected test.

---

### Task 129: Verify rotation/window resize does not refetch
**Files:** `app/src/androidTest/.../ConfigurationChangeTest.kt`

**Test first:** Count fake repository refresh calls through portrait→landscape / compact→expanded recreation; assert view state/timeframe survives and no duplicate screen-level network refresh is triggered.

**Implementation:** Move any incorrectly scoped state into ViewModel/saveable state.

**Verify:** Run connected test.

---

## Phase 14 — Final Functional/Accessibility Verification

### Task 130: Add navigation integration test
**Files:** `app/src/androidTest/.../MainNavigationTest.kt`

**Test first:** Navigate Home → Space Weather → Solar Activity → Home and verify correct screen headings and selected nav state.

**Implementation:** Fix only navigation wiring.

**Verify:** Run connected test.

---

### Task 131: Add partial-data integration test
**Files:** `app/src/androidTest/.../PartialDataTest.kt`

**Test first:** Supply one failed source, one valid-empty source, one cached source, and one fresh source; assert each section renders its own state without a screen-wide error.

**Implementation:** Fix section-state plumbing.

**Verify:** Run connected test.

---

### Task 132: Add freshness accessibility test
**Files:** `app/src/androidTest/.../FreshnessAccessibilityTest.kt`

**Test first:** With indicator text visually collapsed, semantics still announce `Live`, `Delayed`, or `Cached` plus last-updated context.

**Implementation:** Add/adjust content descriptions and state descriptions.

**Verify:** Run connected test.

---

### Task 133: Add no-location-permission manifest test
**Files:** `app/src/test/.../ManifestPolicyTest.kt` or manifest inspection script/test

**Test first:** Assert merged/requested permissions do not include coarse/fine/background location.

**Implementation:** Remove any accidental location dependency/permission if introduced.

**Verify:** Run test and inspect `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`.

---

### Task 134: Run full unit-test suite
**Files:** none unless failures expose planned defects

**Test first:** This task is the verification itself.

**Implementation:** Do not make unrelated cleanup. Fix only failures caused by the planned implementation.

**Verify:** Run `./gradlew testDebugUnitTest`; all unit tests pass.

---

### Task 135: Run full connected UI/instrumentation suite
**Files:** none unless failures expose planned defects

**Test first:** This task is the verification itself.

**Implementation:** Fix only test failures tied to design requirements.

**Verify:** Run `./gradlew connectedDebugAndroidTest`; all connected tests pass.

---

### Task 136: Build release candidate artifacts
**Files:** build configuration only if required by an existing planned dependency

**Test first:** Run lint/assemble before changing anything.

**Implementation:** Address only build/lint issues introduced by this plan; do not add signing secrets or release automation.

**Verify:** Run `./gradlew lintDebug assembleDebug`; build succeeds with no blocking lint errors.

---

### Task 137: Compare feature parity against PWA checklist
**Files:** `docs/plans/2026-09-19-helioflux-android-native-conversion-progress.md`

**Test first:** Create a checklist from the design success criteria and PWA reference features, marking each item pass/fail based on actual app behavior.

**Implementation:** Record Home, Space Weather, Solar Activity, offline/freshness, alerts, all three widgets, adaptive layouts, and globe status. Do not mark incomplete items complete.

**Verify:** Every design success criterion has an evidence note or an explicitly open defect.

---

## Implementation Order Notes

- Minimize GitHub Actions usage. Do not trigger CI for every small TDD step or individual file change; batch coherent work locally/in commits and use Actions only at meaningful verification gates where CI adds value.

- Execute tasks strictly in order unless a task explicitly says it can be skipped based on a recorded validation result.
- Do not build UI network calls directly; all screens/widgets/workers consume repositories.
- Do not modify `godwept/HelioFlux-web`.
- Keep commits scoped to coherent groups of tasks; avoid unrelated formatting or refactors.
- When a PWA parser and this plan disagree, use the approved design plus current source implementation as the reference and update the plan before changing semantics.
- For external API payload drift discovered during implementation, add a failing fixture/test before changing a parser.
- Do not add location support, customizable alert thresholds, accounts, cloud sync, long-term archives, or dashboard customization in v1.
- Do not replace cached good data with a malformed response.
- Do not make the globe responsible for networking or repository access.
- Do not add a second DI framework unless manual composition becomes demonstrably unmanageable and the design is revised.

## Definition of Done

- [ ] All tasks completed in order or explicitly skipped by a documented validation decision.
- [ ] All new production code has tests written first.
- [ ] All parser/domain tests pass.
- [ ] All repository/cache tests pass.
- [ ] All ViewModel/unit tests pass.
- [ ] All Compose UI/instrumentation tests pass.
- [ ] `./gradlew testDebugUnitTest` passes.
- [ ] `./gradlew connectedDebugAndroidTest` passes on the configured emulator/device.
- [ ] `./gradlew lintDebug assembleDebug` passes.
- [ ] No unplanned files or features were added.
- [ ] No location permission is present.
- [ ] Cached data is clearly identified when live refresh is unavailable.
- [ ] Portrait and landscape/wide layouts behave intentionally.
- [ ] Space Weather Status, Aurora Conditions, and Sun Hero widgets work.
- [ ] The Aurora Globe is native and failure-isolated.
- [ ] The PWA repository remains unchanged.
- [ ] The feature-parity progress checklist is complete.
