# WhatCable Android - Architecture Sketch

## Overview

Kotlin + Jetpack Compose app with a two-tier data model:

- **Tier 1 (Public)** - No root, Play Store eligible. USB Host API + BatteryManager.
- **Tier 2 (Root)** - Reads `/sys/class/typec/` sysfs tree for full cable identity.

The app detects root availability at launch and unlocks Tier 2 features automatically.

---

## Project Structure

```
app/
  src/main/
    kotlin/com/whatcable/android/
      MainActivity.kt
      WhatCableApp.kt              # Compose entry point
      di/                          # Hilt modules
        AppModule.kt
        UsbModule.kt

      core/
        model/
          CableSnapshot.kt         # Unified cable state (mirrors macOS CableSnapshot)
          UsbDevice.kt             # Parsed device descriptor
          BosDescriptor.kt         # BOS descriptor model
          BillboardDescriptor.kt   # Billboard capability descriptor
          PowerDelivery.kt         # PD state (voltage, current, PDOs)
          CableIdentity.kt        # E-marker VDOs (speed, current, active/passive)
          AltMode.kt               # Alt-mode SVID + status
          ChargingState.kt         # Current/voltage/wattage
        db/
          CableDatabase.kt         # Room DB for cable history
          CableDao.kt
        report/
          CableReport.kt           # Shareable diagnostic report

      data/
        usb/
          UsbHostScanner.kt        # UsbManager device enumeration
          BosReader.kt             # controlTransfer() BOS descriptor fetch
          BillboardParser.kt       # Billboard class 0x11 descriptor parsing
          DescriptorParser.kt      # Raw descriptor byte parsing
        power/
          BatteryMonitor.kt        # BatteryManager observer
          PowerSupplyReader.kt     # /sys/class/power_supply/ reader
        typec/
          TypeCPortReader.kt       # /sys/class/typec/ sysfs reader (root)
          CableIdentityReader.kt   # port0-cable/identity/ parser (root)
          AltModeReader.kt         # port0-partner/<altmode>/ parser (root)
        root/
          RootChecker.kt           # su/Magisk availability detection
          SuExecutor.kt            # Shell command execution via su

      domain/
        CableDiagnosticEngine.kt   # Combines all data sources into CableSnapshot
        SpeedClassifier.kt         # Determines cable speed tier from available evidence
        ChargingAnalyser.kt        # Evaluates charging performance
        TrustScorer.kt             # Cable quality/trust rating

      ui/
        home/
          HomeScreen.kt            # Main dashboard
          HomeViewModel.kt
        cable/
          CableDetailScreen.kt     # Full cable breakdown
          CableDetailViewModel.kt
        devices/
          DeviceListScreen.kt      # Connected USB devices
        charging/
          ChargingScreen.kt        # Live charging monitor
        history/
          HistoryScreen.kt         # Past cable snapshots
        components/
          SpeedBadge.kt            # USB 2.0/3.x/4 badge
          PowerBadge.kt            # Wattage badge
          TrustIndicator.kt        # Cable quality indicator
          FeatureGate.kt           # "Requires root" overlay

      service/
        UsbAttachService.kt        # Foreground service for USB attach events
        CableMonitorWorker.kt      # WorkManager periodic checks

    res/
      values/
        strings.xml
      xml/
        usb_device_filter.xml      # Intent filter for USB device attach
```

---

## Data Flow

```
USB Host API ─────────────┐
  (devices, descriptors,  │
   BOS, Billboard)        │
                          ▼
BatteryManager ────► CableDiagnosticEngine ────► CableSnapshot ────► UI
                          ▲
PowerSupply sysfs ────────┤
                          │
/sys/class/typec/ ────────┘
  (root only: cable ID,
   orientation, alt-modes,
   PD revision)
```

---

## Tier 1 Features (No Root)

### USB Device Enumeration
- Register `USB_DEVICE_ATTACHED` broadcast receiver
- Request permission via `UsbManager.requestPermission()`
- Parse raw descriptors from `getRawDescriptors()`
- Display device tree: configs, interfaces, endpoints

### BOS Descriptor Reading
- `controlTransfer(0x80, 0x06, 0x0F00, 0, buffer, 5, timeout)` to get BOS header
- Second transfer with full length to get complete BOS
- Parse capability descriptors: USB 2.0 Extension, SuperSpeed, SuperSpeedPlus, Container ID

