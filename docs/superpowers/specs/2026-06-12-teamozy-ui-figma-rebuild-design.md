# Teamozy UI Rebuild — Match Figma Wireframes

**Date:** 2026-06-12
**Status:** Awaiting user review
**Figma:** [Teamozy-Wireframes](https://www.figma.com/design/Jm0Eljn4NCnkxUMdKnwdt2/Teamozy-Wireframes) — file key `Jm0Eljn4NCnkxUMdKnwdt2`

## Goal

Rebuild the app's UI layer screen-by-screen so every screen matches the Figma
wireframes **1:1** (layout, spacing, typography, color). The wireframes are
full-fidelity and are the single source of truth. Where the current build
diverges from Figma, Figma wins.

## Non-Goals / What Is Preserved

Only the Compose UI (`presentation/` composables) changes. The following are
**not** touched except where a signature must change to feed new UI state:

- ViewModels, UI-state classes, repositories, use cases, DI (`KoinModules`)
- State-based navigation (`AppScreen`, `MainScreen` toggles, `DeepLink`)
- Business/runtime logic: check-in/out flow, face capture/registration ML,
  location tracking service, FCM, WorkManager/AlarmManager
- API layer (`ApiService`, `ApiModels`, network module)

No new libraries. No Tailwind. Existing patterns (`koinViewModel()`,
`koinInject()`, sealed outcomes, `withContext(Dispatchers.IO)`) stay.

## Design System (extracted from Figma)

A single source-of-truth file `core/designsystem/` will hold these tokens so
every screen references them instead of hardcoding hex values.

### Colors

| Role | Hex |
|---|---|
| Screen background | `#F6F7FB` |
| **App bar (logged-in screens)** | **`#7C3AED`** (violet-600) |
| App bar text/icons on violet | `#FFFFFF`, secondary `#DDD6FE` |
| Primary action / active / link | `#6366F1` (indigo-500) |
| Brand gradient (logo, splash) | `#6366F1` → `#8B5CF6` |
| Success / timer / present | `#10B981` |
| Warning / pending | `#F59E0B` |
| Error / absent / rejected | `#EF4444` |
| Heading text | `#0F172A` / `#111827` |
| Label text | `#334155` |
| Secondary text | `#6B7280` / `#64748B` |
| Tertiary / placeholder | `#9CA3AF` / `#94A1B8` |
| Card label text | `#374151` |
| Border | `#E5E7EB` / `#E2E8F0` |
| Field background | `#F8FAFC` |
| Quick-access icon tiles | rose `#FFE4E6`, teal `#CCFBF1`, amber `#FEF3C7` |
| Auth header gradient | `#EEF2FF` → `#F8FAFF` |

Dark-on-tint container text uses the Tailwind 600/700 ramp already adopted
(`#DC2626`, `#D97706`, `#059669`, `#4338CA`).

### Typography (Inter)

| Style | Size / weight / color |
|---|---|
| Screen title (auth) | 26 SemiBold `#0F172A` |
| Greeting / section heading | 18 Bold `#111827` |
| Card / section header | 16 Bold `#111827` |
| Top-bar name | 15 SemiBold white |
| Body | 14 Regular/Medium |
| Field label | 13 SemiBold `#334155` |
| Caption / meta | 12–13 Regular `#6B7280` / `#9CA3AF` |
| Top-bar company / micro | 11 Regular `#DDD6FE` |
| Timer | 30 Bold `#10B981` |

If Inter is not already bundled, fall back to the platform default (current
behavior) — bundling Inter is an optional follow-up, not part of this rebuild.

### Shape, spacing, elevation

- Screen padding 16dp; inter-item gap 12dp; card inner padding 20dp
- Card radius: 24dp (hero/check-in), 20dp (standard), 16dp (compact/quick-access)
- Card fill white, shadow `0 2px 8px rgba(0,0,0,0.08)` (≈2–4dp elevation)
- Buttons radius 12dp, height 48dp; fields radius 12dp; OTP box 56dp radius 12dp

## Shared Components (built first, in `core/designsystem/`)

Built/refactored once, reused everywhere:

1. `TeamozyTheme` color + type tokens (object or Material theme extension)
2. `HomeAppBar` — violet, avatar + name/company + refresh + bell-with-badge
3. `TeamozyTopBar` — back + title; variants: violet-filled and white-surface
   (each screen uses whichever its wireframe shows)
4. `TeamozyCard` — white, configurable radius, standard shadow
5. `PrimaryButton` / `SecondaryButton` (outlined) — 48dp, radius 12dp
6. `TeamozyTextField` — field bg `#F8FAFC`, border `#E5E7EB`, radius 12dp
7. `OtpBoxes` — 56dp boxes (already matches; promote to shared)
8. `SegmentedToggle` — track `#F1F5FA`, active white pill w/ shadow
9. `BottomNavBar` — white, 60dp, 1px top border `#E5E7EB`, active `#6366F1`
10. `StatusBadge` / `Chip` — success/pending/error/neutral tint pairs
11. `QuickAccessCard` — white radius 16, 48dp tinted icon tile radius 12
12. `SectionHeader`, `EmptyState`, `ScreenScaffold`

## Screens & Build Order

Order = daily-use first. Each screen is fetched fresh from Figma via
`get_design_context` at implementation time for exact pixel values.

**Phase 0 — Foundation:** design-system tokens + shared components above.

**Phase 1 — Daily use**
- `home/home` `22:7`, `home/home-checkout` `22:59`, `home/home-complete` `22:113`
- `attendance/history` `25:2`, `attendance/day-detail` `25:154`
- `profile/main` `28:2`
- `leave/history` `27:2`, `leave/apply` `27:74`, `leave/detail` `27:118`

**Phase 2 — Profile suite**
- `profile/personal-info` `28:97`, `profile/employment` `28:154`,
  `profile/edit-contact` `28:219`, `profile/banking-info` `47:2`,
  `profile/employment-identity` `47:37`, `profile/shift-details` `47:50`,
  `profile/edit-social` `52:2`

**Phase 3 — Secondary features**
- `circular/list` `31:53`, `circular/detail` `31:116`
- `workreport/add` `31:138`, `workreport/history` `31:169`
- `notification/list` `31:216`

**Phase 4 — Auth, splash, system**
- `splash/splash` `22:2`
- `auth/login` `18:2` (already ~matches — verify/polish),
  `auth/forgot-password/1-3` `20:2` / `20:32` / `20:65`
- `face/capture` `31:4`, `face/registration` `31:16`
- `permissions/dialog` `31:282`

## Per-Screen Workflow

1. `get_design_context` for the screen's node → exact layout/colors/spacing.
2. Map Figma frames to existing composable; rebuild markup using shared
   components + tokens, wiring the **existing** ViewModel state/callbacks.
3. Compile (`:app:compileDebugKotlin`).
4. Visual verify on device via ADB when the device is available (screenshot
   vs Figma); otherwise rely on code review + compile. Image-heavy turns may
   hit the chat image cap — fall back to UI-dump text checks.

## Verification

- Each phase ends with a clean `:app:compileDebugKotlin`.
- Spot-check key screens on the running app (ADB) against the Figma frame.
- Final pass: design-token sweep (no stray off-token hex outside
  `core/designsystem/` except intentional face/social-brand colors).

## Risks / Notes

- **Violet vs indigo:** app bar is violet `#7C3AED`; actions/active stay indigo
  `#6366F1`. Two-tone is intentional per Figma.
- Inter font not bundled — acceptable to ship with system font; visual weight
  differs slightly from Figma. Bundling is an optional follow-up.
- Large surface area (~28 screens). Delivered in phases; each phase is
  independently compilable and shippable.
