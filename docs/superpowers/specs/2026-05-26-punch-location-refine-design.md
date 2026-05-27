# Punch location refinement + details-sheet feedback — Design

**Date:** 2026-05-26
**Scope:** Both check-in and check-out (consistent — no side-specific behavior).
**Feature area:** Attendance punch (`feature/attendance`), live-location capture (`feature/location`), details sheet (`feature/home`).

---

## 1. Problem

When the user punches in/out, the app captures the committed GPS location (`first_location` / `last_location`) **once**, early — the prefetch fires the moment face verification starts (`startLocationPrefetch`, `AttendanceViewModel` L92–97) and runs `LiveLocationHelper.captureLiveLocation()` (3 attempts, then **freezes** the result).

The user then spends time on the reason / work-report sheets — often 30–60 s. During that whole window the app does nothing further with GPS, and at commit it `await()`s the already-frozen early fix (`completeCheckIn` L847, `completeCheckOut` L1078). On a weak first fix (observed: 19 m indoors) we commit a stale, inaccurate location even though GPS would have sharpened to <5 m if we'd kept sampling.

Two secondary UX gaps in the out-of-range details sheet (`ReasonBottomSheet`):
- The live distance ("You're about 5 m from the location") gives no signal that the app is *still working* — it can look frozen.
- The out-of-range reason input field doesn't show how far out the user actually is.

## 2. Goals

1. **Keep refining the committed location for the whole punch process.** From punch-start until commit, continuously sample high-accuracy GPS and hold the most accurate fix. At commit, use that best fix.
2. **Never block or delay the punch.** If GPS is still above target accuracy at commit, commit the best fix anyway. The server stays the authority on the actual punch decision.
3. **Show live meters at the out-of-range reason input field.**
4. **Show a small spinner next to the live distance label** so the user sees the app is actively working, stopping the instant they're confirmed in range.

## 3. Non-goals / must stay untouched

- Server stays the authority. Local accuracy never gates the commit.
- `t_token` 3-phase protocol, `/check-in` & `/check-out` params, the signature commit contract (`first_location` / `last_location` shape + metadata), and `LocationTrackingService` (the shift-long trail) are **unchanged**.
- `MAX_ACCURACY_METERS` (15 m) and `captureLiveLocation()`'s retry logic are **unchanged** — `captureLiveLocation()` remains the fallback path.
- No new server params, no API contract changes.

## 4. Approach (chosen: continuous refiner)

A continuous high-accuracy `LocationCallback` (mirroring `GeofenceLocationStream`) holds the most-accurate fix across the punch process. At commit it stops and builds the committed `LocationData` from the best fix using the **existing** metadata + mock-check code. This is preferred over looping the one-shot (wasteful: rebuilds metadata each loop, 2 s gaps). The committed-location output is byte-for-byte the same shape as today; only *how the location is chosen* changes.

### 4.1 New unit — `LiveLocationRefiner`

New file: `feature/location/util/LiveLocationRefiner.kt`. One instance held by `AttendanceViewModel` for the lifetime of a punch.

```
class LiveLocationRefiner(context):
    start():
        # mirror GeofenceLocationStream exactly
        LocationRequest(PRIORITY_HIGH_ACCURACY, intervalMs = 1000)
            .setMinUpdateIntervalMillis(500)
        on each fix: keep most accurate; replace if held fix is stale (>10 s)
                     (ignore accuracy <= 0 and (0,0) fixes)
    stop(): removeLocationUpdates
    suspend finish(): LiveLocationResult
        stop()
        best = held best fix
        if best == null -> return LiveLocationHelper(context).captureLiveLocation()  # fallback one-shot
        return LiveLocationHelper(context).buildResult(best)
```

- Best-fix rule + freshness constants are copied from `GeofenceLocationStream` (interval 1000/500 ms, stale = 10 000 ms) so behavior is identical and already field-proven.
- `finish()` is idempotent/safe to call once at commit; if no usable fix was gathered (e.g. permission/GPS edge case) it falls back to the existing one-shot capture, which also surfaces the existing error messaging.

### 4.2 `LiveLocationHelper` refactor (behavior-preserving extraction)

Extract the mock-check + metadata-collection + `LocationData` construction (current `captureLiveLocation()` L94–135) into:

```
fun buildResult(location: Location): LiveLocationResult
```

