🫧 MindLol

Your scroll. Your rules. Your mind.

<p align="center">
  <strong>A digital-wellbeing app that helps you become aware of your short-form video consumption — and take back control, one swipe at a time.</strong>
</p><p align="center">
  <em>Less mindless scrolling. More mindful living.</em>
</p>---

💭 The problem

You open Instagram to watch one Reel.

One becomes five. Five becomes twenty. And suddenly, the time you wanted for your projects, learning, or simply resting has disappeared.

The problem isn't that short-form video exists. It's that we often lose track of how much we're consuming.

Most screen-time tools focus on how long an app stays open. But time alone doesn't tell the whole story.

What if you could see how many individual short-form videos you've watched, set your own daily limit, and get a gentle nudge when it's time to stop?

That's the idea behind MindLol.

✨ What is MindLol?

MindLol is a digital-wellbeing project exploring a different approach to managing short-form video consumption.

Instead of relying only on app timers, MindLol aims to count individual Reels and Shorts, make that consumption visible, and help users stick to a limit they choose.

At its heart is a simple belief:

«Technology should help us regain control of our attention, not compete endlessly for it.»

🚀 What's working so far?

The Android prototype has progressed beyond a mockup. It has a native detection and counting engine connected to a Flutter interface, with core behavior tested on a physical Android device.

- 🔎 Structural detection — Uses Android Accessibility to identify supported short-video surfaces instead of treating every scroll as a video view.
- 🔢 Transition-based counting — Counts forward video transitions while avoiding increments for backward navigation and unchanged positions.
- 💾 Persistent daily limits — Stores the daily count and user-configured limit across app restarts.
- 🚧 Limit enforcement — Implements an Android overlay-based blocker when the configured limit is reached.
- ⚡ Floating count indicator — Displays a compact count over a supported video surface and updates as the count changes.
- 🔄 Live synchronization — Keeps the Flutter dashboard, native counter, blocker, and floating indicator connected to the same authoritative state.
- 🫧 Blob, the MindLol mascot — Changes its expression, motion, and messaging according to consumption relative to the user's daily limit.
- 🌗 Light and dark themes — Supports both appearances, with reduced-motion handling for the mascot.

The Android implementation has automated tests, Flutter tests, and a debug APK build. Real-device testing has validated the core Instagram workflow.

Current validation boundary: Instagram Reels is the primary validated detection target. YouTube Shorts detection still requires dedicated investigation and validation before it can be presented as equally reliable. MindLol is under active development and is not yet a production-ready public release.

🫧 Meet Blob

Blob isn't here to shame you for scrolling.

It's a little companion that reflects your progress throughout the day.

State| What it means
🌱 Calm| You're within a comfortable range of your daily limit.
🫨 Wobbly| Your consumption is increasing.
💔 Cracked| You're approaching or crossing your limit.
🫧 Shattered| You've gone well beyond your limit. Time for a breather.

Blob's state is calculated relative to your configured daily limit, so its behavior adapts to your personal target rather than relying solely on a fixed number of videos.

Its personality is designed to be playful, slightly weird, and supportive — never judgmental.

🛠️ How it works

MindLol combines native Android capabilities with a shared Flutter product interface.

        Instagram Reels
               │
               ▼
     Android Accessibility
               │
               ▼
      Structural Detection
               │
               ▼
      Transition-based Counter
               │
               ▼
        Android Repository
          /      |       \
         ▼       ▼        ▼
    Storage   Blocker   Floating HUD
         │
         ▼
   Status Broadcaster
         │
         ▼
      EventChannel
         │
         ▼
      Flutter UI
      ├── Dashboard
      ├── Daily progress
      └── Blob mascot

Why this architecture?

Android owns detection and enforcement. Flutter does not independently calculate the authoritative count or maintain a separate daily limit.

The native repository acts as the single source of truth. Flutter requests configuration changes through a platform channel and receives aggregate state updates through an event channel.

This separation allows the product interface to evolve without duplicating the core counting logic.

🧰 Tech stack

