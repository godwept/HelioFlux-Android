# LaTiS HMIBC Magnetogram Source Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal

Replace only the Android app's current HMI magnetogram image acquisition with the current colorized HMI magnetogram (HMIBC) exposed through LASP LaTiS/CCMC ISWA, while preserving the existing Solar Activity UI, active-region overlay, caching/freshness behavior, and every other working data source.

## Success Criteria

- [ ] HMI uses the newest LaTiS `iswa_sdo_aia_hmic_files` sample rather than the existing Cloudflare-worker HMIB image.
- [ ] The app displays the returned 2048x2048 `HMIBC.jpg` URL.
- [ ] The LaTiS sample timestamp becomes the magnetogram observation/source timestamp.
- [ ] A failed/empty/malformed LaTiS lookup does not replace the previous HMIBC image.
- [ ] When refresh fails and a previous HMIBC image exists, the existing repository behavior presents that retained image as `Cached`.
- [ ] No fallback to the old black-and-white HMIB image is added.
- [ ] No unrelated endpoint, worker route, screen, imagery source, or active-region behavior changes.

## Scope

**In scope:**
- Direct Android request to LASP LaTiS for the latest HMIBC metadata record.
- Parsing the LaTiS JSON response containing `timestamp` and `url`.
- Persisting the returned HMIBC URL and timestamp through the existing `SolarImage` persistence path.
- Focused unit coverage for successful refresh and failure-with-retained-data behavior.

**Out of scope:**
- Cloudflare worker changes.
- DONKI, Helioviewer, HEK, LASCO, ENLIL, NOAA, or ACE EPAM endpoint changes.
- Fixing or redesigning active-region labels/overlay.
- Changes to the Solar Activity gallery, full-screen viewer, zoom/pan, layout, or styling.
- Adding a fallback to the legacy HMIB image.
- Reworking general repository caching or freshness policy.

## Design

### Data source

Use the LASP Space Weather Portal LaTiS dataset:

`iswa_sdo_aia_hmic_files`

The metadata request should ask LaTiS for only the newest record and only the fields the app needs:

`project(time,url)`

with a server-side latest-record operation such as `takeRight(1)`.

A known-good LaTiS response has this shape:

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

The Android app should request the LaTiS metadata directly. The Cloudflare worker is not part of this new HMI path.

### Repository behavior

`SolarImageryRepository.refreshAll()` should continue refreshing the same sources in the same order. Only the Magnetogram refresh path changes.

For the magnetogram refresh:

1. Mark the existing HMI data source as attempted.
2. Fetch the latest LaTiS HMIBC metadata.
3. Require a successful HTTP response.
4. Parse exactly one valid sample containing a timestamp and HTTPS image URL.
5. Convert the sample timestamp to epoch milliseconds.
6. Persist `SolarImage(SolarImageType.Magnetogram, timestamp, returnedUrl)`.
7. Mark the HMI source refresh successful using the LaTiS observation timestamp.

The existing generic fixed-image refresh path remains available for LASCO and any other current callers; it should not be broadly redesigned just to support HMI.

### Failure and freshness behavior

If the LaTiS request fails, returns a non-2xx status, contains no samples, has malformed JSON, has an invalid timestamp, or has no usable image URL:

- record the HMI refresh failure using the repository's existing status mechanism;
- do not overwrite/delete the previously persisted magnetogram;
- do not fetch the legacy black-and-white HMIB image;
- allow the existing `RepositoryState.Failure` + retained-data logic to return the previous HMIBC image with `DataFreshness.Cached`.

A successfully fetched but old LaTiS observation continues to use the existing imagery freshness evaluation; no new freshness thresholds are introduced.

### Active-region overlay

The current active-region/HEK data path and `ActiveRegionOverlay` are unchanged. This change only replaces the base HMI image. Missing labels remain a separate bug to investigate after the HMIBC source is physically verified.

### Interfaces

Keep the existing `SolarImage`, `SolarImageType.Magnetogram`, DAO, and UI contracts unchanged.

Introduce only the smallest parsing/helper surface needed for the LaTiS response. Avoid a general-purpose LaTiS imagery framework unless another current use case requires it.

## Testing Strategy

Use strict TDD.

1. Add a repository/parser test with the observed LaTiS JSON shape and assert that refresh persists:
   - `SolarImageType.Magnetogram`;
   - the returned ISWA HMIBC URL;
   - the parsed LaTiS observation timestamp.
2. Add/adjust a failure regression test:
   - seed a previously successful HMIBC image;
   - make the LaTiS metadata refresh fail;
   - assert the stored image remains unchanged;
   - assert repository state reports failure with retained data and `Cached` freshness.
3. Verify LASCO refresh tests remain unchanged and green, proving the generic fixed-image path was not broken.
4. Run the full unit, instrumentation-compilation, lint, and debug-APK gates.
5. Push one focused implementation batch and monitor Android CI to green.
6. Physically verify on the Pixel that the Solar Activity HMI view shows the current color HMIBC image and that a failed refresh retains the last image as `Cached`.

## Open Questions

None. Active-region labels are explicitly deferred to a separate bug investigation after the new HMIBC base image is verified.
