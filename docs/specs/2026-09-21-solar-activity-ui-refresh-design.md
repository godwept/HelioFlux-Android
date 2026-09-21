# Solar Activity UI Refresh Design

**Date:** 2026-09-21  
**Status:** Approved

## Goal

Refresh the native Android Solar Activity screen so it has the same polished, scientific visual language as the current Home and Space Weather screens while preserving the working Solar Activity data, chart, imagery, refresh, and detail behavior. Portrait should become a clean true-black scientific feed, while landscape should deliberately use the available width with a fixed Solar Imagery pane on the left and an independently scrollable science/data pane on the right.

## Success Criteria

- [ ] Solar Activity uses a true-black canvas and the shared HelioFlux section-heading treatment.
- [ ] The generic `Solar Activity` page-title row is removed in favor of stronger section hierarchy.
- [ ] C/M/X flare probabilities use compact scientific metric pills rather than generic Material chips.
- [ ] Portrait and landscape both use the same HMI / C2 / C3 / ENLIL imagery selector instead of the horizontal imagery carousel.
- [ ] HMI is the default selected imagery source.
- [ ] Portrait presents one large selected imagery item in the main feed.
- [ ] Landscape keeps one large selected imagery item fixed in the left pane while science/data content scrolls independently on the right.
- [ ] Tapping HMI, C2, C3, or ENLIL opens a true edge-to-edge fullscreen viewer on black.
- [ ] HMI, C2, and C3 retain existing zoom/pan behavior in fullscreen.
- [ ] HMI active-region overlays remain visible and aligned.
- [ ] ENLIL continues its existing blended animation in fullscreen rather than falling back to a static frame.
- [ ] X-Ray and ACE EPAM preserve the existing shared chart engine and 72-hour presentation behavior.
- [ ] Recent Flares and Recent CMEs become compact scientific event rows with human-readable UTC timestamps.
- [ ] Existing CME detail access is preserved.
- [ ] Loading, empty, failure, retained-data, and pull-to-refresh behavior remain section-local and resilient.
- [ ] The screen avoids unnecessary eager work where the redesign naturally permits it, without speculative broad refactoring.
- [ ] Existing Solar Activity data/repository behavior remains unchanged.

## Scope

**In scope:**

- True-black Solar Activity canvas.
- Shared `HelioFluxSectionHeading` hierarchy for:
  - Solar Imagery
  - X-Ray Activity
  - Recent Flares
  - Recent CMEs
  - Particle Environment
- Removal of the generic Solar Activity page-title row.
- Compact C/M/X probability pills using established HelioFlux metric styling and semantic class colors.
- One shared imagery-selection model for portrait and landscape:
  - HMI
  - LASCO C2
  - LASCO C3
  - WSA-Enlil
- Large selected imagery presentation with strong title/source/time hierarchy.
- True fullscreen imagery presentation.
- Existing fullscreen image zoom/pan behavior.
- HMI active-region overlays.
- Animated ENLIL fullscreen playback using the existing blend pipeline.
- Portrait feed restructuring so expensive content is not unnecessarily composed all at once.
- Landscape split layout with a fixed imagery pane and scrollable science/data pane.
- Presentation polish around existing X-Ray and ACE EPAM charts without changing chart-engine behavior.
- Compact scientific flare/CME event rows.
- Human-readable UTC event and imagery timestamps.
- Removal of clearly redundant UI work, such as re-sorting flare/CME data that already arrives newest-first.
- Focused regression tests for the redesigned presentation and preserved behaviors.

**Out of scope:**

- Repository, DAO, ViewModel, networking, parser, database, or refresh-scheduling changes.
- New solar data sources or new Solar Activity features.
- X-Ray/ACE chart rendering, axes, reduction, inspection, reference-guide, or gesture rewrites.
- New timeframe controls for Solar Activity.
- Home or Space Weather changes.
- HMI active-region coordinate/mapping changes.
- ENLIL blend timing or animation-pipeline redesign.
- Coil/image-cache redesign.
- Download/share actions in fullscreen.
- Playback controls or image-navigation carousel in fullscreen.
- PWA modifications.
- Broad performance refactors not demonstrated by a reproducible problem.

## Design

### Visual hierarchy

Solar Activity should read as a scientific instrument surface rather than a stack of generic Material cards.

The screen uses a true-black background. Section titles use the shared `HelioFluxSectionHeading` treatment already established on Home and Space Weather: normal app UI typography, restrained Solar Orange rail, and consistent section spacing.