### Billboard Descriptor Parsing
- Detect devices with class code `0x11` (Billboard)
- Parse Billboard Capability descriptor from BOS
- Extract: number of alt modes, alt mode SVIDs (DP, TB, etc.), alt mode status (success/fail/not attempted)
- Show which alt modes were offered and why they failed

### Charging Monitor
- Observe `BatteryManager` via `ACTION_BATTERY_CHANGED`
- Display: current (mA), voltage (mV), calculated wattage, temperature
- Read `/sys/class/power_supply/usb/type` for charger classification
- Track charging curve over time (Room DB)

---

## Tier 2 Features (Root)

### Cable Identity (E-Marker)
Read from `/sys/class/typec/port0-cable/identity/`:
- `id_header` - Cable VDO header (USB-IF assigned)
- `product_type_vdo1` - Passive/active, max speed (USB 2.0/3.2 Gen1/Gen2/USB4), max current (3A/5A)
- `product_type_vdo2` - Active cable details (SBU type, voltage support)
- `product` - Manufacturer + product ID

### Port State
Read from `/sys/class/typec/port0/`:
- `orientation` - normal/reverse
- `usb_capability` - usb2/usb3/usb4
- `power_operation_mode` - default/1.5A/3.0A/usb_power_delivery
- `usb_power_delivery_revision` - 2.0/3.0/3.1
- `data_role` - host/device
- `power_role` - source/sink

### Alt-Mode Partner
Read from `/sys/class/typec/port0-partner/`:
- Enumerate `<altmode.N>/` directories
- Parse SVID (0xFF01 = DisplayPort, 0x8087 = Intel/TB)
- Read `active` state
- Read `mode` and `vdo`

### Device Variance Handling
OEM sysfs paths differ. Strategy:
- Probe known paths at startup: `port0`, `port1`, multiple typec controller names
- Cache discovered paths
- Graceful degradation when nodes are missing or permission-denied

---

## Key Technical Decisions

### Language & UI
- **Kotlin** with coroutines for async USB operations
- **Jetpack Compose** for UI (Material 3)
- **Hilt** for dependency injection
- **Room** for cable history persistence

### USB Host Mode Constraint
The phone must be in host mode (OTG) to enumerate devices. This means:
- Testing cable between phone and a peripheral (drive, hub, dock) = works
- Testing cable between phone and charger = only charging data, no device enumeration
- The app should detect and explain this limitation clearly in the UI

### Permission Model
- `USB_DEVICE_ATTACHED` intent filter in manifest
- Runtime `UsbManager.requestPermission()` for device access
- No dangerous permissions needed for Tier 1
- Root detection via checking for `su` binary / Magisk presence

### Offline Cable Database
Ship a bundled known-cables database (VID/PID to manufacturer/model mapping), similar to macOS WhatCable's CableDB. Update via app updates or remote config.

---

## What This Gives Users

### Without root (most users):
- "What's plugged into my phone?" - full device descriptor breakdown
- "Is my cable/dock advertising the right alt modes?" - Billboard parsing
- "How fast is my cable charging?" - live wattage with history
- "Does this cable support SuperSpeed?" - BOS descriptor evidence

### With root (power users):
- "What speed is my cable rated for?" - e-marker max speed
- "Is this an active or passive cable?" - cable identity VDO
- "Which way is the cable plugged in?" - orientation
- "What PD revision is negotiated?" - PD rev from sysfs
- "What alt modes are active?" - partner alt-mode enumeration

---

## Build & Distribution

- **Min SDK:** 26 (Android 8.0) - broadest USB Host API coverage
- **Target SDK:** 35 (Android 15)
- **Distribution:** Google Play (Tier 1 only features described) + GitHub APK (includes root features)
- **Play Store policy:** Root features must not be the primary function. Frame the app as "USB device inspector with advanced diagnostics for rooted devices."

---

## Tier 1.5: Shizuku Integration (No Root, System API Access)

Shizuku is confirmed viable for accessing `@SystemApi` USB port APIs. This creates a
middle tier between basic (Tier 1) and root (Tier 2) that unlocks most of the valuable
data without requiring a rooted device.

### How it works

