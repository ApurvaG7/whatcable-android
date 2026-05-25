package com.whatcable.android.core.model

enum class CapabilityTier {
    BASIC,
    ENHANCED,
    FULL;

    val label: String
        get() = when (this) {
            BASIC -> "Basic"
            ENHANCED -> "Enhanced (Shizuku)"
            FULL -> "Full (Root)"
        }
}
