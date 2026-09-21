# HelioFlux Periodic and Manual Data Refresh Design

**Date:** 2026-09-20  
**Status:** Approved

## Goal

Give HelioFlux one coherent refresh model so current scientific data stays current without requiring an app restart, slower sources are refreshed without unnecessary network load, users can force a full refresh from any main screen, and the Space Weather charts that matter most can be refreshed individually.

The design must preserve the existing repository/Room architecture: remote APIs are fetched through repositories, valid responses are persisted, and the UI continues to observe cached repository state rather than consuming network responses directly.

## Problems Being Solved

The current refresh behavior is fragmented:

- most repositories refresh once at application startup;
- the alert worker independently refreshes only Kp, hemispheric power, and flares;
- the Sun widget has its own 30-minute refresh;
- OVATION/Aurora does not have a continuing general refresh path;
- Hemispheric Power needs repeated persistence during the UTC day because NOAA resets its source file at UTC midnight;
- users cannot manually refresh all app data or selectively refresh important Space Weather charts;
- OVATION freshness is evaluated from its observation timestamp even though the globe represents the forecast timestamp, which can make a current forecast appear permanently delayed.

## Design

### 1. One Shared Refresh Coordinator

Evolve the existing `AppRefreshCoordinator` into the single orchestration point for broad application refreshes.

It exposes distinct operations:

- `refreshStartup()` — refresh the normal startup datasets while preserving the Solar Hero's existing cache-aware startup path;
- `refreshFast()` — refresh near-real-time scientific feeds;
- `refreshSlow()` — refresh slower-changing feeds;
- `refreshAll()` — user-initiated full refresh, including the Solar Hero because the user explicitly asked to refresh all screens/data.

Repository-specific refresh methods remain the source of truth for actual networking, validation, persistence, retention, and source status. The coordinator only groups and invokes them.

Failures remain isolated per source. A failure in one refresh action must not cancel unrelated actions or clear valid cached data.

### 2. Fast Refresh Tier — Approximately Every 15 Minutes

Use Android WorkManager periodic work with a network-required constraint. Android may defer execution for battery optimization, so 15 minutes is a requested cadence rather than an exact delivery guarantee.

The fast tier includes:

- Solar-wind magnetic feed:
  - Bz
  - Bt
- Solar-wind plasma feed:
  - speed
  - density
  - temperature
- planetary Kp;
- GOES magnetometer;
- GOES X-ray;
- ACE EPAM;
- NOAA OVATION/Aurora globe;
- Hemispheric Power.

This tier intentionally contains Hemispheric Power so current-day NOAA rows are persisted repeatedly before the upstream file resets at UTC midnight. Existing Room retention then carries prior-day history across the reset.

### 3. Slow Refresh Tier — Approximately Hourly

Use a second network-constrained periodic WorkManager job.

The slow tier includes:

- NOAA forecast discussion;
- flare probabilities;
- DONKI flare events;
- DONKI CME events;
- HMI magnetogram metadata;
- LASCO C2 metadata;
- LASCO C3 metadata;
- HEK active regions;
- ENLIL metadata/frame listing.

The existing Sun Hero remains on its specialized cache/frame pipeline and is not added to either generic periodic tier. The existing Sun widget refresh remains separate at approximately 30 minutes.

### 4. Startup Refresh

Application startup continues to show cached data immediately and then refresh in the background.

Startup should use the shared coordinator rather than maintaining a separate list of refresh calls.

The Solar Hero keeps its existing `refreshIfStale()` ViewModel behavior so startup does not duplicate its frame-loading/cache pipeline.

### 5. Pull-to-Refresh on All Main Screens

Home, Space Weather, and Solar Activity each support native pull-to-refresh.

Pull-to-refresh invokes the shared `refreshAll()` path, regardless of which main screen initiated it. This means the user can refresh the entire app from any primary destination.

Behavior:

- existing cached content remains visible while refresh runs;
- the pull-refresh indicator communicates broad refresh progress;
- one failed source does not fail or blank the whole screen;
- successful repository writes propagate to all screens through their existing Room/Flow observation;
- Solar Hero is included because this is an explicit user-requested full refresh.

The implementation must not introduce an independent networking path in Compose.

### 6. Targeted Space Weather Chart Refresh

Add a small refresh action to these chart cards:

- Bz/Bt → `refreshMagnetic()`;
- Density → `refreshPlasma()`;
- Speed → `refreshPlasma()`;
- Temperature → `refreshPlasma()`;
- GOES Magnetometer → `refreshGoesMagnetometer()`;
- Hemispheric Power → `refreshHemisphericPower()`.

