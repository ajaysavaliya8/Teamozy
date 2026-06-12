# Teamozy UI Figma Rebuild — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild every app screen's Compose UI to match the Teamozy Figma wireframes 1:1, on a shared design-system foundation, keeping all existing logic/navigation intact.

**Architecture:** A new `core/designsystem/` package holds color/type/shape tokens and ~12 reusable composables extracted from the wireframes. Each feature screen is then rebuilt to reference those tokens/components and match its Figma frame, wiring the **existing** ViewModel state and callbacks unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Koin. No new dependencies.

**Verification model (UI work):** Each task ends by (a) `:app:compileDebugKotlin` clean, and (b) visual match against the named Figma node — ADB screenshot vs Figma when a device is connected, else code review. There are no unit tests for visual layout.

**Spec:** `docs/superpowers/specs/2026-06-12-teamozy-ui-figma-rebuild-design.md`

**Figma file key:** `Jm0Eljn4NCnkxUMdKnwdt2`

**Build command (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "C:\Users\ajays\Documents\Teamozy\gradlew.bat" -p "C:\Users\ajays\Documents\Teamozy" :app:compileDebugKotlin --offline --console=plain --max-workers=1
```

**ADB path:** `C:\Users\ajays\AppData\Local\Android\Sdk\platform-tools\adb.exe` — device `bd6cd484` (1080×2340). Downscale screenshots to ≤1080px tall before reading; if the chat image cap is hit, verify via `uiautomator dump` text.

---

## Setup Task: Branch

- [ ] **Step 1: Create the rebuild branch**

```powershell
git -C "C:\Users\ajays\Documents\Teamozy" checkout -b ui-figma-rebuild
```

- [ ] **Step 2: Confirm branch**

Run: `git -C "C:\Users\ajays\Documents\Teamozy" branch --show-current`
Expected: `ui-figma-rebuild`

---

## PHASE 0 — Design-System Foundation

Package: `app/src/main/java/com/hrms/jeejateamozy/core/designsystem/`

### Task 0.1: Color tokens

**Files:**
- Create: `core/designsystem/TeamozyColors.kt`

- [ ] **Step 1: Create the color token object**

```kotlin
package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.ui.graphics.Color

/** Single source of truth for Teamozy colors, extracted from the Figma wireframes. */
object TeamozyColors {
    val Background = Color(0xFFF6F7FB)

    // App bar (logged-in screens) is violet; actions/active states are indigo.
    val AppBar = Color(0xFF7C3AED)
    val OnAppBar = Color(0xFFFFFFFF)
    val OnAppBarSecondary = Color(0xFFDDD6FE)

    val Primary = Color(0xFF6366F1)       // indigo-500 — actions, active, links
    val PrimaryGradientStart = Color(0xFF6366F1)
    val PrimaryGradientEnd = Color(0xFF8B5CF6)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)

    // Dark-on-tint container text (Tailwind 600/700 ramp)
    val SuccessDark = Color(0xFF059669)
    val WarningDark = Color(0xFFD97706)
    val ErrorDark = Color(0xFFDC2626)
    val PrimaryDark = Color(0xFF4338CA)

    val Heading = Color(0xFF111827)
    val HeadingStrong = Color(0xFF0F172A)
    val Label = Color(0xFF334155)
    val CardLabel = Color(0xFF374151)
    val Secondary = Color(0xFF6B7280)
    val SecondaryAlt = Color(0xFF64748B)
    val Tertiary = Color(0xFF9CA3AF)
    val Placeholder = Color(0xFF94A1B8)

    val Border = Color(0xFFE5E7EB)
    val BorderAlt = Color(0xFFE2E8F0)
    val FieldBg = Color(0xFFF8FAFC)
    val TrackBg = Color(0xFFF1F5FA)

    // Quick-access icon tiles
    val TileRose = Color(0xFFFFE4E6)
    val TileTeal = Color(0xFFCCFBF1)
    val TileAmber = Color(0xFFFEF3C7)

    // Auth header gradient
    val AuthHeaderStart = Color(0xFFEEF2FF)
    val AuthHeaderEnd = Color(0xFFF8FAFF)
}
```

- [ ] **Step 2: Compile**

Run the build command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/core/designsystem/TeamozyColors.kt
git commit -m "feat(ui): add Teamozy color tokens from Figma"
```

### Task 0.2: Shape & dimension tokens

**Files:**
- Create: `core/designsystem/TeamozyShapes.kt`

- [ ] **Step 1: Create shape/dimension tokens**

