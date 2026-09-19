# HelioFlux Native Android Conversion Design

**Date:** 2026-09-19  
**Status:** Approved

## Goal

Rebuild the existing `godwept/HelioFlux-web` space-weather PWA as a genuine native Android application in Kotlin using Jetpack Compose and Material 3, while leaving the PWA intact as the reference implementation. The Android app should preserve HelioFlux's dark, solar-focused visual identity and the same core space-weather information, but evolve the user experience around native Android patterns, adaptive layouts, offline resilience, notifications, widgets, and a native 3D Aurora Globe.

The conversion is not intended to be a pixel-for-pixel recreation. Feature parity means preserving the information, scientific meaning, imagery, and core capabilities of HelioFlux while choosing native Android interactions where they provide a better experience.

## Success Criteria

- [ ] The Android app provides native equivalents for the PWA's Home, Space Weather, and Solar Activity functionality without using a WebView as the application UI.
- [ ] Existing NOAA, Helioviewer, NASA DONKI, HEK, LASCO, HMI, ACE EPAM, ENLIL, OVATION, and forecast-discussion data used by the PWA remain available where they are part of the current experience.
- [ ] The app preserves the recognizable HelioFlux visual character while using Material 3 and adaptive Compose layouts.
- [ ] Portrait and landscape are both intentionally designed, with layouts adapting to available window width rather than orientation alone.
- [ ] Cached recent data remains useful offline, with clear freshness state and last-updated information.
- [ ] Significant geomagnetic activity and major solar flares can generate conservative background notifications without location access.
- [ ] Three home-screen widgets are available: Space Weather Status, Aurora Conditions, and Sun Hero.
- [ ] The Aurora Globe is implemented as a native 3D visualization, isolated from the rest of the application architecture.
- [ ] Existing data-processing rules ported from JavaScript are covered by Kotlin tests.
- [ ] Failure of one upstream data source does not make unrelated parts of the application unusable.

## Scope

**In scope:**

- Kotlin Android application.
- Jetpack Compose UI with Material 3.
- Native Home dashboard.
- Native Space Weather screen.
- Native Solar Activity screen.
- Native animated Sun hero based on Helioviewer AIA 304 imagery.
- Native 3D Aurora Globe using a dedicated rendering feature.
- Existing space-weather charts and timeframe concepts.
- Existing solar imagery and fullscreen zoom/pan experience.
- Existing flare, CME, active-region, particle, forecast, solar-wind, Kp, OVATION, hemispheric-power, and GOES data concepts.
- Hybrid networking: direct NOAA access where appropriate, Cloudflare where it adds value.
- Rolling local cache sufficient for existing recent-history chart windows.
- Per-source freshness metadata and a compact visible freshness indicator.
- Background monitoring using normal Android scheduled work.
- Notifications for significant global space-weather events.
- Three home-screen widget types.
- Responsive phone, landscape, tablet, foldable, and split-screen behavior.
- The existing PWA remains unchanged and acts as the behavioral/reference implementation.

**Out of scope for v1:**

- Location permissions or location-aware aurora alerts.
- Long-term historical data archives.
- User-configurable alert thresholds.
- Customizable/reorderable Home dashboard.
- Accounts or cloud synchronization.
- A major rewrite of the existing Cloudflare backend.
- General-purpose mapping or globe capabilities beyond the Aurora visualization.
- Advanced 3D globe features such as terrain, geographic tiles, satellite trajectories, or AR.
- Pixel-for-pixel reproduction of the PWA.
- Replacing the PWA.

## Design

### 1. Product Direction

The Android app follows a **native evolution** approach rather than a faithful visual clone.

The app keeps HelioFlux's established identity:

- near-black / space-themed surfaces;
- solar orange as a strong brand accent;
- cyan/blue data accents;
- restrained warning colors;
- large solar imagery as a visual anchor;
- dense but readable scientific information.

Material 3 is used as a native Android foundation, but the app should not look like an unmodified stock Material sample. HelioFlux should remain visually distinctive.

The three primary destinations remain:

1. **Home**
2. **Space Weather**
3. **Solar Activity**