`captureLiveLocation()` keeps its retry loop unchanged and simply calls `buildResult(bestLocation)` at the end. `LiveLocationRefiner.finish()` reuses the same `buildResult`. Mock-check and metadata (battery/wifi/network/timestamp) are therefore collected at **commit time** for the refiner path — same fields as today, slightly fresher.

> Note: this is the one approved deviation from the "don't touch `captureLiveLocation()`" guardrail. The change is a pure extraction; the retry count, `MAX_ACCURACY_METERS`, and the returned result are unchanged. Verify the extracted path produces identical output before/after.

### 4.3 `AttendanceViewModel` wiring

- Replace `prefetchedLiveLocation: Deferred<LiveLocationResult>?` (L90) with `locationRefiner: LiveLocationRefiner?`.
- `startLocationPrefetch(context)` (L92–97): cancel any existing refiner, create + `start()` a new one. (Called from the same 4 sites: L269, L481 check-in; L367, L460 check-out — unchanged call sites, so both flows get it.)
- `cancelLocationPrefetch()` (L99–102): `stop()` + null the refiner. (Called from the existing cancel paths: pending-message dismiss L499, face-verification cancel L552.)
- Commit await points — `completeCheckIn` L847 and `completeCheckOut` L1078:
  `val locationResult = locationRefiner?.finish() ?: LiveLocationHelper(context).captureLiveLocation()`
  then null the refiner.

Battery: this is a continuous high-accuracy stream during the punch only (screen on, bounded by gather duration, stopped at commit/cancel) — the same cost profile as the existing `GeofenceLocationStream` that already runs on the Home screen.

## 5. UI changes — `ReasonBottomSheet` (`feature/home/.../dialogs/ReasonBottomSheet.kt`)

Both already receive `geoStatus: GeoStatus`. No new wiring.

### 5.1 Spinner next to the distance label (existing block L176–222)

In the distance `Row`, add a small indeterminate `CircularProgressIndicator` (≈14 dp, strokeWidth 2 dp, tinted to the label color), shown only while actively working:

| `geoStatus` | label | spinner |
|-------------|-------|:-------:|
| `OutOfRange(m)` | "You're about *m* m from the location" | ✅ |
| `Acquiring` | "Getting your accurate location…" | ✅ |
| `InRange` | "You're at the location ✓" | — |
| `LocationOff` | "Turn on GPS to verify your location" | — |
| `Disabled` | (hidden) | — |

### 5.2 Live meters at the out-of-range reason input (field at L264–289)

When `outOfRangeReasonRequired`, show a live caption directly under the reason field derived from `geoStatus`:
- `OutOfRange(m)` with `m >= 2` → "**m m away**"
- `OutOfRange(0 or 1)` → "**Right at the edge of the area**" (avoids the misleading "0 m away" on the boundary, observed in logs)
- `InRange` → "**You're at the location ✓**"
- other states → no caption

## 6. Files affected

| File | Change |
|------|--------|
| `feature/location/util/LiveLocationRefiner.kt` | **new** — continuous best-fix refiner |
| `feature/location/util/LiveLocationHelper.kt` | extract `buildResult(location)`; `captureLiveLocation()` calls it (behavior-preserving) |
| `feature/attendance/presentation/AttendanceViewModel.kt` | swap prefetch Deferred → refiner; update `startLocationPrefetch` / `cancelLocationPrefetch` / two commit await points |
| `feature/home/.../dialogs/ReasonBottomSheet.kt` | spinner in distance row; meters caption under out-of-range input |

## 7. Testing

- **Unit:** extract the best-fix selection rule (most-accurate, stale-replacement, reject ≤0 / 0,0) so it's pure-testable; add tests under `app/src/test/`. Verify `buildResult` extraction is output-equivalent.
- **Manual (logcat):** punch out from a weak-GPS spot; confirm the committed accuracy at commit is the best seen across the gather window (not the early frozen fix); confirm spinner shows while out-of-range and stops on confirm; confirm "X m away" / "right at the edge" caption under the field; confirm `captureLiveLocation()` fallback still fires when no fix gathered.
- **Regression:** check-in and check-out both still commit with a valid `first_location` / `last_location`; tracking service still starts on check-in / stops on check-out; never blocks when GPS stays >15 m.
