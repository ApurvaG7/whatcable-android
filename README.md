# WhatCable (Android)

A no-root charging-diagnostics app for Android. It shows what your charger and
cable are actually doing, and lets you compare cables head to head.

## What it does
- **Live charging power**: real watts into the battery, voltage, current, and
  temperature.
- **Negotiated ceiling**: the max voltage and current the charger and cable
  agreed to, with a basic-vs-fast-charger read.
- **Compare cables**: run a timed test per cable on the same charger and rank
  them by measured power, so you can spot a weak cable.
- **USB device inspector**: descriptor details for anything plugged into the
  phone's USB-C port (BOS, Billboard alt modes, interfaces).

Everything runs on public Android APIs. No root, no special permissions, no
network access.

## What it can't do (and why)
A normal Android app cannot read input-side wattage, the charger's type string,
the USB-C port role, or a cable's e-marker chip. Those live in kernel sysfs
nodes that Android's security policy (SELinux) keeps out of reach of normal
apps. So WhatCable judges a cable by *outcome*: the negotiated voltage, and a
side-by-side comparison of how much power each cable actually delivers on the
same charger.

**The harder limit: Android cannot identify a cable at all.** Unlike the macOS
version, which reads a cable's e-marker (its declared 60/100/240W rating and
USB 2.0/3.x/4 capability) through the host platform, a phone usually never
interrogates the cable's e-marker in the first place. Its USB-C controller only
runs cable discovery when it needs to (high power, alt-mode entry), so most of
the time the cable's identity is never read into the system. That data is
therefore unavailable to *any* app tier, not just unprivileged ones: not via the
Play Store sandbox, not via a shell-domain helper like Shizuku, and not reliably
even with root. The cable's own spec simply isn't there to read. Everything this
app shows is the phone's *experience* of the cable (watts pulled, link speed
negotiated), never the cable's declared capability. Since the phone is almost
always the bottleneck, only a grossly bad cable shows up; a good cable and an
adequate one look identical.

For a fair comparison, test each cable on the same charger at a similar battery
level, ideally below 50% so charging isn't throttled near full.

## Build
Requires a JDK (the Android Studio bundled JBR works) and the Android SDK.

```bash
./gradlew :app:installDebug   # build and install to a connected device
./gradlew :app:assembleDebug  # build the debug APK only
```

## Tech
Kotlin, Jetpack Compose (Material 3), Hilt, Room, Coroutines. Single-Activity
MVVM. Min SDK 26, target/compile SDK 36.
