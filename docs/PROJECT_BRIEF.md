# Project Brief — MindLol

## Product Goal
MindLol is a cross-platform digital wellbeing application that detects and counts short-form video consumption (Instagram Reels, YouTube Shorts) in real time on mobile devices. Users can set custom daily consumption limits to automatically trigger soft boundaries or blocking overlays when limits are reached.

## Product Roadmap & Status

### Phase 0 - Phase 7: Android Native Engine Validation & Hardening (COMPLETED)
- Empirical AccessibilityService probe, structural Reel detector, direction-aware counter, persistent repository, and non-autonomous blocking overlay validated and hardened on physical Android hardware.

### Phase 8: Transition from Android Probe to MindLol Application (CURRENT PHASE)
- Transitioned product identity from engineering debug probe to the official **MindLol** application.
- Standardized user-facing app and service names ("MindLol" and "MindLol Accessibility").
- Gated developer diagnostics and test unlock controls behind `BuildConfig.DEBUG`.
- Established clean architecture boundaries between native Android enforcement engine and upcoming cross-platform Flutter UI layer.

---

## Core Technical Constraints & Rules
- **Ground truth before code**: Detection logic must trace back to accessibility tree dumps captured from real devices running production target apps.
- **Obfuscation resilience**: Do not rely strictly on resource IDs (handle `0_resource_name_obfuscated`). Structural matching fallbacks are required.
- **Direction-aware counting**: Distinguish forward swipe vs. rewatch/swipe-back vs. fling bounce.
- **On-device privacy**: Content analysis stays entirely on-device. No visual capture, raw tree dumps, or specific video metadata leaves the device.
- **Compliance & Blocking**: All blocking features must require explicit user interaction (tap to unblock/dismiss). Never take autonomous device actions.
