---
name: project-android-port
description: WhatCable Android port - three-tier architecture (Public/Shizuku/Root) for USB-C cable diagnostics
metadata:
  type: project
---

WhatCable Android port is greenfield, architecture approved 2026-05-25.

Three-tier data access model:
- Tier 1 (Public API): USB Host device enumeration, BOS descriptors, Billboard parsing, BatteryManager charging
- Tier 1.5 (Shizuku): UsbPortStatus system APIs via shell-privileged Binder. Orientation, PD compliance, DP alt-mode info, compliance warnings. No root needed.
- Tier 2 (Root): /sys/class/typec/ sysfs for e-marker identity, cable speed, active/passive, partner alt-modes.

Stack: Kotlin, Jetpack Compose, Hilt, Room. Min SDK 26, target 35.

Shizuku is the key differentiator vs other Android USB apps. Shell UID holds MANAGE_USB; ShizukuBinderWrapper re-dispatches calls under that identity.

**Why:** No existing Android app offers cable diagnostics beyond basic device listing. The @SystemApi wall is why. Shizuku sidesteps it without root.

**How to apply:** Architecture doc lives at ARCHITECTURE.md in the project root. When implementation begins, follow the three-tier pattern and graceful capability detection.
