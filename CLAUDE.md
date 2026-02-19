also # Teamozy - Android HRMS App

## Project Overview
Teamozy is an Android HRMS (Human Resources Management System) app for attendance tracking, leave management, face recognition check-in/out, real-time location tracking, and employee profile management.

- **Package:** `com.hrms.jeejateamozy`
- **Min SDK:** 24 | **Target SDK:** 36 | **Compile SDK:** 36
- **Version:** 1.2.1 (versionCode 21)
- **Language:** Kotlin | **UI:** Jetpack Compose + Material 3

## Architecture

**Clean Architecture + MVVM** with single-activity design.

```
app/src/main/java/com/hrms/jeejateamozy/
├── app/                  # Entry point (MainActivity)
├── core/                 # Cross-cutting: network, utils, FCM, state
├── feature/              # Feature modules (each has data/domain/presentation)
├── navigation/           # AppScreen enum, DeepLink sealed class
└── di/                   # Koin DI modules
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| DI | Koin 4.1.1 |
| Network | Retrofit 2.11.0 + OkHttp 4.12.0 + Gson |
| Local DB | Room 2.8.4 (location sync queue) |
| Background | WorkManager, Foreground Service, AlarmManager |
| Auth | JWT token via SharedPreferences |
| Camera | CameraX 1.3.4-1.4.0 |
| Face ML | ML Kit Face Detection + TFLite (512-d embeddings) |
| Location | Play Services Location 21.3.0 |
| Push | Firebase Cloud Messaging |
| Analytics | Firebase Analytics + Crashlytics |
| Images | Coil 2.5.0 |
| Build | Gradle KTS + Version Catalog (libs.versions.toml) |

## Navigation

State-based navigation (NOT Compose Navigation library). `MainActivity` holds an `AppScreen` enum state:
- `SPLASH` -> `LOGIN` -> `HOME` (main flow)
- `MainScreen` has 3-tab `BottomNavigation`: Home, Attendance, Profile
- Child screens are shown via mutable state toggles within each tab
- Deep links from FCM parsed via `DeepLink` sealed class in `navigation/DeepLink.kt`

## Feature Modules

| Feature | Path | Key Files |
|---------|------|-----------|
| **Auth** | `feature/auth/` | `AuthRepository`, `LoginUseCase`, `LoginScreen`, `ForgotPasswordScreen` |
| **Home** | `feature/home/` | `HomePage`, `AttendanceStatusCard`, `QuickAccessSection` |
| **Attendance** | `feature/attendance/` | `AttendanceRepository`, `AttendanceViewModel`, `AttendanceHistoryScreen` |
| **Leave** | `feature/leave/` | `LeaveRepository`, `LeaveViewModel`, `ApplyLeaveScreen`, `LeaveHistoryScreen` |
| **Profile** | `feature/profile/` | `ProfileRepository`, `ProfileScreen`, Edit/View sub-screens |
| **Face** | `feature/face/` | `FaceCaptureScreen`, `FaceVerifier`, `EmbeddingExtractor` |
| **Location** | `feature/location/` | `LocationTrackingService`, `LocationRepository`, `LocationDatabase` |
| **Circular** | `feature/circular/` | `CircularRepository`, `CircularViewModel`, `CircularListScreen` |
| **Work Report** | `feature/workreport/` | `WorkReportRepository`, `WorkReportViewModel`, `WorkReportScreen` |
| **Notification** | `feature/notification/` | `NotificationRepository`, `NotificationViewModel`, `NotificationListScreen` |
| **Splash** | `feature/splash/` | `SplashScreen` (token verification) |
| **Permissions** | `feature/permissions/` | `PermissionDialog`, `PermissionChecker` |
| **Main** | `feature/main/` | `MainScreen` (scaffold with bottom nav) |

## API Configuration

- **Base URL:** `https://teamozy.com/data/jeejafashion/m/`
- **Auth:** Bearer token via `Authorization` header (added by OkHttp interceptor)
- **Device ID:** `X-Device-Id` header on all requests
- **All endpoints defined in:** `core/network/ApiService.kt`
- **All models defined in:** `core/network/ApiModels.kt`
- **Network setup:** `core/network/NetworkModule.kt`
- **Error parsing:** `core/utils/NetworkErrorHandler.kt`
- **401 handling:** Interceptor emits `AppEvent.Unauthorized` via `AppStateManager`

## DI (Koin)

All modules defined in `di/KoinModules.kt`. Initialized in `MainActivity.onCreate()`.

Modules: `authModule`, `attendanceModule`, `permissionsModule`, `homeModule`, `circularModule`, `leaveModule`, `attendanceHistoryModule`, `locationModule`, `notificationModule`