The top-level `Solar Activity` title row is removed. Navigation already establishes the destination, so the screen should open directly with the at-a-glance flare-probability strip and then the first major section.

### Flare probability strip

The existing Material `AssistChip` presentation is replaced by three compact scientific metric pills for C, M, and X class.

Each pill should:

- show the class label clearly;
- center a strong percentage value;
- use the established rounded HelioFlux metric treatment;
- use semantic class coloring:
  - C: caution/yellow;
  - M: orange;
  - X: red;
- remain visually integrated with the black canvas rather than looking like standalone Material controls.

The pills are informational only; no new tap behavior is introduced.

### Imagery selection model

Portrait and landscape use the same imagery-selection interaction.

The selector contains four compact choices:

`HMI | C2 | C3 | ENLIL`

It should look like a restrained HelioFlux segmented control rather than platform tabs: dark background, subtle border, Solar Orange selected state, short labels, and only a subtle selection transition.

HMI is the default selection.

Selection state is local presentation state. It does not belong in the ViewModel and does not need persistence across process death.

Only the selected source is presented as the primary imagery item at one time. No horizontal imagery carousel remains.

### Portrait layout

Portrait is one vertically scrolling Solar Activity feed on a true-black canvas.

Visible order:

1. C/M/X flare probability pills
2. Solar Imagery heading
3. HMI / C2 / C3 / ENLIL selector
4. selected imagery
5. X-Ray Activity
6. Recent Flares
7. Recent CMEs
8. Particle Environment

The outer feed should use an appropriate lazy/block-based structure so the long screen does not eagerly compose every section without need. Event sections remain part of the main page flow; do not introduce nested vertical scrolling.

### Landscape layout

Landscape uses a two-pane layout modeled on the successful Home and Space Weather adaptive approach.

**Left fixed pane:**

- Solar Imagery heading
- HMI / C2 / C3 / ENLIL selector
- large selected imagery item

**Right scrollable pane:**

1. C/M/X flare probability pills
2. X-Ray Activity
3. Recent Flares
4. Recent CMEs
5. Particle Environment

The left imagery pane remains fixed while the right pane owns vertical scrolling and pull-to-refresh. The design should use proportions close to the existing 43% / 57% Home/Space Weather split unless the actual Solar Activity content needs a small local adjustment during implementation to avoid clipping.

Do not add a second independent vertical scroll inside event lists.

### Imagery presentation

HMI, LASCO C2, LASCO C3, and ENLIL should sit on deliberate black-backed imagery stages with restrained borders/accent treatment.

Each selected visual should have a clear hierarchy for:

- title;
- source;
- updated/run time in human-readable UTC.

Keep aspect ratios consistent and favor a large square stage for the selected solar imagery.

Existing HMI active-region labels remain supported.

### Fullscreen imagery

Tapping the selected imagery opens a true edge-to-edge fullscreen viewer on black instead of a card-sized dialog.

For HMI, C2, and C3:

- initially fit the whole image to the available display;
- preserve existing pinch-to-zoom and pan behavior;
- allow substantial zoom for detail inspection;
- keep system/chrome intrusion minimal.

For HMI, active-region overlays remain visible and aligned in fullscreen.

For ENLIL:

- fullscreen remains animated;
- preserve the existing 200 ms blended-frame playback behavior;
- do not replace the animation with a still image;
- do not add new playback controls.

A minimal floating close control is sufficient. Do not add metadata drawers, download buttons, share controls, or a fullscreen image carousel.

Closing fullscreen returns to the same selected imagery source.

### X-Ray Activity and Particle Environment

The existing `HelioFluxLineChart` behavior is already the approved chart foundation and should not be reimplemented.

X-Ray Activity keeps:

- the existing GOES series;
- the current 72-hour domain;
- current axes;
- C/M/X reference guides;
- inspection behavior;
- reduction behavior.

Particle Environment keeps the existing ACE EPAM chart engine and series behavior.

This redesign only changes surrounding section/card/header/spacing treatment so both charts visually belong to the refreshed Solar Activity screen.

### Recent Flares

Replace large generic event cards with compact scientific rows.

Each flare row should prioritize:

- flare class;
- human-readable UTC time.

Secondary metadata should include the existing observatory, active region, and location data when present.

Flare class uses semantic emphasis consistent with the existing domain grouping:

- A/B: lower emphasis;
- C: cyan/caution emphasis as appropriate to the existing HelioFlux palette;
- M: orange;
- X: red.

Do not add nested vertical scrolling. Events remain in the main feed.

### Recent CMEs

Replace large generic CME cards with compact scientific rows.

Each row should prioritize:

- CME speed;
- human-readable UTC time.

Secondary metadata keeps existing direction and width information. Existing CME details access is preserved.

Use the existing speed-emphasis thresholds:

- below 500 km/s: normal;
- 500–999 km/s: elevated;
- 1000+ km/s: strong.

Do not add nested vertical scrolling.

### State model

No repository or ViewModel state changes are required.

`SolarActivityUiState` remains the source of truth for probabilities, HMI/regions, LASCO imagery, ENLIL, X-Ray, flares, CMEs, and ACE EPAM.

The only new screen state is the selected imagery source. It is local Compose presentation state and defaults to HMI.

Small immutable presentation helpers/models may be introduced only where they make formatting and testing simpler, such as:

- flare probability label/value/accent;
- imagery title/source/formatted UTC time;
- flare class/time/metadata/accent;
- CME speed/time/metadata/emphasis.

Do not duplicate domain data or create a new screen state machine.

### Loading, failure, and retained data

Solar Activity retains section-level resilience.

If fresh data exists, show it normally.

If refresh fails but retained data exists, keep showing retained content with the existing stale/failure indication rather than replacing the entire section.

If no usable data exists, show a compact inline unavailable/empty state within that section.

Imagery failures remain isolated to the selected source. The imagery selector stays usable so the user can switch to another source.

Fullscreen opens only when there is usable content to display.

Pull-to-refresh continues to use the existing Solar Activity refresh path. Do not add separate retry buttons, modal errors, snackbars, or alternate refresh state.

### Performance constraints

The redesign may remove obvious redundant UI work that is already proven unnecessary, including flare/CME sorting in composables when repository/DAO output is already newest-first.

The portrait outer feed should avoid eagerly composing the entire long screen where a lazy structure is straightforward.

Only the selected imagery item should be actively presented by the gallery component.

The currently observed ENLIL behavior—being treated as visible while the eager Solar Activity screen remains composed—may represent a real off-screen animation/performance defect. That must not be silently fixed as part of styling. If implementation confirms a behavioral/performance bug, stop and route that correction through the project `$debugprompt` workflow with a focused reproduction before changing the playback/lifecycle behavior.

Do not speculate into chart memoization, image-cache rewrites, or broad lifecycle refactors without a demonstrated problem.

## Testing Strategy

Use strict TDD for every production change.

Focused automated coverage should verify:

- C/M/X probability labels, values, and semantic presentation.
- Approved section headings and ordering.
- Removal of the generic Solar Activity page-title row.
- HMI is the default selected imagery source.
- The same HMI/C2/C3/ENLIL selector exists in portrait and landscape.
- Selector changes switch the primary imagery source.
- Portrait uses one selected imagery presentation rather than the old horizontal carousel.
- Landscape keeps imagery in the fixed left pane and science/data in the independently scrollable right pane.
- Fullscreen opens from each usable imagery type.
- Fullscreen image surface is edge-to-edge rather than card-sized.
- HMI fullscreen preserves region overlays and existing transform behavior.
- ENLIL fullscreen preserves animated blended playback.
- Exiting fullscreen returns to the selected imagery source.
- Human-readable UTC formatting for imagery, flare, and CME timestamps.
- Flare class grouping/emphasis remains correct.
- CME speed-emphasis thresholds remain correct.
- CME details remain reachable.
- Existing 72-hour X-Ray chart contract remains unchanged.
- Existing ACE EPAM chart contract remains unchanged.
- Loading, empty, failure, retained-data, and mixed partial-data rendering remain section-local.
- Pull-to-refresh remains wired.
- Existing ENLIL blend/playback unit contracts remain green unless a separately debugged defect requires a focused change.
- Existing HMI region-mapping tests remain green.

After focused tests, run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

After pushing the implementation batch, actively monitor Android CI to green. If CI fails, inspect and reproduce the failure before making the minimum correction.

Finally, perform a physical Pixel acceptance pass in portrait and landscape covering:

- section hierarchy and spacing;
- probability pills;
- imagery selector;
- HMI/C2/C3/ENLIL switching;
- true fullscreen imagery;
- HMI zoom/pan and active-region overlay alignment;
- ENLIL animation in normal and fullscreen presentation;
- portrait scrolling;
- landscape fixed imagery pane and independent right-pane scrolling;
- flare/CME readability;
- X-Ray and ACE chart presentation;
- pull-to-refresh;
- rotation back and forth without duplicated/clipped content.

Any device-only defect discovered during acceptance starts the project `$debugprompt` workflow before code changes.

## Open Questions

None. All design decisions required for implementation are approved.