Navigation adapts to available width:

- compact layouts use bottom navigation;
- wider layouts use a navigation rail.

This decision is based on available window width rather than a hard portrait/landscape rule so it also behaves correctly on tablets, foldables, and split-screen windows.

### 2. Architecture and Module Boundaries

Use a shared data core with selective presentation modules.

Initial module boundaries:

- **app** — Compose screens, navigation, Material theme, adaptive layout, screen-level ViewModels.
- **core:model** — shared Kotlin domain models.
- **core:data** — repositories, API clients, parsing, caching coordination, freshness logic, retries and validation.
- **core:database** — rolling recent-history storage and source metadata.
- **feature:globe** — native 3D Aurora Globe renderer and Compose integration.
- **feature:widgets** — all home-screen widgets.
- **feature:alerts** — background checks and notification rules.

Home, Space Weather, and Solar Activity remain screens within `app` initially. They should not become separate Gradle modules until their complexity proves that separation is useful.

Primary data flow:

```
NOAA / Cloudflare
        ↓
network clients
        ↓
parsers / normalizers
        ↓
repositories
        ↓
local database + image cache
        ↓
UI / widgets / background alerts
```

The local data layer is the application's working source of truth. Screens observe stored state and repositories refresh it. UI components should not independently fetch remote APIs.

Use conventional Android state management:

- ViewModel;
- Kotlin coroutines;
- Flow;
- repositories as the data boundary;
- modest dependency injection without adding architecture for its own sake.

### 3. Data Model and Freshness State

Every persisted dataset should carry enough metadata to distinguish source age from network state.

At minimum, maintain:

- observation timestamp, when supplied by the upstream source;
- fetched timestamp;
- source identifier;
- most recent refresh result;
- derived freshness state.

Freshness should be evaluated per source internally. A failed Helioviewer refresh must not mark otherwise-current NOAA solar-wind data as stale.

Visible freshness states:

- **Green** — current / live enough for the source.
- **Amber** — delayed or stale but still usable.
- **Red** — refresh failed or device is offline and cached data is being shown.

The UI keeps the colored indicator visible but normally hides the text label.

When freshness changes, the text appears briefly, for example:

- `Live · Updated 2 min ago`
- `Delayed · Updated 24 min ago`
- `Cached · Updated 1h 12m ago`

The text then fades, leaving only the indicator. Tapping the indicator reveals the message again for a few seconds.

Accessibility semantics must expose the full freshness state even when only the visual indicator is shown.

### 4. Home Screen

Home becomes a native dashboard rather than a direct port of the PWA carousel structure.

The animated Sun remains the dominant visual element and brand anchor.

Portrait / compact layout:

1. HelioFlux title / header.
2. Animated Sun hero.
3. Compact current-conditions dashboard.
4. NOAA forecast summaries.

Current conditions should include the most useful at-a-glance values such as:

- Kp;
- Bz;
- solar-wind speed;
- flare activity;
- relevant aurora / geomagnetic status.

Important metrics should be actionable when useful. Tapping one can navigate to the relevant detailed section instead of remaining a decorative badge.

Wider layouts can place the Sun and current-condition dashboard side-by-side, with forecasts continuing below.

### 5. Space Weather Screen

Space Weather remains the main monitoring and charting screen.

Primary content:

- native 3D Aurora Globe;
- current Bz;
- solar-wind speed;
- solar-wind density;
- 1h / 3h / 12h / 2-day timeframe control;
- Bz/Bt magnetic-field chart;
- density chart;
- speed chart;
- temperature chart;
- GOES magnetometer primary/secondary data;
- planetary Kp 3-hour visualization;
- hemispheric power North/South.

Current PWA Kp status thresholds and equivalent scientific meaning should be preserved unless a source definition requires correction during implementation.

Wide layouts should use the extra width intelligently rather than merely stretching portrait cards. Related charts may appear side-by-side while important visualizations can still span the available width.

Loading and failure are handled per section. A failure in one feed should not replace the entire screen with an error state.

### 6. Solar Activity Screen

