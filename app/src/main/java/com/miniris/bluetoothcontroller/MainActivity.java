package com.miniris.bluetoothcontroller;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    private ArrayAdapter<String> messageAdapter;
    private BluetoothService mBluetoothService;
    private Boolean isGettingData = false;
    private byte[] imageData;
    private int expectDataLength;
    private int currentPosition = 0;

    public void displayToast(int stringId) {
        Toast mToast = Toast.makeText(this,stringId, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public void display_message(String message) {
        messageAdapter.add(message);
    }


    public void send_message(String message) {
        byte[] buff = message.getBytes();
        mBluetoothService.write(buff);
    }

    // on send button click
    public void sendMessage(View view) {
        switch (view.getId()) {
            case R.id.send_button:
                EditText message = (EditText) findViewById(R.id.message_edit);
                send_message(message.getText().toString());
                message.setText("");
                break;
            case R.id.led_yellow_on_button:
                send_message("LED11");
                break;
            case R.id.led_red_on_button:
                send_message("LED21");
                break;
            case R.id.led_blue_on_button:
                send_message("LED31");
                break;
            case R.id.led_yellow_off_button:
                send_message("LED10");
                break;
            case R.id.led_red_off_button:
                send_message("LED20");
                break;
            case R.id.led_blue_off_button:
                send_message("LED30");
                break;
            case R.id.exit_button:
                send_message("exit");
                finish();
        }

    }

    public void startBluetoothServer() {
        try {
            mBluetoothService = new BluetoothService(mHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            displayToast(R.string.unsupport_bluetooth);
            finish();
        }else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, Common.REQUEST_BLUETOOTH_ENABLE);
        } else {
            startBluetoothServer();
        }

        // Set up View
        ListView messageList = (ListView) findViewById(R.id.message_listView);
        messageAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        messageList.setAdapter(messageAdapter);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case Common.REQUEST_BLUETOOTH_ENABLE:
                if (resultCode == RESULT_CANCELED){
                    displayToast(R.string.disable_bluetooth);
                    finish();
                } else {
                    startBluetoothServer();
                }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    public static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Common.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    if (isGettingData) {
                        if (expectDataLength < currentPosition + readBuf.length)
                        {
                            int restSize = expectDataLength - currentPosition;
                            byte[] tmp = new byte[restSize];
                            System.arraycopy(readBuf, 0, tmp, 0, restSize);
                            readBuf = tmp;
                        }
                        System.arraycopy(readBuf, 0, imageData, currentPosition, readBuf.length-1);
                       currentPosition += readBuf.length;

                        if (currentPosition >= expectDataLength) {
                            display_message("Image Download is done.");
                            display_image(imageData);
                            display_message("" + imageData.length);
                            isGettingData = false;
                            expectDataLength = 0;
                            currentPosition = 0;
                            imageData = null;
                        }
                    } else {
                        //String readMessage = new String(readBuf, 0, msg.arg1);
                        String cmd = new String(readBuf, 0, 3);
                        display_message(cmd);

                        switch (cmd) {
                            case "IMG":
                                expectDataLength = Integer.parseInt(new String(readBuf, 3, 10));
                                display_message("Image size : " + expectDataLength);
                                imageData = new byte[expectDataLength];
                                System.arraycopy(readBuf, 13, imageData, 0, readBuf.length - 13);
                                isGettingData = true;
                                break;
                            default:
                                display_message("<= " + readBuf.toString());
                                break;
                        }
                    }
                    break;
                case Common.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    display_message("=> " + writeMessage);
                    break;
            }
        }
    };

    private void display_image(byte[] imageData) {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Bitmap bmpImage = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        imageView.setImageBitmap(bmpImage);
    }
}
