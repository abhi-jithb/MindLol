# MindLol System Architecture

## Overview
MindLol is a privacy-first digital wellbeing application that counts short-form video consumption (Instagram Reels, YouTube Shorts) in real time on mobile devices, displays a live in-app floating HUD, and enforces non-autonomous blocking overlays when custom daily limits are reached.

---

## High-Level System Architecture

```
                                  MINDLOL APPLICATION
                                           │
          ┌────────────────────────────────┴────────────────────────────────┐
          ▼                                                                 ▼
┌───────────────────────────────────┐                     ┌───────────────────────────────────┐
│     FLUTTER PRODUCT LAYER (UI)    │                     │     NATIVE ANDROID ENGINE (CORE)  │
│ - Design System & Themes          │                     │ - AccessibilityService Lifecycle  │
│ - HomeScreen & Blob Mascot Motion │ ◄─ MethodChannel ───│ - Structural Reel Detector        │
│ - Centralized BlobCopy Microcopy  │ ◄─ EventChannel ────│ - Direction-Aware Counter         │
│ - Progress & Limit Configuration  │                     │ - Persistent Daily State          │
│ - Reduced Motion Accessibility    │                     │ - StatusBroadcaster Real-Time Sync│
│                                   │                     │ - FloatingCountHudManager (HUD)   │
│                                   │                     │ - WindowManager Blocking Overlay  │
└───────────────────────────────────┘                     └───────────────────────────────────┘
```

---

## 1. Native Android Engine & Real-Time Sync (Phase 11)

### Architecture Pipeline
```
[AccessibilityEvent]
        │
        ▼
[InstagramReelDetector] ──► Pure, read-only structural analysis: isReel, bounds
        │
        ▼
 [ReelObservation] ──────► Normalized event container
        │
        ▼
[InstagramReelCounter] ──► Stateful direction-aware transition counter (forward = +1)
        │
        ▼
[ReelCounterRepository] ──► Single source of truth for todayCount, dailyLimit, persistent storage
        │
        ├─────────────────────────┬─────────────────────────┐
        ▼                         ▼                         ▼
[StatusBroadcaster]    [FloatingCountHudManager]  [BlockOverlayManager]
(Emits to EventChannel) (Shows HUD on Reels)       (Shows Blocker on Limit)
```

### Key Native Components
- **Detector (`InstagramReelDetector`)**: Inspects Android `AccessibilityNodeInfo` tree structure (ViewPager ID `clips_viewer_view_pager` and bounds overlap) to verify active Reel viewing. Read-only and privacy-first.
- **Counter (`InstagramReelCounter`)**: Stateful direction-aware counter. Increments strictly on forward index transitions; rewatches and fling bounces are ignored (`count +0`).
- **Repository (`ReelCounterRepositoryImpl`)**: Authoritative single source of truth for `todayCount`, `dailyLimit`, `limitReached`, and date rollover persistence in `KeyValueStore`.
- **Status Broadcaster (`StatusBroadcaster`)**: Thread-safe event hub that dispatches count and status changes to `EventChannel` and `FloatingCountHudManager` in real time.
- **Floating Count HUD (`FloatingCountHudManager`)**: Non-interactive (`FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`) floating badge overlay that appears ONLY on supported short-video surfaces (Instagram Reels, YouTube Shorts) and auto-hides when entering non-supported apps or screens.
- **Overlay Manager (`BlockOverlayManager`)**: Non-autonomous full-screen `TYPE_APPLICATION_OVERLAY` view with interactive `[ TEST UNLOCK ]` dismissal. Touch events are blocked from underlying target apps when daily limit is reached.

---

## 2. Flutter Product Layer & Blob Motion Personality (Phase 12)

### Architectural Separation
- **Android is Authoritative**: Flutter does NOT independently calculate, increment, or persist the count or limit enforcement state. All updates flow through `MindLolPlatform` channels and `StatusBroadcaster`.
- **Flutter handles Presentation & Motion**: Rendering the Blob mascot, daily metrics, human-friendly progress indicators, limit edit bottom sheets, light/dark themes, and error recovery states.