Retain the PWA's existing major Solar Activity capabilities:

- flare probabilities;
- HMI magnetogram;
- active-region markers;
- LASCO C2 imagery;
- LASCO C3 imagery;
- WSA-Enlil animation;
- GOES X-ray flux;
- recent flares;
- recent CMEs;
- ACE EPAM particle data.

The PWA's horizontal imagery carousel should evolve into an adaptive native presentation:

- swipeable imagery cards on narrow screens;
- grid-like presentation when more width is available.

Selecting imagery opens a native fullscreen viewer with pinch zoom and pan.

Landscape should provide a meaningfully improved viewing experience for magnetogram, LASCO, ENLIL, and charts rather than simply rotating the portrait layout.

### 7. Animated Sun Hero

The PWA's Helioviewer AIA 304 animation is retained as a native feature.

Existing behavior to preserve conceptually:

- recent AIA 304 imagery;
- temporal animation;
- smooth frame transitions;
- image preloading / caching;
- pinch zoom;
- pan when zoomed;
- reset interaction;
- pausing or reducing unnecessary animation work when not visible.

The Android version should use native image loading, disk caching, Compose/native gesture handling, and native animation rather than browser canvas logic.

The Helioviewer selection rules in the current PWA are reference behavior and should be ported into tested Kotlin data logic.

### 8. Native 3D Aurora Globe

The Aurora Globe is a dedicated rendering feature, isolated behind `feature:globe`.

The intended renderer is a native Android real-time 3D solution based on Google Filament, preferably through a maintained Compose-oriented integration such as SceneView if technical validation confirms it is suitable.

The globe scene is intentionally small:

1. textured Earth sphere;
2. dynamic aurora overlay;
3. lightweight atmospheric glow;
4. camera and interaction controls.

The globe is not a general-purpose map engine.

#### Earth

Render a night-Earth texture that preserves the character of the PWA globe.

The core Earth texture should be bundled or locally available so the Earth can appear immediately even when offline.

#### Aurora Overlay

Do not create thousands of individual scene objects for OVATION samples.

Instead:

1. parse and normalize OVATION points;
2. map latitude, longitude, and intensity into a transparent dynamic aurora texture;
3. wrap the texture on a sphere slightly larger than Earth;
4. regenerate the texture only when new OVATION data is available.

The PWA intensity treatment is the initial visual reference:

- faint cyan;
- teal;
- green;
- orange;
- red for strongest intensity.

Exact blending can be refined during implementation without changing the scientific values represented.

The renderer receives normalized data such as:

`AuroraPoint(latitude, longitude, intensity)`

It never performs NOAA networking itself.

Flow:

```
NOAA OVATION
    ↓
parser
    ↓
cached aurora models
    ↓
globe texture mapper
    ↓
dynamic aurora texture
    ↓
native 3D renderer
```

#### Camera and Interaction

Preserve the PWA's Arctic / North Atlantic emphasis and slow automatic rotation.

Android interaction:

- slow auto-rotation;
- drag to rotate manually;
- user interaction pauses auto-rotation;
- gentle auto-rotation resumes after an idle period;
- limited pinch zoom;
- camera movement constrained so the Earth cannot easily be lost.

#### Atmosphere

Use a lightweight translucent atmospheric shell / material to recreate the blue atmospheric edge. This is a visual effect, not a physically accurate atmosphere simulation.

#### Failure Behavior

If new OVATION data fails:

- keep the Earth visible;
- keep the last cached aurora field if available;
- surface the cached/stale freshness state.

If there is no OVATION data:

- render Earth without the aurora overlay.

If the 3D renderer itself fails on an unusual device:

- the surrounding Space Weather screen and all non-globe data remain functional.

The 3D globe is a high-value visualization, not a dependency for accessing the underlying data.

### 9. Networking and Cloudflare Strategy

Use a hybrid approach.

Android may call uncomplicated public NOAA endpoints directly where there is no meaningful benefit to proxying them.

Retain the existing Cloudflare Worker where it provides real value, including:

