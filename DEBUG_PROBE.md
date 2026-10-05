# Phase 0: Debug Probe Documentation

## Overview
This document details the architecture, setup instructions, testing procedure, and output specifications for the Phase 0 disposable Android Accessibility Probe.

The probe exists to record real accessibility tree structures from physical OEM devices running production Instagram and YouTube apps.

---

## 12 Evaluation Questions

The captured logs must be analyzed to answer:
1. **Entering Surface**: What accessibility events are emitted when entering Instagram Reels or YouTube Shorts?
2. **Forward Navigation**: What events occur when moving forward to another video (e.g., swipe up)?
3. **Backward Navigation**: What events occur when swiping back to a previous video?
4. **Fling & Overscroll**: What happens during rapid flings or overscroll bounce?
5. **Node Hierarchy**: What node structures are exposed in the accessibility tree?
6. **Resource IDs**: Which nodes expose view resource IDs (`viewIdResourceName`)?
7. **Obfuscation Status**: Are resource IDs human-readable (e.g. `reel_container`) or obfuscated (`0_resource_name_obfuscated`)?
8. **Class Names**: What UI class names are exposed (e.g., `RecyclerView`, `ViewPager2`, `ComposeView`)?
9. **Screen Bounds**: What are the bounding rectangles (`boundsInScreen`) of visible video surface containers?
10. **Scrollability**: Which nodes have `isScrollable = true`?
11. **Text & Content Descriptions**: What text or accessibility description labels are exposed on screen?
12. **Structural Information**: Does the accessibility tree provide reliable structural signals to distinguish short-video surfaces from general feed items?

---

## Log Output Format

Logs are formatted to logcat under tag `TreeLoggerService` and appended to `/sdcard/Android/data/com.mindlol.probe.debug/files/tree_dump.log`.

Each log entry includes:
- **Event Header**:
  - `TIMESTAMP`: `YYYY-MM-DD HH:mm:ss.SSS`
  - `PACKAGE`: Package name (`com.instagram.android` or `com.google.android.youtube`)
  - `EVENT_TYPE`: Standard AccessibilityEvent type string (e.g., `TYPE_VIEW_SCROLLED`)
  - `EVENT_CLASS`: Event source class name
  - `EVENT_INDEXES`: `fromIndex`, `toIndex`
  - `EVENT_SCROLL`: `scrollX`, `scrollY`
- **Node Hierarchy (Indented by depth)**:
  - `className`: Full Java/Kotlin class name
  - `id`: Resource ID name or `OBFUSCATED` flag
  - `text`: Node text content
  - `desc`: Content description
  - `bounds`: Screen bounding rect `[left,top][right,bottom]`
  - `visible`: `isVisibleToUser` boolean flag
  - `scrollable`: `isScrollable` boolean flag
  - `children`: Number of child nodes

---

## Setup & Execution Instructions

### 1. Build and Install
Build the debug probe application on a physical Android device:
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Enable Accessibility Service
1. Open physical device **Settings** -> **Accessibility**.
2. Locate **MindLol Accessibility Probe**.
3. Toggle the service **ON** and accept the permission prompt.

### 3. Run Test Scenarios

#### Scenario A: Instagram Reels (`com.instagram.android`)
1. Open Instagram.
2. Navigate to the Reels tab.
3. Perform **10 forward swipes** (pause 2 seconds on each reel).
4. Perform **2 backward swipes** to rewatch previous reels.
5. Perform **1 rapid fling** passing multiple reels quickly.
6. Leave device idle on one reel for **5 seconds**.

#### Scenario B: YouTube Shorts (`com.google.android.youtube`)
1. Open YouTube.
2. Tap into Shorts.
3. Perform **10 forward swipes** (pause 2 seconds on each short).
4. Perform **2 backward swipes**.
5. Perform **1 rapid fling**.
6. Leave device idle on one short for **5 seconds**.

---

## Extracting Logs

### Option A: Read Live Logcat
```bash
adb logcat -s TreeLoggerService
```

### Option B: Pull Log File from Device
```bash
adb pull /sdcard/Android/data/com.mindlol.probe.debug/files/tree_dump.log ./tree_dump.log
```

---

## Privacy & Safety Compliance
- All logs remain strictly local to the device filesystem.
- No network connections, telemetry, screenshots, or external data transmission.
