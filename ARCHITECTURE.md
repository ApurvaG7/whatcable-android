# WhatCable Android - Architecture

## Overview
WhatCable is a no-root Android app for charging diagnostics. It surfaces live
charging power, the negotiated charging ceiling, and a comparative cable tester,
plus USB-host descriptor inspection for any device plugged into the phone. It
runs entirely on public APIs so it can ship on Google Play with no special
permissions.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **DI**: Hilt
- **Database**: Room (charging history + saved cable tests)
- **Async**: Coroutines + Flow
- **Min SDK**: 26 (Android 8.0), Target/Compile SDK: 36

## What is and isn't readable without root
A normal app reads battery-side current (`BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`)
and the negotiated charging ceiling from the `ACTION_BATTERY_CHANGED` broadcast
(`max_charging_voltage` / `max_charging_current`, read by string key as the
framework constants are `@hide`). It cannot read input-side wattage, the charger
type string, the USB-C port role, or the cable e-marker: those sysfs nodes are
gated by SELinux (`sysfs_batteryinfo`) to the shell domain, not untrusted apps.
So "is this cable bad?" is answered by outcome (the negotiated voltage, and an
A/B comparison of measured watts), never by reading the cable directly.

## Module Structure

### `data/` - Data Layer

#### `data/usb/` - USB host (public API)
- **UsbHostScanner**: enumerates connected USB devices via `UsbManager`, observes
  attach/detach events.
- **BosReader**: reads Binary Object Store (BOS) descriptors via control transfers.
- **BillboardParser** / **DescriptorParser**: parse raw USB descriptors into
  structured data.

#### `data/charging/` - Charging monitor
- **ChargingMonitor**: observes `ACTION_BATTERY_CHANGED` and reads BatteryManager;
  exposes charging state (current, voltage, wattage, temperature) and the
  negotiated ceiling, with a `chargerClass` inference (basic 5V vs fast charger).

#### `data/db/` - Persistence (Room)
- **ChargingSample** / **ChargingSampleDao**: rolling charging history.
- **CableTest** / **CableTestDao**: saved comparative cable-test results.

### `domain/` - Business Logic
- **CableDiagnosticEngine**: orchestrates USB descriptors + battery state into a
  `CableSnapshot`.
- **CableTestRecorder**: time-bounded charging-power capture (polls the sensor on
  a 1s timer); computes peak and a spike-resistant sustained rate.
- **SpeedClassifier**: cable speed tier from descriptors.
- **ChargingAnalyser**: charging capability estimate from device descriptors.
- **TrustScorer**: cable quality score from descriptor signals.
- **CableReportGenerator**: plain-text shareable report.

### `ui/` - Presentation (Compose, MVVM)
- **HomeScreen**: dashboard (charging card, compare-cables entry, USB devices).
- **ChargingScreen**: live charging stats and history.
- **CableTestScreen**: guided capture, result review, ranked saved cables.
- **DeviceDetailScreen**: detailed USB device descriptor view.

## Key Design Decisions
- **No root, public APIs only** so it is Play-store shippable.
- **Offline and private**: all diagnostics run locally, no network calls.
- **Honest framing**: the app measures what arrives and how high the contract
  negotiated; it does not claim to read a cable's spec.
