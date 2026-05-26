package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public final class UsbPortStatus implements Parcelable {

    // API 26+
    public boolean isConnected() { throw new UnsupportedOperationException(); }
    public int getCurrentMode() { throw new UnsupportedOperationException(); }
    public int getCurrentPowerRole() { throw new UnsupportedOperationException(); }
    public int getCurrentDataRole() { throw new UnsupportedOperationException(); }
    public int getSupportedRoleCombinations() { throw new UnsupportedOperationException(); }

    // API 29+
    public int getContaminantDetectionStatus() { throw new UnsupportedOperationException(); }
    public int getContaminantProtectionStatus() { throw new UnsupportedOperationException(); }

    // API 31+
    public int getUsbDataStatus() { throw new UnsupportedOperationException(); }
    public boolean isPowerTransferLimited() { throw new UnsupportedOperationException(); }
    public int getPowerBrickConnectionStatus() { throw new UnsupportedOperationException(); }

    // API 34+
    public int[] getComplianceWarnings() { throw new UnsupportedOperationException(); }
    public int getPlugState() { throw new UnsupportedOperationException(); }

    @Override
    public int describeContents() { throw new UnsupportedOperationException(); }

    @Override
    public void writeToParcel(Parcel dest, int flags) { throw new UnsupportedOperationException(); }

    public static final Creator<UsbPortStatus> CREATOR = null;
}
