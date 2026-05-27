# Punch Location Refinement + Details-Sheet Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continuously refine the committed punch location for the whole check-in/out process (commit the best fix, never block), and give the out-of-range details sheet a live spinner + a "metres away" caption at the reason field.

**Architecture:** A new `LiveLocationRefiner` runs a continuous high-accuracy `LocationCallback` (mirroring `GeofenceLocationStream`) from punch-start to commit, holding the most-accurate fix via a pure `BestFixPolicy` rule. At commit it builds the `first_location`/`last_location` payload through `LiveLocationHelper`'s existing metadata code (extracted into `buildResult`), so the wire shape is unchanged. `captureLiveLocation()` stays as the fallback. Two Compose tweaks in `ReasonBottomSheet`.

**Tech Stack:** Kotlin, Coroutines, Play Services Location (FusedLocationProviderClient), Jetpack Compose / Material 3, JUnit 4.

**Build/test commands** (PowerShell, repo root):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1   # compile main + JVM tests
.\gradlew.bat :app:assembleDebug --offline --console=plain --max-workers=1       # full debug APK
```
> Do **not** combine `testDebugUnitTest` with `assembleDebug` in one invocation (host OOMs). Run separately.

**Commit note:** Repo is on `main` with in-progress geofence work in the tree. Do all commits for this plan on a dedicated branch (Task 0); never `git add -A` — stage only the files each task names.

---

### Task 0: Create a working branch

**Files:** none (git only)

- [ ] **Step 1: Branch off main**

```powershell
git checkout -b feat/punch-location-refine
```

- [ ] **Step 2: Confirm branch**

Run: `git rev-parse --abbrev-ref HEAD`
Expected: `feat/punch-location-refine`

---

### Task 1: `BestFixPolicy` (pure best-fix rule, TDD)

**Files:**
- Create: `app/src/main/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicy.kt`
- Test: `app/src/test/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicyTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicyTest.kt`:

```kotlin
package com.hrms.jeejateamozy.feature.location.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BestFixPolicyTest {

    @Test
    fun `takes first valid fix when none held`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = null, heldAgeMs = 0L, newAccuracyM = 30f))
    }

    @Test
    fun `rejects non-positive accuracy even when none held`() {
        assertFalse(BestFixPolicy.shouldReplace(null, 0L, 0f))
        assertFalse(BestFixPolicy.shouldReplace(null, 0L, -1f))
    }

    @Test
    fun `replaces held fix with a more accurate one`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 20f, heldAgeMs = 1_000L, newAccuracyM = 8f))
    }

    @Test
    fun `keeps fresh held fix when new one is less accurate`() {
        assertFalse(BestFixPolicy.shouldReplace(heldAccuracyM = 8f, heldAgeMs = 1_000L, newAccuracyM = 20f))
    }

    @Test
    fun `equal accuracy replaces to prefer newer`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 10f, heldAgeMs = 1_000L, newAccuracyM = 10f))
    }

    @Test
    fun `replaces a stale held fix even with a less accurate new one`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 5f, heldAgeMs = 10_001L, newAccuracyM = 40f))
    }

    @Test
    fun `does not replace held fix at exactly the stale boundary`() {
        assertFalse(BestFixPolicy.shouldReplace(heldAccuracyM = 5f, heldAgeMs = 10_000L, newAccuracyM = 40f))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: FAIL — `Unresolved reference: BestFixPolicy`.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicy.kt`:

```kotlin
package com.hrms.jeejateamozy.feature.location.util

/**
 * Pure decision rule for the continuous commit-location refiner: should a freshly-arrived GPS fix
 * replace the best fix held so far? Mirrors GeofenceLocationStream's "keep the most accurate fix,
 * but accept a newer one if the held fix has gone stale" rule, plus a guard that rejects fixes
 * whose accuracy is non-positive (unknown). Android-free so it is unit-testable.
 */
object BestFixPolicy {

    /** A held fix older than this is replaced even by a less-accurate newer fix (ms). */
    const val STALE_MS = 10_000L

    /**
     * @param heldAccuracyM accuracy of the best fix held so far, or null if none held yet
     * @param heldAgeMs     age of the held fix in ms (ignored when [heldAccuracyM] is null)
     * @param newAccuracyM  accuracy of the newly-arrived fix
     * @return true if the new fix should become the held best fix
     */
    fun shouldReplace(heldAccuracyM: Float?, heldAgeMs: Long, newAccuracyM: Float): Boolean {
        if (newAccuracyM <= 0f) return false        // unknown/invalid accuracy — never trust it
        if (heldAccuracyM == null) return true       // nothing held yet — take the first valid fix
        return newAccuracyM <= heldAccuracyM || heldAgeMs > STALE_MS
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: PASS (all `BestFixPolicyTest` green; pre-existing tests still green).

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicy.kt app/src/test/java/com/hrms/jeejateamozy/feature/location/util/BestFixPolicyTest.kt
git commit -m "feat(location): add pure BestFixPolicy rule for commit-location refiner"
```

---

### Task 2: Extract `buildResult()` in `LiveLocationHelper` (behavior-preserving)

**Files:**
- Modify: `app/src/main/java/com/hrms/jeejateamozy/feature/location/util/LiveLocationHelper.kt:84-141`

- [ ] **Step 1: Replace the tail of `captureLiveLocation()` and add `buildResult()`**

In `captureLiveLocation()`, replace this block (currently lines 84–140, from `val location = bestLocation` through the closing of the `try`)…

```kotlin
            val location = bestLocation
            if (location == null) {
                Log.e(TAG, "❌ Location timeout after $MAX_RETRIES attempts")
                return LiveLocationResult.Error("Unable to get location (timeout)")
            }

            if (location.accuracy > MAX_ACCURACY_METERS) {
                Log.w(TAG, "⚠️ Best accuracy=${location.accuracy}m exceeds ${MAX_ACCURACY_METERS}m limit")
            }

            // Check for mock/fake GPS
            val isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }
            if (isMocked) {
                Log.e(TAG, "🚫 Mock location detected!")
                return LiveLocationResult.Error("Fake GPS detected. Please disable mock location apps.")
            }

            // Collect all device metadata (with safe handling for missing permissions)
            val deviceId = getDeviceId()
            val appVersion = BuildConfig.VERSION_NAME
            val batteryLevel = getBatteryLevel()
            val networkType = getNetworkType()
            val wifiInfo = getWifiInfoSafe()  // Safe version
            val recordedAt = getCurrentISOTimestamp()

            val locationData = LocationData(
                recordedAt = recordedAt,
                latitude = location.latitude,
                longitude = location.longitude,
                locationAccuracy = location.accuracy,
                altitude = if (location.hasAltitude()) location.altitude else null,
                verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters
                } else null,
                speed = if (location.hasSpeed()) location.speed else null,
                heading = if (location.hasBearing()) location.bearing else null,
                deviceId = deviceId,
                appVersion = appVersion,
                networkType = networkType,
                wifiName = wifiInfo.first,
                wifiMacAddress = wifiInfo.second,
                batteryLevel = batteryLevel,
                geofenceId = null // Will be determined by server
            )

            Log.d(TAG, "✅ Live location captured: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
            LiveLocationResult.Success(locationData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error capturing location", e)
            LiveLocationResult.Error(e.message ?: "Failed to capture location")
        }
    }
```

…with this (loop selects the fix; `buildResult` does mock-check + metadata + `LocationData`):

```kotlin
            val location = bestLocation
            if (location == null) {
                Log.e(TAG, "❌ Location timeout after $MAX_RETRIES attempts")
                return LiveLocationResult.Error("Unable to get location (timeout)")
            }

            if (location.accuracy > MAX_ACCURACY_METERS) {
                Log.w(TAG, "⚠️ Best accuracy=${location.accuracy}m exceeds ${MAX_ACCURACY_METERS}m limit")
            }

            buildResult(location)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error capturing location", e)
            LiveLocationResult.Error(e.message ?: "Failed to capture location")
        }
    }

    /**
     * Build the committed [LiveLocationResult] from an already-chosen [location]: mock-check, then
     * collect device metadata at this moment, then assemble [LocationData]. Shared by the one-shot
     * [captureLiveLocation] and the continuous [LiveLocationRefiner] so both produce an identical
     * payload shape. Returns an Error if the fix is from a mock provider.
     */
    fun buildResult(location: Location): LiveLocationResult {
        // Check for mock/fake GPS
        val isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
        if (isMocked) {
            Log.e(TAG, "🚫 Mock location detected!")
            return LiveLocationResult.Error("Fake GPS detected. Please disable mock location apps.")
        }

        // Collect all device metadata (with safe handling for missing permissions)
        val deviceId = getDeviceId()
        val appVersion = BuildConfig.VERSION_NAME
        val batteryLevel = getBatteryLevel()
        val networkType = getNetworkType()
        val wifiInfo = getWifiInfoSafe()  // Safe version
        val recordedAt = getCurrentISOTimestamp()

        val locationData = LocationData(
            recordedAt = recordedAt,
            latitude = location.latitude,
            longitude = location.longitude,
            locationAccuracy = location.accuracy,
            altitude = if (location.hasAltitude()) location.altitude else null,
            verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                location.verticalAccuracyMeters
            } else null,
            speed = if (location.hasSpeed()) location.speed else null,
            heading = if (location.hasBearing()) location.bearing else null,
            deviceId = deviceId,
            appVersion = appVersion,
            networkType = networkType,
            wifiName = wifiInfo.first,
            wifiMacAddress = wifiInfo.second,
            batteryLevel = batteryLevel,
            geofenceId = null // Will be determined by server
        )

        Log.d(TAG, "✅ Live location captured: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
        return LiveLocationResult.Success(locationData)
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: PASS (compiles; existing tests + `BestFixPolicyTest` green). No behavior change.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/feature/location/util/LiveLocationHelper.kt
git commit -m "refactor(location): extract LiveLocationHelper.buildResult() (no behavior change)"
```

---

### Task 3: `LiveLocationRefiner`

**Files:**
- Create: `app/src/main/java/com/hrms/jeejateamozy/feature/location/util/LiveLocationRefiner.kt`

- [ ] **Step 1: Create the refiner**

```kotlin
package com.hrms.jeejateamozy.feature.location.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Continuously samples high-accuracy GPS for the duration of a punch (check-in/out) and holds the
 * single most-accurate fix seen. At commit, [finish] turns that best fix into the committed
 * LiveLocationResult via [LiveLocationHelper.buildResult], so first_location / last_location is
 * identical in shape to the one-shot path.
 *
 * Mirrors GeofenceLocationStream's request config + keep-best rule (via [BestFixPolicy]). Display
 * and auto-reverify use that stream; this refiner exists only to sharpen the committed location
 * while the user fills the reason / work-report sheets. Never blocks the punch — if no usable fix
 * was gathered, [finish] falls back to [LiveLocationHelper.captureLiveLocation].
 */
class LiveLocationRefiner(private val context: Context) {

    private companion object { const val TAG = "LiveLocationRefiner" }

    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val helper by lazy { LiveLocationHelper(context) }

    @Volatile private var best: Location? = null
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (callback != null) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (loc.latitude == 0.0 && loc.longitude == 0.0) return
                val held = best
                val heldAgeMs = held?.let {
                    (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000L
                } ?: 0L
                if (BestFixPolicy.shouldReplace(held?.accuracy, heldAgeMs, loc.accuracy)) {
                    best = loc
                }
            }
        }
        callback = cb
        fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
        Log.d(TAG, "📍 Refiner started")
    }

    fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        Log.d(TAG, "📍 Refiner stopped")
    }

    /**
     * Stop sampling and build the committed location from the best fix gathered. Falls back to a
     * one-shot capture if nothing usable was gathered. Safe to call once at commit.
     */
    suspend fun finish(): LiveLocationResult {
        stop()
        val b = best
        return if (b != null) {
            Log.d(TAG, "📍 Refiner finishing with best acc=${b.accuracy}m")
            helper.buildResult(b)
        } else {
            Log.w(TAG, "📍 Refiner had no fix — falling back to one-shot capture")
            helper.captureLiveLocation()
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: PASS (compiles; tests green).

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/feature/location/util/LiveLocationRefiner.kt
git commit -m "feat(location): add LiveLocationRefiner (continuous best-fix capture)"
```

---

### Task 4: Wire the refiner into `AttendanceViewModel`

**Files:**
- Modify: `app/src/main/java/com/hrms/jeejateamozy/feature/attendance/presentation/AttendanceViewModel.kt` (import ~L40, field L90, `startLocationPrefetch` L92-97, `cancelLocationPrefetch` L99-102, commit await L847-849 and L1078-1080)

- [ ] **Step 1: Add the import**

After the existing line `import com.hrms.jeejateamozy.feature.location.util.LiveLocationHelper` add:

```kotlin
import com.hrms.jeejateamozy.feature.location.util.LiveLocationRefiner
```

- [ ] **Step 2: Replace the prefetch field**

Replace:

```kotlin
    // Pre-fetch live location during face verification so it's ready instantly after
    private var prefetchedLiveLocation: Deferred<LiveLocationResult>? = null
```

with:

```kotlin
    // Continuously refine the committed punch location from punch-start to commit (holds the best
    // fix). Started at face verification, finished at commit. See LiveLocationRefiner.
    private var locationRefiner: LiveLocationRefiner? = null
```

- [ ] **Step 3: Replace `startLocationPrefetch` and `cancelLocationPrefetch`**

Replace:

```kotlin
    private fun startLocationPrefetch(context: Context) {
        prefetchedLiveLocation?.cancel()
        prefetchedLiveLocation = viewModelScope.async {
            LiveLocationHelper(context).captureLiveLocation()
        }
    }

    private fun cancelLocationPrefetch() {
        prefetchedLiveLocation?.cancel()
        prefetchedLiveLocation = null
    }
```

with:

```kotlin
    private fun startLocationPrefetch(context: Context) {
        locationRefiner?.stop()
        locationRefiner = LiveLocationRefiner(context).also { it.start() }
    }

    private fun cancelLocationPrefetch() {
        locationRefiner?.stop()
        locationRefiner = null
    }
```

- [ ] **Step 4: Replace the check-in commit await**

In `completeCheckIn`, replace:

```kotlin
                Log.d(TAG, "📍 Getting live location for check-in...")
                val locationResult = prefetchedLiveLocation?.await()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                prefetchedLiveLocation = null
```

with:

```kotlin
                Log.d(TAG, "📍 Getting live location for check-in...")
                val locationResult = locationRefiner?.finish()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                locationRefiner = null
```

- [ ] **Step 5: Replace the check-out commit await**

In `completeCheckOut`, replace:

```kotlin
                Log.d(TAG, "📍 Getting live location for check-out...")
                val locationResult = prefetchedLiveLocation?.await()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                prefetchedLiveLocation = null
```

with:

```kotlin
                Log.d(TAG, "📍 Getting live location for check-out...")
                val locationResult = locationRefiner?.finish()
                    ?: LiveLocationHelper(context).captureLiveLocation()
                locationRefiner = null
```

- [ ] **Step 6: Verify it compiles**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: PASS. If the compiler errors on unused `import kotlinx.coroutines.Deferred` / `import kotlinx.coroutines.async`, that's only a warning — but if they are now unused you may remove those two import lines to keep it clean (re-run to confirm still green).

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/feature/attendance/presentation/AttendanceViewModel.kt
git commit -m "feat(attendance): use LiveLocationRefiner for committed punch location (both flows)"
```

---

### Task 5: `ReasonBottomSheet` — spinner + metres caption

**Files:**
- Modify: `app/src/main/java/com/hrms/jeejateamozy/feature/home/dialogs/ReasonBottomSheet.kt` (distance Row ~L201-221; reason input ~L264-291)

- [ ] **Step 1: Add the spinner to the distance Row**

Replace:

```kotlin
                if (distLabel != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = distColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = distLabel,
                            color = distColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
```

with:

```kotlin
                if (distLabel != null) {
                    // Spinner spins while we're still working (out-of-range / acquiring) so the
                    // employee sees the app is live; it disappears the instant they're confirmed.
                    val showSpinner = geoStatus is GeoStatus.OutOfRange || geoStatus is GeoStatus.Acquiring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = distColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = distLabel,
                            color = distColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (showSpinner) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = distColor
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
```

- [ ] **Step 2: Add the metres caption under the reason input**

In the `if (reasonRequired)` branch, replace:

```kotlin
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(Modifier.height(24.dp))
            } else {
```

with:

```kotlin
                    minLines = 3,
                    maxLines = 5
                )

                // Live distance attached to the out-of-range reason field (updates as GPS sharpens).
                if (outOfRangeReasonRequired) {
                    val fieldDistLabel: String? = when (val gs = geoStatus) {
                        is GeoStatus.OutOfRange ->
                            if (gs.metres <= 1) "Right at the edge of the area" else "${gs.metres} m away"
                        is GeoStatus.InRange -> "You're at the location ✓"
                        else -> null
                    }
                    if (fieldDistLabel != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = fieldDistLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
```

- [ ] **Step 3: Verify it compiles**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --max-workers=1`
Expected: PASS (compiles). `CircularProgressIndicator`, `Text`, `Spacer`, `GeoStatus` are already imported in this file (`androidx.compose.material3.*`, layout `*`, and the `GeoStatus` import).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/feature/home/dialogs/ReasonBottomSheet.kt
git commit -m "feat(home): out-of-range sheet — live spinner + metres-away caption"
```

---

### Task 6: Full build verification

**Files:** none

- [ ] **Step 1: Build the debug APK**

Run: `.\gradlew.bat :app:assembleDebug --offline --console=plain --max-workers=1`
Expected: `BUILD SUCCESSFUL`; APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Manual on-device checklist (logcat)**

Install the APK, then verify:
- Punch out from a weak-GPS spot, sit on the reason/work-report sheet a while, then finish → `LiveLocationRefiner` log shows `finishing with best acc=…` and the committed accuracy is the **best seen during the wait**, not the early fix.
- Out-of-range sheet shows the small spinner next to "You're about X m…"; it stops when you walk in (label flips to "✓").
- Reason field shows "X m away" (or "Right at the edge of the area" when 0–1 m).
- Disable GPS mid-flow then punch with no gathered fix → log shows `falling back to one-shot capture` and the punch still completes (never blocked).
- Check-in still starts tracking; check-out still stops it.

---

## Self-Review

**Spec coverage:**
- Goal 1 (keep refining commit location) → Tasks 1, 3, 4. ✅
- Goal 2 (never block; fallback) → `finish()` fallback in Task 3 + `?:` fallback in Task 4. ✅
- Goal 3 (metres at reason field) → Task 5 Step 2. ✅
- Goal 4 (spinner) → Task 5 Step 1. ✅
- Non-goals (server authority, `t_token`, params, `MAX_ACCURACY_METERS`, tracking service, `captureLiveLocation` retains retry logic + stays as fallback) → unchanged; Task 2 is a pure extraction. ✅
- Both check-in + check-out → Task 4 touches both commit sites and the shared prefetch. ✅
- Unit tests for best-fix rule → Task 1. ✅

**Placeholder scan:** No TBD/TODO; every code step shows full code. ✅

**Type consistency:** `BestFixPolicy.shouldReplace(heldAccuracyM: Float?, heldAgeMs: Long, newAccuracyM: Float)` defined in Task 1, called identically in Task 3. `LiveLocationHelper.buildResult(location: Location): LiveLocationResult` defined in Task 2, called in Task 3. `LiveLocationRefiner.start()/stop()/finish()` defined in Task 3, called in Task 4. `GeoStatus.OutOfRange.metres` used in Task 5 matches existing usage. ✅
