package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public final class ParcelableUsbPort implements Parcelable {

    public String getId() {
        throw new UnsupportedOperationException();
    }

    public int getSupportedModes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int describeContents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new UnsupportedOperationException();
    }

    public static final Creator<ParcelableUsbPort> CREATOR = null;
}