- NASA DONKI API-key protection;
- Helioviewer proxy behavior where useful;
- HEK access;
- LASCO access;
- HMI access;
- ENLIL directory / frame handling;
- upstream quirks and fallbacks;
- selected server-side caching.

The existing PWA continues to use its current Cloudflare setup unchanged.

The Android data layer must hide whether a dataset came directly from NOAA or through Cloudflare. Screens and widgets consume normalized repository data.

### 10. Ported Data-Processing Logic

The following PWA logic should be ported at the domain level into Kotlin and covered by tests:

- NOAA solar-wind magnetic parsing;
- NOAA plasma parsing;
- planetary Kp processing;
- Kp display / status thresholds;
- GOES X-ray series normalization;
- flare probability parsing;
- DONKI flare normalization;
- DONKI CME normalization;
- ACE EPAM parsing and missing-value handling;
- NOAA forecast-discussion parsing;
- HMI / HEK active-region coordinate handling;
- ENLIL run selection, ordering, and downsampling;
- Helioviewer image/frame selection;
- OVATION coordinate and intensity handling;
- existing timeframe filtering rules.

The JavaScript implementation is the behavioral reference, but Kotlin code should be written idiomatically rather than line-for-line.

### 11. Offline Caching

Use rolling recent history rather than latest-value-only storage.

The cache should preserve enough data to keep the existing recent-history experience useful offline:

- 1 hour;
- 3 hours;
- 12 hours;
- 2 days;
- 72-hour datasets used on Solar Activity.

Structured scientific data belongs in the local database.

Large image/frame content belongs in a separate managed disk cache.

On app start:

1. show cached data immediately;
2. begin refresh opportunistically;
3. validate responses;
4. persist valid new data;
5. update observers;
6. update freshness state.

A successful HTTP response must not automatically replace known-good cache data. Parse and validate first; malformed or obviously incomplete upstream payloads must not overwrite a usable local dataset.

### 12. Background Monitoring and Notifications

Use Android's normal persistent background-work mechanisms rather than a continuously running service.

Background monitoring is intentionally conservative for v1.

Monitor globally relevant conditions such as:

- significant geomagnetic activity;
- major solar flares;
- meaningful escalation in those conditions.

No location permission is requested or required.

The worker uses the same repositories and parsers as the foreground application.

Notification rules must avoid repeated alerts for an unchanged condition. The app should remember what event/state has already produced a notification and notify only for a genuinely new event or meaningful escalation.

The design should accept that Android may defer periodic background work for battery optimization. The product must not promise exact-to-the-minute alerts.

### 13. Home-Screen Widgets

Provide three widget types.

#### Space Weather Status

Compact at-a-glance widget containing the most useful current metrics, such as:

- Kp;
- Bz;
- solar-wind speed;
- flare status;
- freshness.

#### Aurora Conditions

Aurora-oriented widget containing information such as:

- current Kp;
- OVATION / aurora activity summary;
- hemispheric power where useful;
- freshness.

#### Sun Hero

A visually minimal widget centered on the AIA 304 Sun.

Behavior:

- check for newer source imagery at approximately a 30-minute cadence where Android scheduling permits;
- do not redownload the entire animation unnecessarily;
- retain an optimized subset of recent frames locally;
- animate that subset only where widget-host behavior proves reliable;
- otherwise display the latest available frame as a static fallback;
- tapping the widget opens HelioFlux to the full native Sun experience.

The widget's purpose is visual immediacy, not to replicate the full in-app 5 FPS animation at all times.

Space Weather Status and Aurora Conditions should use a modern widget approach such as Jetpack Glance where suitable. The Sun Hero may use a different widget presentation mechanism if required for dependable frame cycling.

### 14. Adaptive Layout

Portrait and landscape are equal design targets.

Layouts adapt to available width rather than reading orientation alone.

This enables consistent behavior across:

- narrow phones;
- phones in landscape;
- tablets;
- foldables;
- split-screen / multi-window.

Examples of adaptive behavior:

