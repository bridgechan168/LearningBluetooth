package com.bridgechan.learningbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,ListView.OnItemClickListener{
    private Button btnOn, btnOff, btnShow, btnDiscover, btnForward, btnLeft, btnBack, btnRight;
    private ListView listviewShow, listviewDiscover;
    private ConstraintLayout layoutCarControl;
    private BluetoothAdapter adapBluetooth;
    private final static int REQUEST_ENABLE_BT = 0;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> adapPairedDevices, adapDiscoveredDevices;
    private ArrayList<BluetoothDevice> arrDiscoveredDevices, arrPairedDevices;
    private Handler mHandler;
    private ConnectThread connectedBT;
    private final static int MESSAGE_READ = 1;
    private final static int MESSAGE_WRITE = 2;
    private final static int MESSAGE_CONNECTED = 3;
    private final static int MESSAGE_STOP = 4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOn = findViewById(R.id.btnOn);
        btnOff = findViewById(R.id.btnOff);
        btnShow = findViewById(R.id.btnShow);
        btnDiscover = findViewById(R.id.btnDiscover);
        listviewShow = findViewById(R.id.listviewPared);
        listviewDiscover = findViewById(R.id.listviewDiscover);
        layoutCarControl = findViewById(R.id.layout_car_control);
        btnForward = findViewById(R.id.btnForward);
        btnBack = findViewById(R.id.btnBack);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);

        btnOn.setOnClickListener(this);
        btnOff.setOnClickListener(this);
        btnShow.setOnClickListener(this);
        btnDiscover.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnLeft.setOnClickListener(this);
        btnRight.setOnClickListener(this);

        adapBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(adapBluetooth == null){
           showToastMessage("Bluetooth is not supported");
        }
        adapPairedDevices = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1);
        listviewShow.setAdapter(adapPairedDevices);
        listviewShow.setOnItemClickListener(this);

        adapDiscoveredDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        listviewDiscover.setAdapter(adapDiscoveredDevices);
        listviewDiscover.setOnItemClickListener(this);

        arrDiscoveredDevices = new ArrayList<BluetoothDevice>();
        arrPairedDevices = new ArrayList<BluetoothDevice>();

        createHandler();
    }

    private void createHandler(){
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_CONNECTED){
                    showToastMessage("connected");
                    connectedBT = (ConnectThread)msg.obj;
                    setButtons(true);

                }else if(msg.what == MESSAGE_STOP){
                    if(connectedBT != null){
                        connectedBT.cancel();
                        connectedBT = null;
                    }
                    setButtons(false);
                }
            }
        };
    }
    private void setButtons(Boolean flag){
        if(flag){
            btnRight.setVisibility(View.VISIBLE);
            btnForward.setVisibility(View.VISIBLE);
            btnLeft.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
            layoutCarControl.setVisibility(View.VISIBLE);
        }else{
            btnRight.setVisibility(View.GONE);
            btnForward.setVisibility(View.GONE);
            btnLeft.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
            layoutCarControl.setVisibility(View.GONE);
        }
    }
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnOn:
                turnOn();
                break;
            case R.id.btnOff:
                turnOff();
                break;
            case R.id.btnShow:
                listviewDiscover.setVisibility(View.GONE);
                listviewShow.setVisibility(View.VISIBLE);
                showPaired();
                break;
            case R.id.btnDiscover:
                listviewDiscover.setVisibility(View.VISIBLE);
                listviewShow.setVisibility(View.GONE);
                discover();
                break;
            case R.id.btnForward:
                sendData2BT("Forward");
                break;
            case R.id.btnBack:
                sendData2BT("Back");
                break;
            case R.id.btnLeft:
                sendData2BT("Left");
                break;
            case R.id.btnRight:
                sendData2BT("Right");
                break;
        }
    }
    private void showToastMessage(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }
    private void sendData2BT(String data){
        connectedBT.sendData(data);
    }
    private void turnOn(){
        if(!adapBluetooth.isEnabled()){
            Intent intentEnable  = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentEnable,REQUEST_ENABLE_BT);
        }
    }
    private void turnOff(){
        if(adapBluetooth != null && adapBluetooth.isEnabled()){
            if(connectedBT != null){
                connectedBT.cancel();
                connectedBT = null;
            }

            adapBluetooth.disable();
            showToastMessage("Off successfully");
        }
    }
    private void showPaired(){
        if(!adapBluetooth.isEnabled()){
            showToastMessage("Bluetooth is not on");
            return ;
        }
        adapBluetooth.cancelDiscovery();
        pairedDevices = adapBluetooth.getBondedDevices();
        adapPairedDevices.clear();
        arrPairedDevices.clear();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                String name = device.getName();
                String address = device.getAddress();
                adapPairedDevices.add(name + "\n" + address);
                arrPairedDevices.add(device);
            }
        }
        listviewShow.deferNotifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiverBT);
        turnOff();
    }

    private void discover(){
        if(adapBluetooth.isDiscovering()){
            adapBluetooth.cancelDiscovery();
            showToastMessage("Discovery stopped");
            adapDiscoveredDevices.clear();
            arrDiscoveredDevices.clear();
            listviewDiscover.deferNotifyDataSetChanged();
        }else{
            if(adapBluetooth.isEnabled()){
                adapPairedDevices.clear();
                arrDiscoveredDevices.clear();
                adapBluetooth.startDiscovery();
                showToastMessage("Discvoery started");
                IntentFilter filters = new IntentFilter();
                filters.addAction(BluetoothDevice.ACTION_FOUND);
                filters.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                registerReceiver(receiverBT,filters);
            }
        }
    }
    final BroadcastReceiver receiverBT = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                adapDiscoveredDevices.add(device.getName() + "\n" + device.getAddress());
                arrDiscoveredDevices.add(device);
                listviewDiscover.deferNotifyDataSetChanged();
            }else if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.setPin("1234".getBytes());
                //refresh
                showPaired();
            }
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                showToastMessage("Bluetooth on successfully");
            }else{
                showToastMessage("Cant turn on bluetooth");
            }

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch(view.getId()){
            case R.id.listviewDiscover:
                pairDevice(position);
                break;
            case R.id.listviewPared:
                connectDevice(position);
                break;
        }
    }
    private void pairDevice(int i){
        if(arrDiscoveredDevices.size() > 0){
            BluetoothDevice device = arrDiscoveredDevices.get(i);
            device.createBond();
        }
    }
    private void connectDevice(int i){
        if(!adapBluetooth.isEnabled()) return ;
        if(arrPairedDevices.size() == 0) return ;
        ConnectThread conn = new ConnectThread(arrPairedDevices.get(i));
        conn.start();
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;
        private BluetoothDevice device;
        private UUID myUUID;
        private ConnectedThread connected;

        public ConnectThread(BluetoothDevice device){
            mmSocket = null;
            connected = null;
            this.device = device;
            myUUID = UUID.fromString("00001100-0000-1000-8000-00805F9B34FB");
            try{
                mmSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }catch(IOException e){
                Log.e("tester","socket creation failed",e);
            }
        }
        @Override
        public void run() {
            adapBluetooth.cancelDiscovery();
            try{
                mmSocket.connect();
                Message msg = mHandler.obtainMessage(MESSAGE_CONNECTED,1,1,this);
                msg.sendToTarget();
            }catch(IOException e){
                try{
                    mmSocket.close();
                }catch(IOException eClose){
                    Log.e("tester","could not close client socket",e);
                }
                return ;
            }
            connected = new ConnectedThread(mmSocket);
            connected.start();

        }
        public BluetoothSocket getSocket(){
            return mmSocket;
        }
        public void sendData(String data){
            if(mmSocket.isConnected()){
                connected.write(data);
            }
        }
        public boolean isConnected(){
            return mmSocket.isConnected();
        }
        public void cancel(){
            try{
                mmSocket.close();
                connected = null;
            }catch(IOException e){
                Log.e("tester","cloud not close connect socket",e);
            }
            Message msg = mHandler.obtainMessage(MESSAGE_STOP,1,1);
            msg.sendToTarget();
        }

    }

    private class ConnectedThread extends Thread{
        private BluetoothSocket mmSocket;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            mmOutStream = null;

            try{
                mmOutStream = socket.getOutputStream();
            }catch(IOException e){
                Log.e("tester","error occurred when creating output stream",e);
            }

        }

        @Override
        public void run() {

        }
        public void write(String data){
            String result = "";
            Message msg;
            try{
                mmOutStream.write(data.getBytes());
                result = "Write ok";
                msg = mHandler.obtainMessage(MESSAGE_WRITE,1,1,result);
            }catch(IOException e){
                Log.e("tester","error occurred when sending data",e);
                result = "Write failed:" + e.getMessage();
                msg = mHandler.obtainMessage(MESSAGE_WRITE,0,0,result);
            }
            msg.sendToTarget();
        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){
                Log.e("tester","cloud not close connect socket",e);
            }
        }
    }

}