Layer| Technology
Product interface| Flutter, Dart
Android engine| Kotlin
Short-video detection| Android AccessibilityService
Blocking interface| Android WindowManager overlay
Local persistence| SharedPreferences
Native–Flutter communication| MethodChannel, EventChannel
UI motion| Flutter animation framework
Testing| Flutter tests, Android unit tests
Development| Android SDK, Gradle, Git

🔐 Privacy by design

MindLol's design aims to minimize the information needed to support its core functionality.

- On-device detection: Short-video detection and counting run on Android.
- Minimal state sharing: The Flutter interface receives aggregate usage and configuration information rather than raw accessibility content.
- No content collection by design: Usernames, captions, comments, and video URLs are not required for the intended counting model.
- No screenshots required: The detector relies on accessibility structure rather than screenshot-based video recognition.
- Local-first prototype: The current prototype uses local persistence; cloud synchronization and analytics are not yet part of the implemented core.

Android Accessibility access and overlay permission are powerful capabilities. Their purpose, behavior, and limitations must be clearly disclosed to users.

These are implementation goals and current architectural choices, not a claim of a completed independent privacy audit or formal security certification.

🗺️ Roadmap

MindLol is being developed incrementally, with real-device evidence guiding the next steps.

- [x] Explore Instagram's accessibility hierarchy using a debug probe.
- [x] Implement and validate Instagram Reel detection and transition-based counting.
- [x] Persist daily counts and configurable limits.
- [x] Implement the Android blocking overlay and lifecycle handling.
- [x] Connect the native engine to Flutter.
- [x] Build the MindLol dashboard and Blob mascot.
- [x] Implement live count synchronization and a floating HUD.
- [x] Add Blob personality, motion, and reduced-motion support.
- [ ] Investigate YouTube Shorts accessibility structure and counting reliability.
- [ ] Design a configurable detection-rule architecture with safe local fallbacks.
- [ ] Expand testing to additional Android devices and OEMs, including concurrent TalkBack testing.
- [ ] Complete usage settings, app controls, and daily-reset behavior.
- [ ] Build a first-run onboarding and permissions experience.
- [ ] Develop the iOS experience using Apple's Screen Time APIs, within their platform limitations.
- [ ] Complete privacy, accessibility, reliability, and store-compliance reviews.
- [ ] Prepare production builds and public testing.

The roadmap may evolve as implementation and testing reveal new constraints.

🧪 Engineering principles

MindLol follows a few principles throughout development:

Observe before implementing. Inspect actual accessibility structures instead of assuming that apps expose the same UI hierarchy.

Count transitions, not random events. A scroll event alone does not prove that another video was watched.

One source of truth. Native counting, persistence, blocking, and the Flutter interface must agree on the current state.

Fail honestly. If a platform does not expose enough reliable information, document the limitation instead of pretending detection works.

Privacy first. Collect only what's necessary for the intended functionality.

Build in stages. Make it work, make it robust, then make it uniquely MindLol.

🌱 Why I'm building this

MindLol started with a simple question:

What if we could make our short-form video consumption visible enough to change our relationship with it?

I'm building this project to explore that question through practical software engineering — from Android accessibility internals and state management to cross-platform UI, privacy-conscious design, and real-device testing.

The goal isn't to eliminate entertainment or tell people how to live.

It's to help people make more intentional decisions about their attention.

I'm developing MindLol as an evolving project and looking forward to learning from real users, mentors, developers, and the startup community along the way.

🤝 Get involved

I'm interested in connecting with people who care about:

- Digital wellbeing and healthy technology.
- Android internals and accessibility engineering.
- Flutter and cross-platform product development.
- Human-centered design and privacy.
- Building and validating early-stage products.

Ideas, thoughtful feedback, technical discussions, and collaboration are welcome.

📌 Project status

Stage: Early Android prototype / active development
Primary validated target: Instagram Reels on a tested Android device
YouTube Shorts: Detection investigation pending validation
iOS: Planned, not yet implemented
Public release: Not yet available

MindLol is still evolving. The current implementation is a foundation for further validation, not a guarantee of reliable detection across every device or app version.

---

<p align="center">
  <strong>MindLol 🫧</strong><br/>
  <em>Your scroll. Your rules. Your mind.</em>
</p>