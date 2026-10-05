# Phase 0 debug probe — how to run it

## Setup
1. Add `probe_service_config.xml`, `NodeTreeDumper.kt`, and `TreeLoggerService.kt` to a **debug build variant** of the app — keep this out of the release build entirely.
2. Merge the manifest snippet and add the `probe_service_description` string.
3. Build and install on a **real device** (not an emulator — some OEM behaviors around obfuscation/overlays only show up on real hardware).
4. Settings → Accessibility → enable the probe service.

## Capture a session
1. Open Instagram, go to Reels.
2. Scroll through 10-15 reels doing a mix of: normal forward swipes, at least 2 swipe-backs to rewatch, and one fast fling through several at once.
3. Switch to YouTube, go to Shorts, repeat the same pattern.
4. Let the app sit idle on a reel for a few seconds at least once (checks whether idle time generates spurious scroll events).

## Pull the log
```
adb pull /sdcard/Android/data/<your.applicationId>/files/tree_dump.log
```
Or watch it live:
```
adb logcat -s TreeLoggerService
```

## What to actually look for when reading it

**1. Resource-id obfuscation** — search the file for `OBFUSCATED`. If the reel surface's container shows this instead of a real id, resource-id-based lookup is not viable for that app/build; the detector needs to fall back to structural matching (class + bounds + scrollable) instead.

**2. Compose vs. classic View tree** — look at the `[ClassName]` on each line. `androidx.compose.ui.platform.ComposeView` or similarly Compose-flavored class names, paired with a much flatter tree (few levels, generic node types) than a classic `RecyclerView`/`ViewPager2` tree, tells you that app's Shorts/Reels surface has migrated to Compose and needs a different detection strategy than the View-based one assumed earlier.

**3. What actually identifies "one full-screen reel"** — find the node that corresponds to the visible video: it should have `scrollable=true` on a parent container, and the full-screen-video child should have `bounds` close to full device width/height. Note its class name and whether `children` count changes predictably as you swipe — that's the real signal to build the structural fallback rule on, rather than a resource-id string.

**4. Event timing around rewatch vs. forward swipe** — compare the dumps immediately after a forward swipe vs. after a swipe-back. If nothing in the tree distinguishes "new video" from "returned to previous video" (same bounds, same class, only content/description differs or doesn't), that confirms direction-aware counting needs to come from gesture/scroll direction, not just tree-state comparison — worth confirming either way before deciding.

## After this
Take the actual findings back into the detector design — replace the placeholder `ReelSurfaceConfig` resource-ids with whatever this session shows is real and reliable (or the structural fallback rule if ids are obfuscated), before writing any more production detection code.