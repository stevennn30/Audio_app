package com.serafimtech.serafimaudio.Service;

import static com.serafimtech.serafimaudio.Service.ServiceAttribute.ACTION_GATT_DISCONNECTED;
import static com.serafimtech.serafimaudio.Service.ServiceAttribute.EXTRA_DATA;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AudioSeriesService extends Service {
    //<editor-fold desc="<Variable>">
    private final String TAG = AudioSeriesService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();

    public BluetoothDevice device;
    public BluetoothGatt mBluetoothGatt;
    public BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;

    public HashMap<String, Integer> ScanAddress = new HashMap<>();
    public String connectedDeviceAddress = "";
    public String connectedDeviceName = "";

    private final byte audioMode = 0x30;

    public int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    public int interval = 100;

    public enum EQDataMode {
        Bank0,
        Bank1,
        Single,
        Update,
    }

    public volatile boolean mBusy = false; // Write/read pending response
    Handler mHandler;

    private NotificationManager notificationManager = null;
    //</editor-fold>

    //<editor-fold desc="<Broadcast>">

    public void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
        mBusy = false;
        interval = 100;
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(ServiceAttribute.EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(ServiceAttribute.EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
        interval = 100;
    }
    //</editor-fold>

    //<editor-fold desc="<LifeCycle>">
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d(TAG, "onCreate");
//    }

    @Override
    public void  onCreate() {
        super.onCreate();
//        INSTANCE = this
//        Log.i(TAG, "RTP Display service create")
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channelId", "channelId", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        keepAliveTrick();
    }

    private void keepAliveTrick() {
        Notification notification = new NotificationCompat.Builder(this, "channelId")
                .setSilent(true)
                .setOngoing(false)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        Log.d(TAG, "Thread interrupted:" + Thread.interrupted());
        broadcastUpdate(ACTION_GATT_DISCONNECTED);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        Log.i(TAG, "initialize");
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public class LocalBinder extends Binder {
        public AudioSeriesService getService() {
            return AudioSeriesService.this;
        }
    }
    //</editor-fold>

    //<editor-fold desc="<Initialize>">

    //</editor-fold>

    //<editor-fold desc="<BluetoothGatt>">
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothAdapter.isEnabled()) {
                    Log.i(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());
                    interval = 100;
                    intentAction = ServiceAttribute.ACTION_GATT_CONNECTED;
                    connectedDeviceAddress = gatt.getDevice().getAddress();
                    connectedDeviceName = gatt.getDevice().getName();
                    broadcastUpdate(intentAction);
                    mConnectionState = BluetoothProfile.STATE_CONNECTED;
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered");
                setCustomCharacteristicNotify(true);
                broadcastUpdate(ServiceAttribute.ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            final byte[] data = characteristic.getValue();
            Log.d(TAG, "Write data        " + byteToString(data).toString());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            //TODO FwUpdate Check
            Log.d(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ServiceAttribute.ACTION_DATA_READ, characteristic, status);
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.d(TAG, "Read data" + byteToString(data).toString());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.i(TAG, "onCharacteristicChanged");
            broadcastUpdate(ServiceAttribute.ACTION_DATA_NOTIFY, characteristic,
                    BluetoothGatt.GATT_SUCCESS);
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                Log.d(TAG, "Notification data " + byteToString(data).toString());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            interval = 100;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
            mBusy = false;
            interval = 100;
        }
    };

    private boolean checkGatt() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        if (mBusy) {
            Log.w(TAG, "LeService busy");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        this.device = device;
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = BluetoothProfile.STATE_CONNECTING;

        return true;
    }

    public void disconnect() {
        Log.d(TAG, "disconnect gatt");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        connectedDeviceAddress = "";
        connectedDeviceName = "";
        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

        broadcastUpdate(ACTION_GATT_DISCONNECTED);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    //</editor-fold>

    //<editor-fold desc="<Scan>">
    public void startLEScan() {
        Log.d(TAG, "startLEScan");
        try {//得到连接状态的方法
            ScanAddress.clear();
            final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            mBluetoothAdapter.getBluetoothLeScanner().startScan(null, settings, scanCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLEScan() {
        Log.d(TAG, "stopLEScan");
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
    }

    final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null) {
                Log.d(TAG, "device name:" + result.getDevice().getName());
                if (result.getDevice().getName().contains("Serafim A1")) {
                    ScanAddress.put(result.getDevice().getAddress(), result.getRssi());
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
    //</editor-fold>

    //<editor-fold desc="<OpenBLENotify>">
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (!checkGatt()) {
            return false;
        }

        boolean ok = false;
        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(ServiceAttribute.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {
                if (enable) {
                    Log.i(TAG, "Enable notification: " +
                            characteristic.getUuid().toString());
                    ok = clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    Log.i(TAG, "Disable notification: " +
                            characteristic.getUuid().toString());
                    ok = clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                if (ok) {
                    mBusy = true;
                    ok = mBluetoothGatt.writeDescriptor(clientConfig);
                    Log.i(TAG, "writeDescriptor: " +
                            characteristic.getUuid().toString());
                }
            }
        }
        return ok;
    }

    public void setCustomCharacteristicNotify(boolean mode) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString("0000FFA0-0000-1000-8000-00805f9b34fb"));
        if (mCustomService == null) {
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic msetCharNotify = mCustomService.getCharacteristic(UUID.fromString("0000FFA3-0000-1000-8000-00805f9b34fb"));

        while (!setCharacteristicNotification(msetCharNotify, mode)) ; //True/False
    }
    //</editor-fold>

    //<editor-fold desc="<AudioProtocol>">
    public boolean writeCharacteristic(byte[] data) {
        if (mConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
            if (!waitIdle(1000)) {
                disconnect();
                return false;
            }
            //check mBluetoothGatt is available
            if (mBluetoothGatt == null) {
                Log.e(TAG, "lost connection");
                return false;
            }
            BluetoothGattService Service = mBluetoothGatt.getService(UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb"));
            if (Service == null) {
                Log.e(TAG, "write service not found!");
                return false;
            }
            BluetoothGattCharacteristic charac = Service.getCharacteristic(UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb"));
            if (charac == null) {
                Log.e(TAG, "char not found!");
                return false;
            }
            charac.setValue(data);
            charac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBusy = true;
            return mBluetoothGatt.writeCharacteristic(charac);
        } else {
            return false;
        }
    }

    public boolean ReadSetting(byte state) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x02;
        data[2] = 0x02;
        data[3] = 0x01;
        data[4] = state;
        return writeCharacteristic(data);
    }

    public boolean ReadBatteryState() {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x07;
        data[2] = 0x02;
        data[3] = 0x00;
        return writeCharacteristic(data);

    }

    public boolean ControlIndependentVolume(byte Vol) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x03;
        data[2] = 0x02;
        data[3] = 0x03;
        data[4] = 0x05;
        data[5] = Vol;
        return writeCharacteristic(data);
    }

    public boolean EnabledEQ(boolean enabled) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x04;
        data[2] = 0x02;
        data[3] = 0x01;
        data[4] = enabled ? 0x01 : (byte) 0x00;
        return writeCharacteristic(data);
    }

    public boolean WriteEQConfig(EQDataMode mode, byte[] configData) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x04;
        data[2] = 0x02;
        data[3] = (byte) (configData.length + 2);
        data[4] = 0x03;
        switch (mode) {
            case Bank0:
                data[5] = 0x00;
                System.arraycopy(configData, 0, data, 6, configData.length);
                break;
            case Bank1:
                data[5] = 0x01;
                System.arraycopy(configData, 0, data, 6, configData.length);
                break;
            case Single:
                data[5] = 0x02;
                System.arraycopy(configData, 0, data, 6, configData.length);
                break;
            case Update:
                data[3] = 0x02;
                data[5] = 0x03;
                break;
        }
        return writeCharacteristic(data);
    }

    public boolean ReadEQConfig(EQDataMode mode) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x04;
        data[2] = 0x02;
        data[3] = 0x02;
        data[4] = 0x02;
        switch (mode) {
            case Bank0:
                data[5] = 0x04;
                break;
            case Bank1:
                data[5] = 0x05;
                break;
        }
        return writeCharacteristic(data);
    }

    public boolean Enabled3DEffect(boolean enabled) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x05;
        data[2] = 0x02;
        data[3] = 0x01;
        data[4] = enabled ? (byte) 0x01 : (byte) 0x00;
        return writeCharacteristic(data);
    }

    public boolean Write3DConfig(byte[] configData) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x05;
        data[2] = 0x02;
        data[3] = 0x04;
        data[4] = 0x04;
        System.arraycopy(configData, 0, data, 5, configData.length);
        return writeCharacteristic(data);
    }

    public boolean Read3DConfig() {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x05;
        data[2] = 0x02;
        data[3] = 0x02;
        data[4] = 0x03;
        return writeCharacteristic(data);
    }

    public boolean EnabledBassBoost(boolean enabled) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x06;
        data[2] = 0x02;
        data[3] = 0x01;
        data[4] = enabled ? (byte) 0x01 : (byte) 0x00;
        return writeCharacteristic(data);
    }

    public boolean WriteBassBoostConfig(byte config) {
        byte[] data = new byte[16];
        data[0] = audioMode;
        data[1] = 0x06;
        data[2] = 0x02;
        data[3] = 0x02;
        data[4] = 0x04;
        data[5] = config;
        return writeCharacteristic(data);
    }
    //</editor-fold>

    public boolean waitIdle(int timeout) {
        while (--timeout > 0) {
            if (mBusy)
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return timeout > 0;
    }

    public static StringBuilder byteToString(byte[] data) {
        StringBuilder str = new StringBuilder();
        str.append("0x");
        for (byte datum : data) str.append(String.format("%02X", datum));
        return str;
    }
}