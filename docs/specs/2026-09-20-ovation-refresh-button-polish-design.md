# OVATION Refresh Reliability and Chart Refresh Button Polish Design

**Date:** 2026-09-20  
**Status:** Approved

## Goal

Fix the post-refresh-system regression where the Aurora globe can settle on `Cached` even though the NOAA OVATION source is available, and make the per-chart refresh actions feel intentional and easier to tap on a physical Pixel.

## Root Cause

The new periodic refresh system introduced two broad refresh paths that can start at the same time:

- application startup immediately starts the fast and slow refresh groups;
- newly enqueued periodic WorkManager requests have no initial delay, so their first constrained run can also begin immediately.

A user pull-to-refresh can additionally overlap either broad refresh.

`AuroraRepository` records success/failure in a single source-status row. If duplicate OVATION requests overlap and one successful request is followed by a slower failed duplicate, the failure becomes the latest source status and the UI correctly renders the retained globe as `Cached`, even though a successful refresh also happened.

The fix is to prevent broad refresh groups from overlapping and to delay the first periodic execution because startup already supplies the immediate refresh.

A genuine single-source OVATION failure must still display `Cached`; this change must not relabel failed data as Fresh.

## Refresh Reliability Changes

- Serialize executions of the fast refresh group.
- Serialize executions of the slow refresh group.
- Make startup call the serialized fast/slow operations instead of bypassing them.
- Make manual `refreshAll()` call the same serialized fast/slow operations.
- Give the 15-minute periodic request a 15-minute initial delay.
- Give the hourly periodic request a 1-hour initial delay.
- Keep individual repository failures isolated within each group.
- Do not change OVATION parsing, Room storage, freshness thresholds, or Filament rendering.

## Chart Refresh Button Polish

Keep the existing location in the upper-right of each supported chart header, but replace the small standalone-looking refresh glyph treatment with a deliberate compact circular control:

- 48dp touch target;
- 36dp visible circular surface;
- subtle neutral Material surface fill;
- subtle outline matching the chart card treatment;
- 24sp refresh glyph;
- 20dp progress spinner while refreshing;
- same accessibility descriptions and disabled/busy behavior.

Use one neutral action treatment across every chart rather than series-specific colors.

## Scope

Affected chart actions remain:

- IMF Bz/Bt;
- Density;
- Speed;
- Temperature;
- GOES Magnetometer;
- Hemispheric Power.

Kp remains unchanged and has no targeted refresh action.

## Testing

Add regression coverage for:

- fast refresh group executions never overlapping;
- periodic workers waiting one cadence before their first run;
- approved 48dp/36dp refresh control dimensions;
- refresh action keeping its accessibility and disabled/busy behavior.

Run the full Android CI-equivalent suite and physically verify the globe status and button appearance on Pixel.

## Non-Goals

- No Aurora globe visual/gesture/lifecycle changes.
- No new refresh icon dependency.
- No change to chart data, scales, timeframe behavior, or Kp.
- No change to NOAA endpoints or freshness thresholds.
