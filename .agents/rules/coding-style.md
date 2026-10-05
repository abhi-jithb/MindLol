---
trigger: always_on
---

# Coding style rules

## General
- Prefer clear, boring code over clever code. This app has two genuinely
  hard, fragile subsystems (Android accessibility detection, iOS Screen
  Time integration) — everything else (UI, backend wiring, payments)
  should be as simple and conventional as possible so complexity budget is
  spent where it's actually needed.
- No silent fallbacks. If a lookup fails, a permission is denied, or an
  assumption doesn't hold at runtime, handle it visibly (log, user-facing
  state, or explicit error) — never swallow it and continue as if nothing
  happened.
- Comments should explain *why*, especially around detection heuristics,
  debounce values, and platform-specific workarounds. A comment like
  "values from real device dump, session 2026-XX-XX, see /docs/probe-notes"
  is far more valuable than restating what the code does.
- No hardcoded resource-ids, class-name hints, or detection thresholds in
  committed production code — these live in Remote Config per
  `android-detection.md`. Local defaults for development are fine if
  clearly marked as such.

## Kotlin (Android native module)
- Null-safety is not optional: every `AccessibilityNodeInfo` access must
  handle the null case explicitly, not with `!!`.
- Recycle `AccessibilityNodeInfo` instances after use to avoid leaks — this
  is a real, known source of subtle memory issues in accessibility service
  code.
- Coroutine scopes tied to service lifecycle must be cancelled in
  `onDestroy`/`onInterrupt` as appropriate — no leaked scopes.
- Every bitwise flag expression (window/overlay flags especially) must be
  verified against documented flag semantics and tested on a real device —
  do not assume an expression does what its name suggests.

## Swift (iOS native module)
- `DeviceActivityMonitor` and `ManagedSettings` extension targets must be
  separate Xcode targets, not code folded into the main app target — this
  is an Apple platform requirement, not a preference.
- Schedule/token handling must account for known API instability (tokens
  and schedules can behave unpredictably beyond ~45 minutes) — chain
  shorter schedules rather than relying on one long-running one.
- All Screen Time API usage must degrade gracefully if the user has not
  granted `FamilyControls` authorization — never crash or silently no-op
  in a way the user can't see.

## Dart / Flutter (shared shell)
- Platform channel messages between Dart and native modules use a single,
  versioned, normalized event shape (e.g. `CounterEvent`) so Android and
  iOS differences stay isolated in their native modules, not leaked into
  shared UI code.
- No business logic (counting, cap comparison, unlock difficulty) in
  widget/UI code — keep it in testable, platform-independent Dart classes
  that the UI observes.
- All user-facing strings follow the tone in `project.md`: sentence case,
  no shaming copy, especially around cap-hit and Shattered-mascot states.

## Testing expectations
- Detection/counting logic: unit tests covering forward-swipe, rewatch/
  back-swipe, and fast-fling/overscroll-bounce scenarios specifically, not
  just a single happy-path case.
- Overlay behavior: documented manual test results (device, nav mode,
  TalkBack on/off) attached to the relevant PR — this can't be fully
  unit-tested, so it needs recorded manual verification instead of being
  skipped.
- Payment/subscription flows: tested against RevenueCat's sandbox modes for
  both Play Billing and StoreKit before any release build.

## Review standard
Before marking any task complete, check it against the relevant rule file
(`android-detection.md`, `privacy.md`, or the quality bar in `project.md`)
it touches. A task that's functionally working but silently violates one of
those constraints is not done.