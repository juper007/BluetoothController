package com.miniris.bluetoothcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by v-mipark on 2/9/2015.
 */
public class BluetoothService {
    private BluetoothServerSocket mServerSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private AcceptSocketThread mAcceptSocket;
    private CommunicationThread mCommunicationThread;
    private Handler mHandler;

    public BluetoothService(Handler handler) throws IOException {
        mHandler = handler;
        mAcceptSocket = new AcceptSocketThread();
        mAcceptSocket.start();
    }

    public void manageConnectedSocket(BluetoothSocket socket) {
        mCommunicationThread = new CommunicationThread(socket);
        mCommunicationThread.start();
    }

    public void write(byte[] buff) {
        mCommunicationThread.write(buff);
    }

    public void stop() {
        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            mCommunicationThread = null;
        }

        if (mAcceptSocket != null) {
            mAcceptSocket.cancel();
            mAcceptSocket = null;
        }
    }

    private class AcceptSocketThread extends Thread {
        public AcceptSocketThread() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Common.CONTROLLER_NAME, Common.MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (socket != null) {
                    try {
                        manageConnectedSocket(socket);
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class CommunicationThread extends Thread {
        private OutputStream outStream;
        private InputStream inStream;
        private BluetoothSocket mmSocket;

        public CommunicationThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmSocket = socket;

            try {
                tmpOut = socket.getOutputStream();
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            outStream = tmpOut;
            inStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);

                    mHandler.obtainMessage(Common.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Common.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
