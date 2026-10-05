---
trigger: always_on
---

# Android detection rules

## Ground truth before code
Do not write or modify production detection logic (`ScrollDetectionService`,
`ReelSurfaceConfig`, or equivalent) based on assumed resource-ids or view
hierarchies. Every detection rule must trace back to an actual accessibility
tree dump captured from a real device running the current production build
of the target app. If no such dump exists yet for a given app/version,
building the debug probe and capturing one is the task — not guessing.

## Known failure modes to always guard against

**Resource-id obfuscation.** `findAccessibilityNodeInfosByViewId(...)` can
return nothing even when the target view exists, because release builds may
strip resource names, surfacing as a literal
`"packagename:id/0_resource_name_obfuscated"` value. Every resource-id-based
lookup must have a structural fallback (node bounds, class name,
`isScrollable()`) that doesn't depend on the id string resolving.

**Jetpack Compose migration.** A target app's short-form video surface may
be built in Compose rather than classic Views, producing a flatter semantics
tree without conventional view ids. Detection code must not assume a
classic `RecyclerView`/`ViewPager2` shape — check the actual dumped tree
structure for the app/version in question before writing the matching logic.

**Signature-based counting without direction.** Do not count "the visible
page's signature changed" as equivalent to "user watched a new reel."
Rewatching a previous video via swipe-back, and rapid fling/overscroll
bounce, both change tree state without representing a new video watched.
Counting logic must be direction-aware (forward swipe vs. back/rewatch) and
must be validated against these specific scenarios in a test, not just
happy-path forward scrolling.

**Debounce values are empirical, not assumed.** Any debounce/min-interval
constant used to suppress duplicate counts during scroll settling must be
justified by observed timing from real captured sessions, and called out in
a comment as such — not a round-number guess.

## Confidence framing
Per-video counts are a high-confidence estimate, not ground truth — the
detection layer only sees what the OS accessibility API exposes, never the
target app's actual internal state. Reflect this honestly:
- Code comments should not describe counts as exact.
- In-app copy should not claim exact per-video accuracy beyond what's
  actually verifiable.

## Remote-config requirement
Per-app detection rules (resource-ids, class-name hints, structural
thresholds) must be loaded from Firebase Remote Config, not hardcoded.
Target apps' UI changes will break matching rules periodically; fixes must
be deployable without an app-store review cycle.

## Blocking behavior — compliance-critical
All blocking must be a user-facing overlay that requires a direct user
action (a tap) to proceed — never an autonomous action the service takes
on the user's behalf (e.g. auto-navigating back, auto-closing the app)
without that direct trigger. This isn't a style choice: Google's
Accessibility API policy, enforced from Jan 28, 2026, prohibits
Accessibility Service usage that autonomously initiates/plans/executes
actions without a per-instance direct user trigger. Any implementation
that could be read as autonomous action must be flagged and reconsidered,
not shipped.

## Overlay testing — do not mark this task done without
- Tested against gesture navigation AND 3-button navigation
- Tested with TalkBack simultaneously enabled — the overlay must not break
  TalkBack's own navigation for users who depend on it
- Tested on at least two real OEM devices (e.g. Samsung + Pixel or Xiaomi),
  not emulator-only
- Verified the overlay's actual focusability/touch behavior matches intent
  — don't trust a flag expression without confirming its runtime effect on
  a real device; a self-canceling bitwise expression (e.g. `X.inv() and X`)
  is a real bug class here, not a hypothetical one

## Battery
The foreground detection service's battery impact must be explicitly
profiled (Android Studio Profiler or equivalent), not assumed acceptable.
This is a proven cause of 1-star reviews in this app category.