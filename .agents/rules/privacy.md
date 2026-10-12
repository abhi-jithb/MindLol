---
trigger: always_on
---

# Privacy rules — MindLol

## Core commitment
Content-detection logic runs entirely on-device. The app never inspects,
stores, or transmits the actual content of what a user watches — only that
a video-consumption event occurred. This is both a genuine product
commitment and the app's core trust pitch; treat any change that would
weaken it as requiring explicit user-facing disclosure, not a quiet
implementation detail.

## What is allowed to leave the device
- Daily aggregate counts per user (e.g. "42 reels today"), for streaks and
  friend-leaderboard features
- Account/auth identifiers (Firebase Auth)
- Crash and performance telemetry (Crashlytics/Analytics), scoped to
  technical diagnostics, not usage content
- Subscription/entitlement status (RevenueCat)

## What must never leave the device
- Raw accessibility node tree data or dumps (probe/debug output stays local
  and must never ship in a production build or be uploaded anywhere)
- Which specific videos, accounts, or content a user viewed
- Screenshots or any visual capture of the target apps
- Timestamps granular enough to reconstruct a user's precise scrolling
  session, beyond what's needed for the daily aggregate

## Debug tooling
Any accessibility-tree logging/debug probe is for local development only.
It must live in a debug-only build variant, must never be included in a
release build, and its output must never be transmitted off-device.

## Disclosure requirements
- Android: the Play Console Accessibility usage declaration and an in-app
  explainer (shown before the OS permission prompt) must describe, in
  plain language, exactly what the accessibility service does and why —
  vague or generic disclosure is a known rejection cause and also just
  isn't honest to the user.
- iOS: App Privacy labels must match actual data flow exactly. The
  in-app UI must disclose that iOS tracking is an estimate (time/session
  based), not per-video, since that's a real capability gap, not a
  formality.
- Both: privacy policy copy must be accurate and specific enough to be
  independently verified against what the code actually does — never
  write a policy claim the implementation doesn't back up.

## When in doubt
If a proposed feature or implementation choice would require collecting or
transmitting more than what's listed under "allowed to leave the device"
above, stop and flag it for explicit discussion rather than proceeding.