- bottom navigation becomes navigation rail;
- Home changes from vertical Sun/dashboard stacking to side-by-side presentation;
- Space Weather can present related chart pairs in columns;
- Solar Activity imagery changes from swipeable cards to a grid;
- 3D Globe gains substantially more visual space;
- fullscreen scientific imagery makes effective use of landscape.

Rotation should not cause unnecessary network reloads or discard view state that should survive normal configuration changes.

### 15. Error Handling and Edge Cases

Errors are isolated to the smallest reasonable feature or data source.

Examples:

- solar-wind failure does not hide cached forecast discussion;
- Helioviewer failure does not remove current Kp;
- OVATION failure does not remove the Earth globe;
- 3D rendering failure does not remove Space Weather charts;
- DONKI failure does not break unrelated Solar Activity imagery.

Expected UI states include:

- loading with no cache;
- fresh data;
- stale but usable data;
- cached/offline data;
- source-specific failure;
- empty-but-valid result, such as no recent CMEs;
- malformed upstream data rejected while cache remains intact.

User-facing errors should be concise and should avoid exposing internal networking implementation details unless useful for troubleshooting.

## Testing Strategy

### Parser and Domain Tests

Ported data-processing behavior receives Kotlin unit tests covering representative real-world and edge-case payloads.

Important areas include:

- NOAA array/header formats;
- missing/null solar-wind values;
- Kp threshold boundaries;
- flare probability parsing;
- GOES X-ray channel mapping and log-scale-safe values;
- DONKI missing/optional fields;
- ACE EPAM sentinel values;
- forecast discussion section extraction;
- HEK active-region filtering and coordinate conversion;
- ENLIL latest-run selection and downsampling;
- Helioviewer stale/latest image handling;
- OVATION intensity mapping.

Where practical, fixtures should be derived from known-good source payload structures already represented in the PWA project.

### Repository and Cache Tests

Verify:

- cached data is emitted before network completion;
- valid refresh replaces older cached data;
- invalid refresh does not overwrite good cache;
- per-source freshness is calculated correctly;
- rolling history retention keeps required windows;
- failed sources do not corrupt unrelated datasets.

### ViewModel / UI State Tests

Verify key screen states:

- initial load;
- cached-first load;
- refresh success;
- partial source failure;
- offline mode;
- empty valid datasets;
- freshness state transitions.

### Compose UI Tests

Cover high-value interactions:

- navigation between the three destinations;
- adaptive navigation mode;
- timeframe controls;
- fullscreen imagery;
- freshness indicator tap/reveal behavior;
- portrait and wide-window arrangements.

### Globe Tests

Keep scientific mapping testable outside the renderer.

Verify:

- latitude/longitude mapping;
- intensity-to-color/opacity mapping;
- aurora texture generation;
- stale/cached data behavior;
- no-data behavior.

The 3D renderer itself should receive device/emulator integration testing for lifecycle handling, rotation, zoom constraints, and renderer recreation.

### Background and Notification Tests

Verify:

- new significant events generate one notification;
- unchanged conditions do not repeatedly notify;
- escalation can generate a new notification;
- worker uses cached/repository state consistently;
- no location dependency exists.

### Widget Tests

Verify:

- all three widgets render from cached normalized data;
- freshness state is represented correctly;
- resize/adaptive widget layouts remain legible;
- Sun Hero static fallback works when animation is unavailable;
- widget taps route to the appropriate application destination.

## Open Questions

The following decisions are intentionally deferred to implementation planning / technical validation:

1. Exact Filament integration layer for the globe: SceneView versus direct Filament integration.
2. Exact native chart library versus custom Compose chart rendering.
3. Exact persistence technology and schema shape for rolling time-series data.
4. Exact dependency-injection library, if one is needed beyond lightweight manual composition.
5. Exact freshness thresholds per upstream source, since different NOAA/Helioviewer products update on different cadences.
6. Exact v1 notification thresholds for "significant" geomagnetic activity and "major" solar flares.
7. Exact frame count and frame cadence for the Sun Hero home-screen widget after testing launcher memory, battery, and animation behavior.
8. Whether the Sun Hero widget is best implemented with Glance, traditional RemoteViews, or a hybrid approach after a small technical validation.
