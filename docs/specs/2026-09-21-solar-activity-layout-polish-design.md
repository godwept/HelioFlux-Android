# Solar Activity Layout Polish Design

**Date:** 2026-09-21  
**Status:** Approved

## Goal

Polish three issues found during physical Pixel testing of the refreshed Solar Activity screen without changing its data flow, chart behavior, imagery behavior, or overall information architecture.

This pass should:

- keep the C/M/X probability pills safely below the Android status bar;
- tighten the visual relationship between the X-Ray Activity heading and its chart;
- present Recent Flares and Recent CMEs as a paired, side-by-side card group while preserving the complete event lists.

## Success Criteria

- [ ] C/M/X probability pills no longer collide with the status bar or clock.
- [ ] Top spacing uses system/window insets rather than a device-specific hardcoded workaround.
- [ ] X-Ray Activity heading and chart read as one cohesive visual block.
- [ ] The large empty gap between the X-Ray heading and chart is removed.
- [ ] Recent Flares and Recent CMEs render as two equal-width cards side by side in portrait.
- [ ] The same paired-card treatment is used in the landscape data pane.
- [ ] Each card keeps the full available event list.
- [ ] Loading, empty, cached, failure, and retained-data states remain localized to the correct card.
- [ ] CME Details remains available.
- [ ] No nested scrolling is introduced.
- [ ] Existing chart, imagery, fullscreen, ENLIL, refresh, repository, and ViewModel behavior remains unchanged.

## Scope

### In scope

- Status-bar-safe top spacing for the Solar Activity feed.
- Tightening X-Ray heading-to-chart spacing.
- Grouping X-Ray heading and chart into one locally controlled layout block.
- Replacing the separate Recent Flares and Recent CMEs sections with one paired two-card row.
- Keeping all flare and CME events visible inside their respective cards.
- Retaining existing UTC formatting, semantic colors, state messages, and CME Details.
- Applying the same event-card treatment in compact and expanded Solar Activity layouts.
- Focused tests for the three layout refinements.

### Out of scope

- X-Ray chart rendering internals.
- X-Ray chart height, axes, 72-hour domain, reduction, guides, inspection, or gestures.
- ACE EPAM behavior.
- Solar imagery selector or card changes.
- Fullscreen imagery changes.
- ENLIL playback/lifecycle changes.
- Repository, ViewModel, DAO, parser, networking, database, or refresh scheduling changes.
- Navigation changes.
- Home or Space Weather changes.
- New data or event truncation.
- Nested event scrolling.
- PWA changes.

## Layout and State Model

No new application state is required.

The probability pills continue to use the existing flare-probability repository state.

The X-Ray section continues to use the existing `RepositoryState<List<XrayFluxSample>>` handling and the existing `XrayChart`.

Recent Flares and Recent CMEs continue to receive their existing repository states. The full event lists, state messages, UTC formatting, semantic colors, and CME Details behavior remain unchanged.

No state belongs in the ViewModel for this refinement.

## Top Probability Strip

The probability strip currently begins too close to the physical top edge on the Pixel and can visually collide with system status content.

The Solar Activity screen should apply status-bar-aware top spacing at the screen/layout boundary using the appropriate Compose window-inset API.

The inset should:

- apply only to the top content boundary;
- adapt automatically across devices and orientation;
- avoid adding repeated inset padding to lower sections;
- preserve the existing horizontal content padding and overall spacing rhythm.

`FlareProbabilityBadges` should remain a portable content component and should not own system-window concerns.

## X-Ray Activity Spacing

The X-Ray Activity heading and chart should be composed as one visual block rather than separate lazy-list items with outer list spacing between them.

The shared `HelioFluxSectionHeading` treatment remains.

The chart should begin approximately 8–12dp below the heading's visual endpoint. Implementation should account for the heading's existing internal bottom padding rather than stacking arbitrary additional spacing.

Do not change `XrayChart` or `HelioFluxLineChart`.

## Paired Recent Events Cards

Recent Flares and Recent CMEs should become one paired event area.

In portrait and in the expanded landscape data pane, render:

`[ Recent Flares ]   [ Recent CMEs ]`

The two cards:

- use equal width;
- remain part of the main Solar Activity page scroll;
- do not create nested vertical scrolling;
- contain the complete flare/CME event lists;
- allow natural independent height based on content;
- do not force equal height when one list is shorter.

Each card owns its title inside the card instead of using the large orange-rail section heading above it.

The internal event rows keep the current compact scientific presentation.

### Recent Flares card

Preserve:

- all available flare events;
- class emphasis;
- UTC time;
- observatory / active-region / location metadata;
- loading, empty, cached, retained, and failure messaging.

### Recent CMEs card

Preserve:

- all available CME events;
- speed emphasis;
- UTC time;
- direction / width metadata;
- loading, empty, cached, retained, and failure messaging;
- CME Details action.

If one side is empty/loading/failing while the other contains events, both cards remain visible and independent.

Long metadata wraps inside its own card rather than widening or clipping the pair.

## Component Boundaries

Keep the change local to Solar Activity.

A small internal screen-level paired-events block may be added, conceptually:

`SolarEventCards(flares, cmes, onCmeDetails)`

This block owns:

- the two-column row;
- the two outer cards;
- the card titles.

`FlareList` and `CmeList` continue to own only their state-aware event content.

A small internal X-Ray section block may group the existing heading and chart/state content so spacing is controlled locally.

No new shared abstraction should be introduced unless an existing one already matches the need.

## Edge Cases

- One event card empty while the other is populated: both cards remain side by side.
- One event source loading/failing/cached: only that card reflects the state.
- Long metadata wraps within its card.
- CME Details must remain tappable without affecting the neighboring card.
- A much longer list on one side may make that card taller; this is acceptable.
- No nested scrolling or list truncation should be introduced.
- Do not invent a narrow-width fallback unless physical or automated testing proves the two-card layout unreadable.
- X-Ray spacing should be corrected at the real layout boundary; do not use negative margins to mask unknown internal padding.

## Testing Strategy

Use strict TDD for production changes.

Automated coverage should verify:

- Solar Activity applies status-bar-safe top inset spacing.
- Probability pills remain present and retain existing presentation/state behavior.
- X-Ray Activity heading and chart are grouped into one cohesive section.
- X-Ray chart contracts remain unchanged, including the 72-hour domain.
- Recent Flares and Recent CMEs render inside one paired two-card row in compact layout.
- The same paired event-card row appears in expanded landscape layout.
- Both event cards remain visible for mixed states.
- Full flare/CME event lists remain visible.
- Existing UTC formatting and semantic accents remain unchanged.
- CME Details remains wired.
- No nested vertical scroll container is added to either event card.
- Existing imagery, fullscreen, ENLIL, partial-data, and pull-to-refresh tests remain green.

After implementation, run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Push one focused implementation batch and monitor Android CI to green.

## Physical Pixel Acceptance

Verify in portrait and landscape:

- C/M/X pills sit comfortably below the status bar.
- No status-bar overlap occurs after rotation.
- X-Ray heading-to-chart spacing is compact and intentional.
- Flares and CMEs appear side by side.
- Both cards remain readable with realistic event content.
- Long event lists scroll naturally with the page.
- CME Details remains easy to tap.
- No clipping or unexpected horizontal overflow occurs.
- Existing imagery, fullscreen, ENLIL, X-Ray chart, ACE EPAM, and pull-to-refresh behavior remain intact.

## Open Questions

None. All design decisions required for implementation are approved.
