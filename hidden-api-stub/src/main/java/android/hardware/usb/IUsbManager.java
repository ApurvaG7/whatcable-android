package android.hardware.usb;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IUsbManager extends IInterface {

    List<ParcelableUsbPort> getPorts() throws RemoteException;

    UsbPortStatus getPortStatus(String portId) throws RemoteException;

    abstract class Stub extends Binder implements IUsbManager {
        public static IUsbManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IBinder asBinder() {
            throw new UnsupportedOperationException();
        }
    }
}
