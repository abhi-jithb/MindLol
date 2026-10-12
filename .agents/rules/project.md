---
trigger: always_on
---

# Project rules — MindLol

## What this is
MindLol — a digital-wellbeing app that counts short-form video consumption
(Instagram Reels, YouTube Shorts, Facebook Reels, Snapchat Spotlight) in
real time and lets users cap and block their feed once they hit a limit.
Launching on Android and iOS. The mascot "Blob" — a single character that
visibly degrades as the day's count rises — is the emotional core of the
product and the primary retention/sharing hook. The name is deliberately
self-aware/sarcastic ("your mind... lol") — tone should lean into that,
never into shaming copy.

Blob's state thresholds are **percentage-of-daily-limit**, not fixed reel
counts (this changed from the original spec, and is the correct version):
- 0–25% of limit → Calm
- 25–75% → Wobbly
- 75–150% → Cracked
- 150%+ → Shattered
This scales correctly regardless of what daily limit a given user sets —
do not revert to fixed-count thresholds.

## Stack
- Cross-platform shell: Flutter (Dart)
- Android native module: Kotlin, AccessibilityService
- iOS native module: Swift, FamilyControls / ManagedSettings / DeviceActivity
- Mascot/animation: Rive
- Backend: Firebase (Auth, Firestore, Remote Config, Cloud Functions,
  Crashlytics, Analytics)
- Payments: RevenueCat (Play Billing + StoreKit)

## Current status (update this section as phases complete — don't let it go stale)
- Android detection engine (Instagram, structural + transition-based
  counting): 🟢 working, validated on one real device
- Blocking overlay (Instagram): 🟢 working, validated on one real device
- Persistence, lifecycle hardening (app-switch, Home, Recents): 🟢 working
- Flutter shell + live sync (MethodChannel/EventChannel, floating HUD):
  🟢 working
- Blob personality/copy: 🟢 working (Flutter-native animation, no Rive yet)
- YouTube Shorts detection: 🔴 not yet validated — do not assume Instagram's
  resource-id-based approach transfers; YouTube has been migrating surfaces
  to Jetpack Compose, which produces a structurally different tree
- Multi-OEM validation (Samsung/Xiaomi/etc., not just one device): 🔴 not done
- TalkBack-concurrent overlay testing: 🔴 not done — required before this
  is considered release-safe, not optional polish
- Remote Config for detection rules: 🔴 not wired yet — resource-ids are
  currently hardcoded; this is higher priority than it looks, see note below
- Settings/usage-control UI: 🟡 next in the original plan, but see reorder
  note below
- iOS engine: 🔴 not started
- Backend (Firebase), monetization (RevenueCat): 🔴 not started, correctly
  deferred — don't pull these forward early
- Package ID is still `com.mindlol.probe` (legacy from the probe phase):
  🔴 unresolved — see hard gate below

## Build order — reordered based on what Phase 0-12 actually taught us
1. ~~Debug probe~~ ✅ done for Instagram. **Re-run the same probe process
   against current YouTube Shorts before building more UI on top of the
   current engine.** Instagram's clean ViewPager/resource-id structure is
   not evidence YouTube will look the same — if YouTube needs a structural
   (non-resource-id) fallback, that's a core-architecture finding and it's
   cheaper to learn now than after Settings is built on an assumption that
   only holds for one of two platforms.
2. Wire Remote Config for detection rules **alongside** the YouTube probe
   work, not after. You're about to find out firsthand how fragile these
   rules are — don't keep shipping APK updates to patch them.
3. Settings / usage-control UI (daily reset, limit configuration,
   supported-app toggles).
4. Multi-OEM validation pass + TalkBack-concurrent overlay testing — both
   required before any public testing track, not deferred to "later
   hardening."
5. Onboarding polish (no developer language, clean permission flow).
6. iOS native module. Start the Apple FamilyControls entitlement request
   as early as possible — it's slow and outside your control.
7. Package ID migration off `com.mindlol.probe` — **hard gate: must happen
   before any closed/open testing track on Play, not just before
   production release.** Changing `applicationId` after real users and
   reviews accumulate on a listing effectively forfeits them.
8. Mascot production assets (Rive), meme engine, paywall, leaderboard.
9. Full QA pass against the quality bar below, then store submission prep.

Confirm each phase with the user before moving to the next. Don't jump
ahead because a later phase looks easier or more interesting — the
detection and overlay phases are where silent, months-long bugs live if
skipped or rushed. "Tests passed" is not sufficient evidence of done —
confirm what scenarios the tests actually cover (see coding-style.md).

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