### Centralized Blob Microcopy (`BlobCopy`)
Microcopy is defined in a single centralized dictionary (`BlobCopy.getCopyForStage(stage, count)`). The tone is human, playful, supportive, slightly weird, and never scolds or shames the user:

| Stage | Threshold (Ratio) | Title | Message |
| :--- | :--- | :--- | :--- |
| **CALM** (0 count) | `count == 0` | "we're chilling." | No Reels today. Mind is clear and calm. |
| **CALM** (low) | `ratio < 0.25` | "nice and easy." | Mindful consumption so far. |
| **WOBBLY** | `0.25 <= ratio < 0.75` | "okay... that's a few." | Blob is getting a bit wobbly. |
| **CRACKED** | `0.75 <= ratio < 1.0` | "uh oh." | Blob needs a tiny break soon. |
| **SHATTERED** | `ratio >= 1.0` | "okay, let's breathe." | Blob is officially overwhelmed. Take a rest. |

### Motion Personality Language (`BlobWidget`)
Each Blob state has a distinct motion personality implemented via standard Flutter `AnimationController` and `CustomPaint`:

- **CALM**: Slow breathing & gentle vertical floating (relaxed 4s cycle, smooth organic expansion).
- **WOBBLY**: Fluid wave deformation & subtle side-to-side tilting (+/- 11px control point shifts).
- **CRACKED**: Tired, asymmetric deformation with periodic micro-tremors and visible fracture lines.
- **SHATTERED**: Restrained distressed trembling with orbiting floating shards (+/- 18px control point shifts).

### State Transitions & Accessibility
- **Color Transitions**: `TweenAnimationBuilder<Color>` smoothly animates color changes across stage shifts (350ms ease-out) without pops or animation leaks.
- **Reduced Motion**: Respects `MediaQuery.of(context).disableAnimations`. When reduced motion is enabled, continuous Blob animation is paused while preserving static visual state representation.

---

## 3. Platform Bridge API (`MindLolPlatform`)

| Channel / Method | Arguments | Returns | Description |
| :--- | :--- | :--- | :--- |
| `MethodChannel("mindlol/platform")` | — | — | Main control and status channel |
| `getMindLolStatus` | — | `Map<String, dynamic>` | Fetches current `todayCount`, `dailyLimit`, `limitReached`, accessibility & overlay permissions |
| `setDailyLimit` | `{'limit': int}` | `Map<String, dynamic>` | Updates native daily limit persistence and dispatches `StatusBroadcaster` event |
| `resetTodayCount` | — | `Map<String, dynamic>` | Resets today's Reel count to 0 (debug/testing helper) |
| `openAccessibilitySettings` | — | `bool` | Opens Android Accessibility Settings screen |
| `openOverlaySettings` | — | `bool` | Opens Android System Overlay Settings screen |
| `EventChannel("mindlol/events")` | — | `Stream<Map>` | Real-time status event stream emitted via `StatusBroadcaster` whenever count or limit updates |

---

## 4. Validated Quality Bar & Test Suite

## 5. Usage Control & Settings Foundation (Phase 13)

### Configuration Architecture & Repository Authority
- **Single Source of Truth**: Android `ReelCounterRepository` remains the sole authoritative source of truth for all usage controls (`todayCount`, `dailyLimit`, `instagramEnabled`, `youtubeEnabled`, `blockingMode`). Flutter never maintains independent limit or app toggle state.
- **Daily Reset Semantics**: On every state read or write (`getTodayCount()`, `incrementCount()`, `setDailyLimit()`), `ReelCounterRepositoryImpl` compares stored local calendar date (`YYYY-MM-DD`) with `LocalDate.now()`. If a new day has started:
  - `todayCount` is set to 0.
  - Stored date is updated to today's date.
  - Previous day's count does not spill over.
  - No background midnight timers or battery-draining wakeups are created. Rollover works even if app was closed at midnight, device was rebooted, or accessibility service ran continuously across midnight.