Shizuku runs a server process as UID 2000 (Android's `shell` user) via ADB or wireless
debugging pairing. The shell user holds `MANAGE_USB` permission (granted in AOSP's
`packages/Shell/AndroidManifest.xml`). Your app wraps Binder calls with
`ShizukuBinderWrapper`, which re-dispatches them from the privileged server process.
The system server sees an authorized caller and allows the call.

### What Shizuku unlocks

```kotlin
val usbBinder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("usb"))
val iUsbManager = IUsbManager.Stub.asInterface(usbBinder)
val ports: List<UsbPort> = iUsbManager.getPorts()

for (port in ports) {
    val status: UsbPortStatus = port.status
    // All of these now accessible:
    status.isConnected              // plug state
    status.currentMode              // host/device/dual-role
    status.currentPowerRole         // source/sink
    status.currentDataRole          // host/device
    status.plugState                // orientation (CC1/CC2 = normal/flipped)
    status.displayPortAltModeInfo   // DP alt-mode details (API 34+)
    status.isPdCompliant            // PD compliance (API 35+)
    status.complianceWarnings       // bad cable warnings
    status.usbDataStatus            // enabled/disabled + reasons
}
```

### What you get vs. each tier

| Feature | Tier 1 (Public) | Tier 1.5 (Shizuku) | Tier 2 (Root) |
|---|---|---|---|
| Device enumeration | Yes | Yes | Yes |
| BOS descriptors | Yes | Yes | Yes |
| Billboard parsing | Yes | Yes | Yes |
| Charging current/voltage | Yes | Yes | Yes |
| Cable orientation | No | **Yes** | Yes |
| Power/data role | No | **Yes** | Yes |
| PD compliance | No | **Yes** (API 35+) | Yes |
| DisplayPort alt-mode info | No | **Yes** (API 34+) | Yes |
| Compliance warnings | No | **Yes** | Yes |
| E-marker cable identity | No | No | Yes |
| Full sysfs typec tree | No | No | Yes |

### User setup

- Install Shizuku from Play Store
- Android 13+: enable Wireless debugging, pair once via notification shade. Shizuku
  auto-starts on boot if trusted Wi-Fi is saved
- Android 11-12: pair once, tap "Start" in Shizuku after reboot
- Rooted devices: fully automatic, no user action

### Build requirements

- Add Shizuku API dependency (`dev.rikka.shizuku:api` + `dev.rikka.shizuku:provider`)
- Include `IUsbManager.aidl` stub (from hidden-api-stub or compile against system SDK)
- Add `SystemServiceHelper` for service lookup
- Declare Shizuku provider in manifest

### Play Store safety

Shizuku itself is on the Play Store. Many apps using it are published without issue
(KeyMapper, RootlessJamesDSP, MacroDroid). Rules:
- Do NOT declare MANAGE_USB in your manifest (you use Shizuku's identity, not your own)
- Make Shizuku optional with graceful fallback
- Frame as "advanced diagnostics for developers"

### Updated project structure (additions)

```
data/
  shizuku/
    ShizukuUsbPortReader.kt    # UsbPort/UsbPortStatus via Shizuku binder
    ShizukuSetupHelper.kt      # Availability check, permission request flow
    IUsbManagerCompat.kt       # AIDL stub wrapper with version checks
```

---

## Revised Three-Tier Model

The app now has three access levels, detected and surfaced automatically:

```
┌─────────────────────────────────────────────────────────┐
│  Tier 2: Root                                           │
│  Full sysfs access. E-marker identity, PDOs, alt-modes. │
├─────────────────────────────────────────────────────────┤
│  Tier 1.5: Shizuku                                      │
│  UsbPortStatus via system API. Orientation, PD,         │
│  compliance, DisplayPort alt-mode info.                 │
├─────────────────────────────────────────────────────────┤
│  Tier 1: Public API                                     │
│  USB Host device enumeration, BOS, Billboard,           │
│  BatteryManager charging data.                          │
└─────────────────────────────────────────────────────────┘
```

The UI shows a capability indicator: "Basic / Enhanced (Shizuku) / Full (Root)" and
prompts users to unlock the next tier if relevant features would benefit them.

---

## Open Questions

1. **Companion hardware?** A USB-C inline adapter with a microcontroller could read PD
   messages and relay them over BLE/USB-serial to the phone. This would unlock full PD
   diagnostics without any privilege escalation. Hardware cost vs. software-only tradeoff.

2. **ADB-over-USB mode?** When the phone is connected to a computer, could a desktop
   companion app relay the computer's USB data to the phone app? Niche but interesting.

3. **OEM partnerships?** Samsung, Google, OnePlus all have engineering modes that expose
   more USB data. Could partner for "certified device" deep diagnostics.

4. **Shizuku + sysfs?** Shizuku's shell UID might also have read access to some
   `/sys/class/typec/` nodes that regular apps cannot read (SELinux context for shell
   is more permissive than untrusted_app). Could blur the line between Tier 1.5 and
   Tier 2 on some devices. Needs testing on real hardware.