```kotlin
package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object TeamozyShapes {
    val CardHero = RoundedCornerShape(24.dp)
    val Card = RoundedCornerShape(20.dp)
    val CardCompact = RoundedCornerShape(16.dp)
    val Control = RoundedCornerShape(12.dp)   // buttons, fields, OTP boxes
    val Pill = RoundedCornerShape(10.dp)
}

object TeamozyDimens {
    val ScreenPadding = 16.dp
    val ItemGap = 12.dp
    val CardPadding = 20.dp
    val ButtonHeight = 48.dp
    val FieldHeight = 48.dp
    val OtpBox = 56.dp
    val BottomNavHeight = 60.dp
}
```

- [ ] **Step 2: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/core/designsystem/TeamozyShapes.kt
git commit -m "feat(ui): add Teamozy shape/dimension tokens"
```

### Task 0.3: Typography tokens

**Files:**
- Create: `core/designsystem/TeamozyType.kt`

- [ ] **Step 1: Create text-style tokens** (system font; sizes/weights/colors from Figma)

```kotlin
package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Inter-equivalent scale. Uses the platform default font family until Inter is bundled. */
object TeamozyType {
    val AuthTitle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.HeadingStrong)
    val Greeting = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Heading)
    val SectionHeader = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Heading)
    val AppBarName = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.OnAppBar)
    val AppBarCompany = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.OnAppBarSecondary)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val BodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val FieldLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.Label)
    val Caption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.Secondary)
    val Meta = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = TeamozyColors.Tertiary)
    val Timer = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TeamozyColors.Success)
}
```

- [ ] **Step 2: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/core/designsystem/TeamozyType.kt
git commit -m "feat(ui): add Teamozy typography tokens"
```

### Task 0.4: Core components — TeamozyCard, PrimaryButton, SecondaryButton

**Files:**
- Create: `core/designsystem/TeamozyButtons.kt`
- Create: `core/designsystem/TeamozyCard.kt`

- [ ] **Step 1: TeamozyCard**

```kotlin
package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TeamozyCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = TeamozyShapes.Card,
    containerColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) { Box(Modifier) { content() } }
}
```

- [ ] **Step 2: Buttons**

```kotlin
package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(TeamozyDimens.ButtonHeight),
        shape = TeamozyShapes.Control,
        colors = ButtonDefaults.buttonColors(containerColor = TeamozyColors.Primary)
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(TeamozyDimens.ButtonHeight),
        shape = TeamozyShapes.Control,
        border = BorderStroke(1.dp, TeamozyColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TeamozyColors.HeadingStrong) }
}
```
(Add `import androidx.compose.ui.unit.dp` to the buttons file.)