- **Limit Validation & Clamping**: Daily limit configuration accepts values from 1 to 500 (`MAX_DAILY_LIMIT = 500`). Values less than 1 revert to `DEFAULT_DAILY_LIMIT` (20), and values over 500 clamp to 500.

### Supported Apps & Service Integration
- **Configuration Toggles**: Supported apps are constrained to `Instagram` and `YouTube`.
- **Target App Gating**: `DebugProbeService` evaluates `isAppEnabled(activePackage)` on every accessibility event:
  - When Instagram or YouTube is disabled, structural detection, count incrementing, floating count HUD display, and block overlay enforcement for that package are bypassed completely.
  - Re-enabling the toggle instantly resumes detection and enforcement without requiring service restarts.

### Blocking Configuration
- **Blocking Mode**: Enforces `BlockingMode.BLOCK_AT_LIMIT`. When daily limit is reached for an enabled package, full-screen non-autonomous block overlay is displayed.

### Settings UI (`SettingsScreen`)
Minimal Flutter settings screen adhering to MindLol design system (sentence-case, flat cards, no tabs):
- **USAGE**: Displays current daily limit with interactive preset picker bottom sheet (10, 20, 30, 50, Custom).
- **APPS**: Switch toggles for Instagram Reels and YouTube Shorts.
- **BLOCKING**: Displays active Blocking Mode ("Block at daily limit").
- **ABOUT**: Privacy commitment summary and app version details.

---

## 6. Platform Bridge API (`MindLolPlatform`)

| Channel / Method | Arguments | Returns | Description |
| :--- | :--- | :--- | :--- |
| `MethodChannel("mindlol/platform")` | — | — | Main control and status channel |
| `getMindLolStatus` | — | `Map<String, dynamic>` | Fetches `todayCount`, `dailyLimit`, `limitReached`, `instagramEnabled`, `youtubeEnabled`, `blockingMode`, permissions |
| `setDailyLimit` | `{'limit': int}` | `Map<String, dynamic>` | Updates native daily limit persistence and dispatches `StatusBroadcaster` event |
| `setAppConfiguration` | `{'instagramEnabled': bool?, 'youtubeEnabled': bool?}` | `Map<String, dynamic>` | Updates native app toggles and dispatches `StatusBroadcaster` event |
| `getBlockingMode` | — | `String` | Returns active blocking mode (`BLOCK_AT_LIMIT`) |
| `resetTodayCount` | — | `Map<String, dynamic>` | Resets today's Reel count to 0 (debug/testing helper) |
| `openAccessibilitySettings` | — | `bool` | Opens Android Accessibility Settings screen |
| `openOverlaySettings` | — | `bool` | Opens Android System Overlay Settings screen |
| `EventChannel("mindlol/events")` | — | `Stream<Map>` | Real-time status event stream emitted via `StatusBroadcaster` whenever count, limit, or config updates |

---

## 7. Validated Quality Bar & Test Suite

- **Flutter Unit & Widget Tests**: 30 automated tests passing (`test/blob_state_test.dart`, `test/blob_widget_test.dart`, `test/home_screen_test.dart`, `test/settings_screen_test.dart`, `test/mindlol_platform_test.dart`, `test/widget_test.dart`).
- **Flutter Analysis**: Clean run with 0 errors, 0 warnings.
- **Android Native JVM Tests**: All 51 unit test tasks passing (`ReelCounterRepositoryTest.kt`, `InstagramReelCounterTest.kt`, `InstagramReelDetectorTest.kt`, `StatusBroadcasterTest.kt`).
- **Physical Device Validation**: Validated on physical device (`10AFCB07PR003X0`). Settings screen navigation, limit modification, app toggles disabling/enabling, HUD auto-hide when app is disabled, Blob stage state recalculation, and reboot/restart persistence verified.