Kp does not receive a per-chart refresh icon in this scope.

The three plasma charts intentionally share one source. Refreshing any one of Density, Speed, or Temperature refreshes the common plasma feed, so all three may update together.

Each targeted refresh action:

- keeps the current chart visible;
- shows that action as busy while its request is in flight;
- ignores duplicate taps for the same source while already refreshing;
- updates through repository persistence and existing observed state;
- preserves cached data on failure.

### 7. Space Weather Gesture Compatibility

Space Weather pull-to-refresh must coexist with the already-working Aurora globe gestures.

The globe currently suppresses parent list scrolling for the lifetime of its active touch stream so SceneView drag/pinch is not cancelled. Preserve that behavior.

Pull-to-refresh must only engage through normal vertical scroll/overscroll behavior when the globe is not actively consuming its gesture stream.

Do not broadly refactor the Filament/SceneView resource or lifecycle pipeline.

### 8. Background Worker Structure

Add two application-level periodic data-refresh workers/schedulers:

- **Live Data Refresh** — approximately every 15 minutes;
- **Slow Data Refresh** — approximately hourly.

Workers obtain the existing repository provider from the application and call the corresponding shared coordinator operation.

Use unique periodic work names and `ExistingPeriodicWorkPolicy.KEEP` so application restarts do not create duplicate schedules.

Both workers require network connectivity.

No foreground service or continuously running process is introduced.

### 9. Alert Worker Deduplication

The alert worker should stop independently downloading Kp, Hemispheric Power, and flare data.

Its job becomes:

1. read the cached repository state already maintained by the periodic refresh workers;
2. evaluate alert policy;
3. persist notification state;
4. post only genuinely new/escalated alerts.

This prevents duplicate network work and keeps refresh ownership centralized.

If required cached data is unavailable or in a failed state, the alert worker may retain its existing retry behavior.

### 10. OVATION Freshness Correction

The Aurora globe represents NOAA's forecast product. Freshness should therefore be evaluated against the OVATION **forecast timestamp**, not the older observation timestamp.

Persistence still retains both observation and forecast timestamps in the Aurora snapshot.

For source freshness metadata, store/evaluate the forecast-valid timestamp so a current forecast is not mislabeled `Delayed` merely because its observation input is older.

Keep the existing OVATION freshness window unless testing against real payload cadence proves a separate threshold is required.

### 11. Refresh State

Broad manual refresh needs one observable busy state so all three main screens can render pull-to-refresh consistently.

Targeted Space Weather refreshes need source-level busy state so:

- magnetic refresh can spin only the Bz/Bt action;
- plasma refresh state is shared by Density, Speed, and Temperature;
- GOES and HP remain independent.

Do not infer refresh activity from repository `Loading` because cached-first repositories intentionally keep usable data available while network work occurs.

### 12. Data Flow

Broad refresh:

```
pull gesture / startup / WorkManager
            ↓
AppRefreshCoordinator
            ↓
repository refresh methods
            ↓
validation + Room persistence
            ↓
repository Flows
            ↓
all observing screens/widgets
```

Targeted chart refresh:

```
chart refresh icon
       ↓
SpaceWeatherViewModel
       ↓
specific repository refresh method
       ↓
Room + repository state
       ↓
chart updates naturally
```

### 13. Testing

Add focused tests for:

- fast/slow/all grouping and source-failure isolation;
- startup continuing to exclude duplicate Solar Hero work;
- manual all-refresh including Solar Hero;
- fast tier including HP and OVATION;
- WorkManager cadence, network constraint, unique work names, and KEEP policy;
- alert repository no longer initiating network refreshes;
- OVATION freshness using forecast timestamp;
- Space Weather targeted refresh routing and duplicate-tap suppression;
- shared busy state for all plasma charts;
- pull-to-refresh callbacks on Home, Space Weather, and Solar Activity;
- chart refresh affordances on magnetic, plasma, GOES, and HP cards;
- globe gesture suppression remaining intact while Space Weather gains pull-to-refresh.

Run the complete Android test/lint/build suite after focused tests.

## Non-Goals

This change does not:

- add a per-chart refresh button to Kp;
- change chart timeframes or chart rendering behavior;
- alter NOAA parsing beyond the OVATION freshness timestamp selection;
- add exact-alarm scheduling;
- promise exact 15-minute execution in the background;
- replace the Solar Hero frame-loading pipeline;
- modify the PWA;
- redesign the Aurora globe or its Filament lifecycle.
