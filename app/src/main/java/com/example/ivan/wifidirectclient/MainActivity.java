package com.example.ivan.wifidirectclient;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements WifiP2pManager.PeerListListener{

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    List<String> peerNames = new ArrayList<String>();
    int peerSelected=0;

    WifiP2pConfig config = new WifiP2pConfig();

    IntentFilter mIntentFilter;

    Button button1, button2, button3, button4, button5;
    EditText editText1;
    FrameLayout frame1;
    TextView text1;
    Boolean validPeers = false;

    WifiP2pInfo wifiP2pInfo;
    WifiP2pDevice targetDevice;
    Boolean transferReadyState=false;
    Boolean activeTransfer=false;

    private Intent clientServiceIntent;

    public final int port = 7950;

    Camera mainCamera;
    Preview mPreview;
    String previewState = "OFF";
    public byte[] pictureData;

    ImageView imageView;
    Bitmap bmpout;
    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager,mChannel,this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        button1 = (Button)findViewById(R.id.button1);
        button2 = (Button)findViewById(R.id.button2);
        button3 = (Button)findViewById(R.id.button3);
        button4 = (Button)findViewById(R.id.button4);
        button5= (Button)findViewById(R.id.button5);
        text1 = (TextView)findViewById(R.id.textView1);
        frame1= (FrameLayout)findViewById(R.id.previewFrame);
        editText1 = (EditText)findViewById(R.id.editText);

        imageView = (ImageView)findViewById(R.id.imageView);

        //====================================INITIATE THE CAMERA=================================
        openCamera();

        mPreview = new Preview(this, mainCamera);

        //Define the handler to listen for messages from the Preview Class
        mPreview.callHandler(new Handler(){

            public void handleMessage(Message msg){
                if (activeTransfer==false){
                    pictureData =(byte[]) msg.obj;
                    sendData();
                }
            }

        });

        //====================================INITIATE WIFI DIRECT====================================
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener(){

            @Override
            public void onSuccess() {
                text1.setText("Wifi Direct Initiation: Peer Discovery Conducted");
            }

            @Override
            public void onFailure(int reason) {
                text1.setText("Wifi Direct Initiation: Peer Discovery Unsuccessful");
            }
        });

        //====================================INITATE BUTTONS====================================
        //UPDATE PEERS ON UI
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                peerNames.clear();

                if (peers.size()>0){
                    text1.setText("Peers Discovered");
                    int i = 0;
                    while(i<peers.size()){
                        peerNames.add(peers.get(i).deviceName);
                        i++;
                    }
                }
                else
                {
                    text1.setText("No Peers Available");
                }
            }
        });

        //CONNECT TO PEER
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (validPeers==true){
                    config.wps.setup = WpsInfo.PBC;
                    config.groupOwnerIntent = 15;
                    config.deviceAddress = peers.get(peerSelected).deviceAddress;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("NEUTRAL","Connection successful");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d("NEUTRAL","Connection failed");
                        }
                    });
                }


            }
        });

        //SEND DATA
        button3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                /*
                sendData = editText1.getText().toString();
                sendData();
                */
                Log.d("NEUTRAL","BUTTON CLICKED");
            }
        });

        //=================STOP THE PREVIEW=================
        button4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (previewState=="ON"){
                    frame1.removeAllViews();
                    releaseCamera();

                    previewState="PAUSE";
                    text1.setText("Preview Paused");
                    activeTransfer=false;
                    Log.d("NEUTRAL","Preview Paused");
                }
            }
        });

        //=================START THE PREVIEW=================
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewState=="OFF"){
                    try{
                        frame1.addView(mPreview);
                        previewState="ON";
                    }catch(RuntimeException e){
                        Log.d("NEUTRAL","Main Activity: Error in starting preview");
                        System.err.println(e);
                        return;
                    }
                }
                else if(previewState=="PAUSE"){
                    try{
                        openCamera();
                        mPreview.resumePreview(mainCamera);
                        frame1.addView(mPreview);
                        previewState="ON";
                        text1.setText("Preview Resumed");
                        activeTransfer=false;
                    }catch(RuntimeException e){
                        Log.d("NEUTRAL","Main Activity: Error in resuming preview");
                        System.err.println(e);
                        return;
                    }
                }

            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
        releaseCamera();
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList){
        Log.d("NEUTRAL","Main Activity: Listener");
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        validPeers = true;
    }

    public void setClientStatus(String message){
        text1.setText(message);
    }

    public void setNetworkToReadyState(boolean status, WifiP2pInfo info, WifiP2pDevice device){
        wifiP2pInfo=info;
        targetDevice=device;
        transferReadyState=status;
    }

    public void setNetworkToPendingState(boolean status){
        transferReadyState=status;
    }

    public void sendData(){

        if(activeTransfer==false){

            activeTransfer = true;

            if(!transferReadyState){
                Log.d("NEUTRAL","Error - Connection not ready");
                text1.setText("Error - Connection not ready");
            }else if(wifiP2pInfo==null){
                Log.d("NEUTRAL","Error - Missing Wifi P2P Information");
                text1.setText("Error - Missing Wifi P2P Information");
            }
            else
            {
                //Launch Client Service
                //Log.d("NEUTRAL","Data Sent");
                clientServiceIntent = new Intent(this, ClientService.class);
                clientServiceIntent.putExtra("port",new Integer(port));
                clientServiceIntent.putExtra("wifiInfo",wifiP2pInfo);
                clientServiceIntent.putExtra("pictureData",pictureData);
                clientServiceIntent.putExtra("clientResult", new ResultReceiver(null){

                    @Override
                    protected void onReceiveResult(int resultCode, final Bundle resultData){
                        if(resultCode == port){

                            if(resultData==null){
                                activeTransfer=false;

                            }else{
                                final TextView client_status_text= (TextView) findViewById(R.id.textView2);
                                client_status_text.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        client_status_text.setText((String)resultData.get("message"));
                                    }
                                });
                            }
                        }
                    }
                });
                this.startService(clientServiceIntent);
            }
        }else
        {
            Log.d("NEUTRAL","Cannot Send Data");
        }


        //TEST IF DATA TRANSMITTED IS CORRECT - NO FLICKERS
        /*count = pictureData.length;
        Log.d("NEUTRAL","Send Data Called");

        imageView.post(new Runnable() {
            @Override
            public void run() {
                if (count>1500){
                    //Log.d("NEUTRAL","Count Value: " + count);
                    bmpout = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                    imageView.setImageBitmap(bmpout);
                }
            }
        });*/


    }

    public void openCamera(){
        try{
            mainCamera= Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }catch(Exception e){
            Log.d("NEUTRAL","Error opening camera: " + e.getMessage());
        }
    }

    public void releaseCamera(){
        if(mainCamera!=null){
            mainCamera.release();
            mainCamera=null;
        }
    }

}