- [ ] **Step 3: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/hrms/jeejateamozy/core/designsystem/TeamozyCard.kt app/src/main/java/com/hrms/jeejateamozy/core/designsystem/TeamozyButtons.kt
git commit -m "feat(ui): add TeamozyCard and button components"
```

### Task 0.5: TeamozyTextField

**Files:**
- Create: `core/designsystem/TeamozyTextField.kt`

- [ ] **Step 1: Create the field** — bg `FieldBg`, border `Border`, radius 12, label uses `TeamozyType.FieldLabel`. Build with `OutlinedTextField` + `OutlinedTextFieldDefaults.colors(unfocusedContainerColor = FieldBg, focusedContainerColor = FieldBg, unfocusedBorderColor = Border, focusedBorderColor = Primary)` and `shape = TeamozyShapes.Control`. Expose `value`, `onValueChange`, `label`, `placeholder`, `leadingIcon`, `keyboardOptions`, `visualTransformation`, `isError`, `modifier`.
- [ ] **Step 2: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit** `feat(ui): add TeamozyTextField`.

### Task 0.6: BottomNavBar

**Files:**
- Create: `core/designsystem/BottomNavBar.kt`
- Reference Figma: home bottom nav `69:36` (white, 60dp, 1px top border `Border`, 3 tabs, active `Primary`, inactive `Tertiary`).

- [ ] **Step 1:** Build a `Row` (height 60dp, white, top border via `drawBehind` or a 1px `HorizontalDivider` above) with 3 equal-weight items (icon + 11sp label), `selected` index param, `onSelect: (Int) -> Unit`. Active tint `Primary`, inactive `Tertiary`.
- [ ] **Step 2: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit** `feat(ui): add BottomNavBar component`.

### Task 0.7: HomeAppBar + TeamozyTopBar

**Files:**
- Create: `core/designsystem/TeamozyAppBars.kt`
- Reference Figma: home header `69:2` (violet `AppBar`, avatar 44dp, name/company, refresh, bell+badge).

- [ ] **Step 1: HomeAppBar** — violet surface, `statusBarsPadding()`, avatar (async image) + name (`AppBarName`) + company (`AppBarCompany`) + refresh icon + bell with red badge. Params mirror the existing `HomeTopBar` signature so wiring is drop-in.
- [ ] **Step 2: TeamozyTopBar** — Material3 `TopAppBar`, params: `title`, `onBack`, optional `actions`, and `filled: Boolean` (true → violet `AppBar`/white text; false → white `surface`/`Heading` text). Each screen chooses per its wireframe.
- [ ] **Step 3: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit** `feat(ui): add HomeAppBar and TeamozyTopBar`.

### Task 0.8: SegmentedToggle, StatusBadge, QuickAccessCard, SectionHeader, EmptyState

**Files:**
- Create: `core/designsystem/TeamozyControls.kt`
- Reference Figma: segmented toggle `18:28`; quick-access card `72:2`.

- [ ] **Step 1: SegmentedToggle** — track `TrackBg` radius 12 padding 4; N options; selected = white pill radius 10 with shadow + `Primary` text; unselected = `SecondaryAlt` text. Params `options: List<String>`, `selected: Int`, `onSelect`.
- [ ] **Step 2: StatusBadge** — `status: String` → (bg tint, text color, icon) using token pairs: present/approved/completed → Success; pending/in-progress → Warning (in-progress may use `PrimaryGradientEnd`); rejected/absent → Error; withdrawn/cancelled → `SecondaryAlt`. Rounded pill, icon + label.
- [ ] **Step 3: QuickAccessCard** — white card radius 16 (`CardCompact`), 48dp icon tile radius 12 with tint param, icon, label (`CardLabel` 12sp). Params `icon`, `label`, `tileColor`, `iconTint`, `onClick`.
- [ ] **Step 4: SectionHeader** (`TeamozyType.SectionHeader`) and **EmptyState** (centered icon + title + subtitle).
- [ ] **Step 5: Compile.** Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit** `feat(ui): add segmented toggle, badge, quick-access, section header, empty state`.

---

## Per-Screen Task Template (Phases 1–4)

Every screen task below follows this exact workflow. Do them one screen at a time.

- [ ] **Step 1: Fetch the Figma frame** — call `get_design_context` with `fileKey=Jm0Eljn4NCnkxUMdKnwdt2` and the screen's `nodeId`. Record exact positions, sizes, colors, text, and the asset URLs.
- [ ] **Step 2: Read the current screen file and its ViewModel/UI-state** — identify every piece of state and every callback the UI consumes/emits. These must be preserved verbatim.
- [ ] **Step 3: Rebuild the composable** to match the Figma frame using `core/designsystem` tokens + shared components. Replace hardcoded colors/shapes with tokens. Keep all `koinViewModel()`/`koinInject()` calls, state collection, and callback wiring unchanged. Do not alter the ViewModel unless a new callback is genuinely required (note it if so).
- [ ] **Step 4: Compile** — run the build command. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5: Visual verify** — if device connected: ADB screenshot the screen, downscale, compare to the Figma frame; fix gaps. Else: code-review against the recorded Figma values.
- [ ] **Step 6: Commit** — `feat(ui): rebuild <screen> to match Figma`.

---

## PHASE 1 — Daily Use

- [ ] **Task 1.1: home/home** — node `22:7` — `feature/home/presentation/HomePage.kt` + components (`HomeTopBar`→use `HomeAppBar`, `AttendanceStatusCard`, `QuickAccessSection`→use `QuickAccessCard`, `GreetingSection`). Wire `HomeViewModel` state.
- [ ] **Task 1.2: home/home-checkout** — node `22:59` — same files; the checkout/working state of `AttendanceStatusCard`.
- [ ] **Task 1.3: home/home-complete** — node `22:113` — same files; the completed state.
- [ ] **Task 1.4: attendance/history** — node `25:2` — `feature/attendance/presentation/AttendanceHistoryScreen.kt`. Calendar grid, legend, `MonthSummaryCard`. Wire `AttendanceHistoryViewModel`.
- [ ] **Task 1.5: attendance/day-detail** — node `25:154` — `AttendanceDayDetailScreen.kt`. Day header, Week-Off/status row, Shift Info, Hours Summary, Submit Correction button.
- [ ] **Task 1.6: profile/main** — node `28:2` — `feature/profile/presentation/ProfileScreen.kt` + `components/`. Header card, profile completion, contact row, social icons, Quick Access grid.
- [ ] **Task 1.7: leave/history** — node `27:2` — `feature/leave/presentation/LeaveHistoryScreen.kt`. Year/summary bar, filter chips, leave cards, pagination, empty state.
- [ ] **Task 1.8: leave/apply** — node `27:74` (height 900) — `ApplyLeaveScreen.kt`. Leave-type dropdown, date range, priority selector, reason, attachments, submit.
- [ ] **Task 1.9: leave/detail** — node `27:118` — `LeaveDetailScreen.kt`. Header, status/paid badges, From/To/Duration, reason, timeline, approvers/rejection/pending cards, withdraw button.

---

## PHASE 2 — Profile Suite

- [ ] **Task 2.1: profile/personal-info** — node `28:97` — `Viewpersonalinfoscreen.kt`.
- [ ] **Task 2.2: profile/employment** — node `28:154` — `ViewEmploymentDetailScreen.kt`.
- [ ] **Task 2.3: profile/edit-contact** — node `28:219` — `EditContactDetailScreen.kt`.
- [ ] **Task 2.4: profile/banking-info** — node `47:2` — `Editbankinginfoscreen.kt` (and any view variant).
- [ ] **Task 2.5: profile/employment-identity** — node `47:37` — `Viewemploymentidentityscreen.kt`.
- [ ] **Task 2.6: profile/shift-details** — node `47:50` — `Viewshiftdetailsscreen.kt`.
- [ ] **Task 2.7: profile/edit-social** — node `52:2` — `EditSocialMediaScreen.kt`.

---

## PHASE 3 — Secondary Features

- [ ] **Task 3.1: circular/list** — node `31:53` — `feature/circular/presentation/Circularlistscreen.kt`.
- [ ] **Task 3.2: circular/detail** — node `31:116` — `Circulardetailscreen.kt`.
- [ ] **Task 3.3: workreport/add** — node `31:138` — `feature/workreport/presentation/WorkReportScreen.kt` + `AddNewWorkReportTab.kt`.
- [ ] **Task 3.4: workreport/history** — node `31:169` — `WorkReportHistoryTab.kt` + `WorkReportDetailBottomSheet.kt`.
- [ ] **Task 3.5: notification/list** — node `31:216` — `feature/notification/presentation/NotificationListScreen.kt`.

---

## PHASE 4 — Auth, Splash, System

- [ ] **Task 4.1: splash/splash** — node `22:2` — `feature/splash/presentation/SplashScreen.kt`.
- [ ] **Task 4.2: auth/login** — node `18:2` — `feature/auth/presentation/LoginScreen.kt` (already ~matches; refactor to tokens/components, verify).
- [ ] **Task 4.3: auth/forgot-password 1/2/3** — nodes `20:2` / `20:32` / `20:65` — `ForgotPasswordScreen.kt`.
- [ ] **Task 4.4: face/capture** — node `31:4` — `feature/face/presentation/FaceCaptureScreen.kt` (preserve camera/ML; restyle overlays/chrome only).
- [ ] **Task 4.5: face/registration** — node `31:16` — `FaceRegistrationScreen.kt`.
- [ ] **Task 4.6: permissions/dialog** — node `31:282` — `feature/permissions/.../PermissionDialog.kt`.

---

## Final Task: Full verification

- [ ] **Step 1:** Full `:app:compileDebugKotlin` clean.
- [ ] **Step 2:** Token sweep — no off-token hex outside `core/designsystem/` except intentional face/social-brand colors.
- [ ] **Step 3:** ADB spot-check Home, Attendance, Profile, Leave Detail against their Figma frames.
- [ ] **Step 4:** Commit any final fixes; summarize the diff.

---

## Self-Review Notes

- **Spec coverage:** every spec screen maps to a task (Phases 1–4 cover all 28 frames; Phase 0 covers the design-system + shared components section).
- **Type consistency:** token names (`TeamozyColors.*`, `TeamozyShapes.*`, `TeamozyType.*`, `TeamozyDimens.*`) and component names (`TeamozyCard`, `PrimaryButton`, `SecondaryButton`, `TeamozyTextField`, `BottomNavBar`, `HomeAppBar`, `TeamozyTopBar`, `SegmentedToggle`, `StatusBadge`, `QuickAccessCard`, `SectionHeader`, `EmptyState`) are used consistently across Phase 0 and the per-screen tasks.
- **Verification adaptation:** UI work uses compile + visual-match instead of unit tests; stated in the header and per-screen template.
