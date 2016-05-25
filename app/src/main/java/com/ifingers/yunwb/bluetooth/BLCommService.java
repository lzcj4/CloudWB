package com.ifingers.yunwb.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BLCommService {
    private String mSearchPattern = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mSocket = null;
    private ConnectedThread mConnectedThread;
    private TouchScreen mTouchScreen = null;
    private JYDZ_Comm_Protocol mProtocol = null;
    private IrmtInterface mIrTouchInterface = null;
    private Activity mParentActivity = null;

    public BLCommService(Activity activity, IrmtInterface newInterface) {
        mParentActivity = activity;
        mIrTouchInterface = newInterface;

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void enable() {
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mParentActivity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    public void connect(String searchPattern) {
        if (mBtAdapter.isEnabled()) {
            mTouchScreen = new TouchScreen();
            mProtocol = new JYDZ_Comm_Protocol(mTouchScreen, mIrTouchInterface);

            mSearchPattern = searchPattern;
            // Register for broadcasts when a device is discovered
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mParentActivity.registerReceiver(mReceiver, filter);
            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mParentActivity.registerReceiver(mReceiver, filter);
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
            mBtAdapter.startDiscovery();
        } else {
            mIrTouchInterface.onError(BL_ERROR_NOT_ENABLE);
        }
    }

    public void disconnect() {
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            mDevice = null;
            //TODO mProtocol.resetProtocol();
        } catch (IOException e) {

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean alive = true;
        private BluetoothSocket mmSocket = BLCommService.this.mSocket;

        public ConnectedThread() {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (alive) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    switch (mProtocol.handlerIncomeData(bytes, buffer)) {
                        case JYDZ_Comm_Protocol.COMM_STATUS_CHANGE_FORMAT:
                            byte[] CmdChangeFeatrue = mProtocol.ChangeDataFeatrue();
                            if (CmdChangeFeatrue != null) {
                                write(CmdChangeFeatrue);
                            }
                            break;
                        case JYDZ_Comm_Protocol.COMM_STATUS_DATA_GET_OK:
                            break;
                        case JYDZ_Comm_Protocol.COMM_STATUS_GESTURE_GET:
                            mIrTouchInterface.onGestureGet(mProtocol.dataBuffer[0]);
                            break;
                        case JYDZ_Comm_Protocol.COMM_STATUS_SNAPSHOT_GET:
                            mIrTouchInterface.onSnapshot(mProtocol.dataBuffer[0]);
                            break;
                        case JYDZ_Comm_Protocol.COMM_STATUS_IDENTI_GET:
                            mIrTouchInterface.onIdGet(mProtocol.dataBuffer[0] + mProtocol.dataBuffer[1] * 256 + mProtocol.dataBuffer[2] * 256 * 256 + mProtocol.dataBuffer[3] * 256 * 256 * 256);
                            break;
                        case JYDZ_Comm_Protocol.COMM_STATUS_SCREENFEATURE_GET:
                            mTouchScreen.setIrTouchFeature(mProtocol.dataBuffer);
                            break;

                    }
                } catch (IOException e) {
                    mIrTouchInterface.onError(BL_ERROR_CONN_LOST);
                    alive = false;
                }
            }

            BLCommService.this.disconnect();
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                alive = false;
                if (mmInStream != null) {
                    mmInStream.close();
                    mmInStream = null;
                }

                if (mmOutStream != null) {
                    mmOutStream.close();
                    mmOutStream = null;
                }

            } catch (IOException e) {
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String addr = device.getAddress();
                if ((name != null && name.equals(mSearchPattern)) || (addr != null && addr.equalsIgnoreCase(mSearchPattern))) {
                    //found the device, cancel the discovery at once
                    mBtAdapter.cancelDiscovery();
                    mParentActivity.unregisterReceiver(mReceiver);
                    mDevice = device;
                    //get socket
                    try {
                        mSocket = device.createRfcommSocketToServiceRecord(MY_UUID_SPP);
                        mSocket.connect();
                        mIrTouchInterface.onBLconnected();
                        mConnectedThread = new ConnectedThread();
                        mConnectedThread.start();
                    } catch (Exception ex) {
                        mIrTouchInterface.onError(BL_ERROR_CONN_FAILED);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mDevice == null) {
                    mIrTouchInterface.onError(BL_ERROR_DEV_NOT_FOUND);
                }
            }
        }
    };
    // Debugging
    private static final String TAG = "BLService";
    public static final int REQUEST_ENABLE_BT = 3;

    public static final int BL_ERROR_CONN_FAILED = 1;
    public static final int BL_ERROR_CONN_LOST = 2;
    public static final int BL_ERROR_DEV_NOT_FOUND = 3;
    public static final int BL_ERROR_NOT_ENABLE = 4;
    // Unique UUID for this application
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    //SerialPortServiceClass_UUID = '{00001101-0000-1000-8000-00805F9B34FB}'
    private static final UUID MY_UUID_SPP = UUID.fromString(SPP_UUID);
}

