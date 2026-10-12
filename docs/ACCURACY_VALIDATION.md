# MindLol — Accuracy Validation & Regression Protocol

## 1. Core Commitment & Confidence Framing

Per `.agents/rules/android-detection.md` and `.agents/rules/privacy.md`:
* **High-Confidence Estimates**: Short-video counts are a high-confidence estimate based strictly on OS Accessibility API signals, not internal proprietary app state. MindLol never claims absolute ground-truth perfection.
* **On-Device Processing**: Content detection runs 100% on-device. No titles, captions, usernames, URLs, comments, or screenshots ever leave the device or enter version control.
* **Single-Device Evidence Boundary**: All current real-device results reflect physical testing on a Vivo V2529 running Android 16, Instagram, and YouTube 21.40.161. These results are empirical benchmarks for this platform/version combination and must not be framed as universal multi-OEM guarantees until Phase 17.

---

## 2. Measurable Acceptance Criteria

To ensure reliable, trustable tracking without user frustration, validation runs must meet the following quantitative criteria:

| Category | Metric / Target | Hard Threshold | Measured (Vivo V2529) |
| :--- | :--- | :--- | :--- |
| **False Increments** | Overcounting rate on valid forward transitions | **< 1.0%** (Ideally 0.0%) | **0.0%** (0 / 50) |
| **Missed Transitions** | Undercounting rate on genuine video switches | **< 4.0%** | **2.0%** (1 / 50) |
| **Backward Rewatches** | Rejection rate of reverse / swipe-back navigation | **100.0%** | **100.0%** (5 / 5) |
| **Partial Gestures** | Rejection rate of cancelled / sub-threshold drags | **100.0%** | **100.0%** (10 / 10) |
| **Comments Scrolling** | Rejection rate of vertical scrolling inside bottom sheets | **100.0%** | **100.0%** (2 / 2) |
| **Brief Surface Exit** | Baseline preservation on departures < 10 seconds | **100.0%** | **100.0%** (1 / 1) |
| **Extended Departure** | Baseline reset on departures >= 10 seconds | **100.0%** | **100.0%** (Verified in unit test) |

---

## 3. Repeatable Validation Procedures

Each target platform integration must be verified across 7 distinct scenarios before release:

### Scenario 1: Forward Transitions
* **Procedure**: Execute 50 sequential forward video transitions with varied touch profiles:
  * Fast flings (< 200ms duration, e.g. 180ms).
  * Medium swipes (200–350ms duration, e.g. 260ms, 300ms).
  * Slow drags (> 350ms duration, e.g. 420ms).
* **Expected Outcome**: Each full video switch increments the session count by exactly 1.
* **Failure Class**: Double-increment (overcount) or failure to register (missed count).

### Scenario 2: Backward Rewatches
* **Procedure**: Execute swipe-back gestures (`y: 700 -> 1900`, delta = +1200px) returning to previously viewed videos.
* **Expected Outcome**: Direction recognized as `BACKWARD`. 0 increments emitted.
* **Failure Class**: Treating reverse navigation or positive recoil bounce as a new video view.

### Scenario 3: Partial or Cancelled Gestures
* **Procedure**: Perform small vertical drags under 250px displacement (e.g. `y: 1500 -> 1350`, delta = -150px) and release without crossing snap boundaries.
* **Expected Outcome**: Peak displacement remains below `minForwardScrollPx` (400px). View snaps back to 0. 0 increments emitted.
* **Failure Class**: Summing multiple small drags across time into a false increment.

### Scenario 4: Rapid Flings, Inertia & Settling
* **Procedure**: Rapid flick gesture that emits multiple inertial scroll events followed by deceleration gaps and opposing recoil settling (e.g. -143px or +279px).
* **Expected Outcome**: Exactly 1 increment emitted on initial threshold crossing. Late inertia events suppressed by `debounceWindowMs` (650ms). Opposing recoil bounce suppressed by settling states (`FORWARD_SETTLING` / `BACKWARD_SETTLING`).
* **Failure Class**: Double-counting due to deceleration event gaps or premature resets to rest.

### Scenario 5: Comments-Panel Interactions
* **Procedure**: Open the comments bottom sheet on a supported short video surface. Perform vertical scrolling inside the comments list, then dismiss the sheet.
* **Expected Outcome**: `isCommentsOpen` returns `true`. All scroll events suppressed with `COMMENTS_OVERLAY_ACTIVE_IGNORED`. 0 increments emitted.
* **Failure Class**: Incrementing video count while the user is simply reading comments.

### Scenario 6: Transient Surface Departures
* **Procedure**: Press HOME or receive an in-app system dialog / store popup. Remain away from the target app for 2–5 seconds (< 10s tolerance). Re-enter the target app and perform the next video transition.
* **Expected Outcome**: Session baseline preserved. Next transition counted immediately without requiring a baseline re-initialization drop.
* **Failure Class**: Prematurely wiping baseline on brief popups, causing missed counts.

### Scenario 7: Daily Count Persistence & Limit Activation
* **Procedure**: Advance count to configured `dailyLimit`. Verify repository persistence in `SharedPreferencesStore`. Restart app / activity and inspect status broadcaster and blocking overlay.
* **Expected Outcome**: `limitReached == true`. Overlays and HUD update immediately. Count preserved across process recreation.
* **Failure Class**: Count resetting to 0 or limit state dropping on process recreation.

---

## 4. Privacy-Safe Regression Fixture Specification

All automated detector regression tests must utilize sanitized JSON fixtures under:
* `android/app/src/test/resources/fixtures/`
* `app/src/test/resources/fixtures/`

### Allowed Fields
* `packageName` (e.g., `com.instagram.android`, `com.google.android.youtube`)
* `viewId` (allowlisted resource IDs only, e.g., `clips_viewer_view_pager`, `reel_watch_fragment_root`)
* `className` (e.g., `androidx.recyclerview.widget.RecyclerView`, `android.view.SurfaceView`)
* `bounds` (screen coordinate arrays `[left, top, right, bottom]`)
* `visible` (boolean)
* `scrollable` (boolean)
* `children` (nested list of structural nodes)

### Strictly Forbidden Fields
* Arbitrary text, captions, or video descriptions (`text`).
* Usernames, profile handles, or account identifiers.
* HTTP / HTTPS URLs or media endpoints.
* Visual imagery, screenshots, or base64 pixel data.

---

## 5. Known Limitations & Untested Environments

The following areas are explicitly documented as **pending future validation phases** before public release:
1. **Multi-OEM Behavior**: Samsung OneUI and Xiaomi HyperOS use proprietary window managers and aggressive background-service task killers. Multi-OEM validation is required in Phase 17.
2. **TalkBack Concurrency**: Overlay and accessibility service must be verified with TalkBack screen reader enabled to ensure no navigation interference for visually impaired users.
3. **Jetpack Compose Migration**: Target apps periodically migrate surfaces to Compose. When resource IDs are stripped in future release builds, structural fallback heuristics will be validated via hot-patchable Remote Config rules (Phase 16).
