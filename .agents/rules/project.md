---
trigger: always_on
---

# Project rules — Reel Counter & Blocker

## What this is
A digital-wellbeing app that counts short-form video consumption (Instagram
Reels, YouTube Shorts, Facebook Reels, Snapchat Spotlight) in real time and
lets users cap and block their feed once they hit a limit. Launching on
Android and iOS. The mascot "Blob" — a single character that visibly
degrades as the day's count rises — is the emotional core of the product
and the primary retention/sharing hook.

## Stack
- Cross-platform shell: Flutter (Dart)
- Android native module: Kotlin, AccessibilityService
- iOS native module: Swift, FamilyControls / ManagedSettings / DeviceActivity
- Mascot/animation: Rive
- Backend: Firebase (Auth, Firestore, Remote Config, Cloud Functions,
  Crashlytics, Analytics)
- Payments: RevenueCat (Play Billing + StoreKit)

## Build order — do not skip or reorder
1. Debug probe (`.agents/rules/android-detection.md` has the detail) —
   confirm real accessibility-tree structure on real devices before
   writing any production detection logic.
2. Detector design from actual probe findings, with structural fallback
   and direction-aware counting.
3. Overlay implementation, tested on real devices across nav modes and
   with TalkBack enabled.
4. Flutter shell: onboarding, home screen, mascot integration.
5. iOS native module (start the Apple FamilyControls entitlement request
   immediately — it's slow and outside your control; don't wait for step 4
   to finish before starting it).
6. Meme engine, paywall, leaderboard.
7. Full QA pass against the quality bar below, then store submission prep.

Confirm each phase with the user before moving to the next. Don't jump
ahead because a later phase looks easier or more interesting — the
detection and overlay phases are where silent, months-long bugs live if
skipped or rushed.

## Quality bar (release-candidate gate)
- Cold start under 2 seconds to interactive
- Crash-free session rate above 99.5%, verified via Crashlytics over a
  sustained internal-testing period, not a single smoke test
- Foreground detection service battery drain explicitly measured, not
  assumed
- No silent failures: every user-facing screen has explicit error/empty
  states (permission denial, network failure, invalid input)
- Automated tests specifically covering direction-aware counting edge
  cases (rewatch, overscroll bounce, fast fling)

## Non-negotiables
- Never claim iOS achieves per-video counting. It doesn't, and the API it's
  built on can't. Disclose iOS tracking as an estimate in the UI itself,
  not buried in a help doc.
- Never let a "clean/simple" implementation quietly trade away a
  reliability or compliance requirement in this file or in
  `android-detection.md` / `privacy.md`. Flag the tradeoff instead of
  picking the easier path silently.
- Ask before assuming scope on anything ambiguous rather than guessing.

## Design language
Minimal, flat, one accent color per screen, sentence-case copy, one type
family (3 weights max), no gradients/drop shadows. Onboarding is 3 screens
max. Home screen is one screen, no tabs. The mascot is always the largest
visual element wherever it appears — everything else stays quiet around it.
Full light/dark mode and TalkBack/VoiceOver support are required, not
optional — an app about digital wellbeing that fails accessibility users is
both a bad look and a store-policy risk.