Usage in Compose:
```kotlin
val vm: SomeViewModel = koinViewModel()
val repo: SomeRepository = koinInject()
```

## Key Patterns

### Repository Pattern
Each feature has a Repository in `data/` that calls `ApiService` and returns sealed class outcomes:
```kotlin
sealed class AuthOutcome {
    data class Success(val message: String) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class DeviceNotRegistered(val message: String) : AuthOutcome()
    data class UpdateRequired(val message: String) : AuthOutcome()
}
```

### ViewModel Pattern
```kotlin
class XxxViewModel(repo: XxxRepository) : ViewModel() {
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
}
```

### Screen Pattern
- Screens are `@Composable fun` (not classes)
- State managed with `remember` / `rememberSaveable`
- Loading/error states inline
- Dialogs and bottom sheets as separate composables within the same file

### Auth Flow
1. `POST send-login` → sends OTP
2. `POST verify-login` → returns JWT token + user data (saved to PreferencesManager)
3. `GET verify-token` → validates token on app startup
4. Password login also via `verify-login` with password param

### Forgot Password Flow
1. `POST forgot-password` → sends reset OTP (param: `number`)
2. `POST verify-reset-otp` → returns `reset_token` (params: `number`, `otp`)
3. `POST reset-password` → resets password (params: `reset_token`, `new_password`, `confirm_password`)

### Location Tracking
- Starts after check-in signature → foreground service
- Captures GPS every 30-60s
- Syncs batch every 5min or 50+ locations
- Room DB for offline buffering
- Survives reboot (BootReceiver), OEM kill (AlarmManager)
- Stops on check-out or 403 from server

## Design Language

- **Primary color:** `#6366F1` (Indigo)
- **Secondary accent:** `#8B5CF6` (Purple)
- **Success:** `#10B981` (Green)
- **Error:** `#EF4444` (Red)
- **Warning:** `#F59E0B` (Amber)
- **Background:** `#F6F7FB` (Light gray)
- **Card:** White with `RoundedCornerShape(20.dp)`, elevation 2dp
- **Buttons:** `RoundedCornerShape(12.dp)`, height 48-52dp
- **Text fields:** `OutlinedTextField` with `RoundedCornerShape(12.dp)`
- **OTP input:** Custom `OtpBoxes` composable (56x56dp boxes)

## Important Files Quick Reference

| Purpose | File |
|---------|------|
| App entry | `app/MainActivity.kt` |
| API endpoints | `core/network/ApiService.kt` |
| API models | `core/network/ApiModels.kt` |
| Network setup | `core/network/NetworkModule.kt` |
| Preferences | `core/utils/PreferencesManager.kt` |
| Error handler | `core/utils/NetworkErrorHandler.kt` |
| App state | `core/state/AppStateManager.kt` |
| FCM service | `core/fcm/TeamozyFirebaseMessagingService.kt` |
| DI modules | `di/KoinModules.kt` |
| Screen enum | `navigation/AppScreen.kt` |
| Deep links | `navigation/DeepLink.kt` |
| Main scaffold | `feature/main/presentation/MainScreen.kt` |
| Login | `feature/auth/presentation/LoginScreen.kt` |
| Home | `feature/home/presentation/HomePage.kt` |

## Build Commands

Build requires JAVA_HOME set (use Android Studio for building):
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build (minified)
```

## API Response Patterns

Most API responses follow one of two patterns:

**Auth endpoints** use `success` boolean:
```json
{ "success": true, "message": "...", "data": { ... } }
```

**Other endpoints** use `status` string:
```json
{ "status": "success", "message": "...", "data": { ... } }
```

### Work Report API

**GET /work-report** (params: `month`, `year`):
```json
{ "success": true, "message": "...", "data": [ { ...report_dto... } ], "total": 5 }
```

**POST /work-report** (form: `work_description`, optional `attachments` files):
```json
{
  "success": true, "message": "...",
  "data": { "id": 1, "report_date": "...", "work_description": "...",
            "attachments_count": 2, "report_status": "SUBMITTED",
            "submitted_at": "...", "reports_today": 1, "remaining_reports_today": 3 }
}
```

## Conventions

- Feature folders follow: `feature/{name}/data/`, `feature/{name}/domain/usecase/`, `feature/{name}/presentation/`
- API response DTOs use snake_case fields, domain models use camelCase
- DTOs have `.toDomain()` extension functions
- Repositories handle `withContext(Dispatchers.IO)` and error catching
- Screens use `koinInject()` / `koinViewModel()` directly (no constructor injection)
- No Compose Navigation — screens toggled via mutable state
- Commit messages are brief, sometimes just `--`
