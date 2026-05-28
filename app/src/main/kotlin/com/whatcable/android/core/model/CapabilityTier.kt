package com.whatcable.android.core.model

enum class CapabilityTier {
    BASIC,
    FULL;

    val label: String
        get() = when (this) {
            BASIC -> "Basic"
            FULL -> "Full (Root)"
        }
}
