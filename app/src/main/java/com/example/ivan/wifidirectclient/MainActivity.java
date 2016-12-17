package com.example.ivan.wifidirectclient;


import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity implements WifiP2pManager.PeerListListener, AdapterView.OnItemSelectedListener {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    List<WifiP2pDevice> peersConnect = new ArrayList<WifiP2pDevice>();
    List<String> peerNames = new ArrayList<String>();
    int peerSelected=0;

    WifiP2pConfig config = new WifiP2pConfig();

    IntentFilter mIntentFilter;

    Button button1, button2, button3, button4, button5, button6;
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
    //Preview mPreview;
    String previewState = "OFF";
    public byte[] pictureData, audioData;

    ImageView imageView;
    Bitmap bmpout;
    int count=0;
    Spinner spinner;
    ArrayAdapter<String> spinnerArrayAdapter;
    List<Camera.Size> resSize;
    Camera.Size selectedRes;
    List<String> supportedRes = new ArrayList<String>();

    //AUDIO DECLARATIONS
    AudioRecord recorder;
    private int sampleRate = 8000;
    int fixedBufferParameter = 1408;
    int fixedWidth = 320;
    int fixedHeight = 240;
;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean audioStatus = true;
    //private AudioTrack speaker;
    int minBufSize;

    //OPEN GL STUFF
    private MainView mView;
    private PowerManager.WakeLock mWL;
    public Handler screenGrabHandler = null;

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
        button6 = (Button)findViewById(R.id.button6);
        text1 = (TextView)findViewById(R.id.textView1);
        frame1= (FrameLayout)findViewById(R.id.previewFrame);
        editText1 = (EditText)findViewById(R.id.editText);

        imageView = (ImageView)findViewById(R.id.imageView);
        spinner = (Spinner)findViewById(R.id.spinner);

        mView = new MainView(this, fixedWidth, fixedHeight);

        //====================================INITIATE THE CAMERA=================================
        //openCamera();
        getResSize();
        //Default resolution size

        spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, supportedRes);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
        spinner.setAdapter(spinnerArrayAdapter);

        mView.callHandler(new Handler(){

            @Override
            public void handleMessage(Message msg)  {
                int w = fixedWidth;
                int h = fixedHeight
                        ;

                Bundle bundle = msg.getData();
                pictureData = bundle.getByteArray("pictureData");
                sendData();

                /*
                //Byte extraction and bitmap recomposition test

                IntBuffer intBuf =
                        ByteBuffer.wrap(pictureData)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .asIntBuffer();
                int[] array = new int[intBuf.remaining()];
                intBuf.get(array);

                bmpout = Bitmap.createBitmap(array, w, h, Bitmap.Config.ARGB_8888);
                */

                //bmpout = Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);

                //bmpout = BitmapFactory.decodeByteArray(pictureData,0,pictureData.length);

                /*
                if (bmpout == null) {
                    Log.d("NEUTRAL", "Null Bitmap");
                } else {
                    imageView.setImageBitmap(bmpout);
                    Log.d("NEUTRAL", "Handler called, bitmap updated");
                }
                */

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

        //================CONNECT TO PEER================
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

        //================SEND AUDIO - NOT IN USE
        button3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

            }
        });

        button6.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

            }
        });

        //=================STOP THE PREVIEW=================
        button4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (previewState=="ON"){

                    try {
                        previewState = "PAUSE";
                        activeTransfer = false;
                        audioStatus = false;
                        frame1.removeAllViews();

                        Thread.sleep(2000);

                        recorder.release();
                        //releaseCamera();

                        text1.setText("Preview Paused");
                        Log.d("NEUTRAL", "Preview Paused");
                    }catch (Exception e){
                        Log.d("NEUTRAL","Error Stopping Preview");
                        Log.d("NEUTRAL",e.toString());
                    }
                }
            }
        });

        //=================START THE PREVIEW=================
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewState=="OFF"){

                    minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    Log.d("NEUTRAL","Min Buffer Size is:" + minBufSize);
                    minBufSize = fixedBufferParameter;
                    Log.d("NEUTRAL","Audio Sample Rate is: " + sampleRate);
                    Log.d("NEUTRAL","Audio File Format is: " + audioFormat);
                    audioStatus = true;
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize);
                    Log.d("NEUTRAL", "Recorder initialized");
                    startAudioStreaming();

                    try{
                        frame1.addView(mView);
                        previewState="ON";
                    }catch(RuntimeException e){
                        Log.d("NEUTRAL","Main Activity: Error in starting preview");
                        System.err.println(e);
                        return;
                    }
                }
                else if(previewState=="PAUSE"){
                    try{
                        audioStatus = true;
                        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize);
                        Log.d("NEUTRAL", "Recorder initialized");
                        startAudioStreaming();


                        //openCamera();
                        //mPreview.setRes(selectedRes);
                        //mPreview.resumePreview(mainCamera);
                        //frame1.addView(mPreview);
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

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        spinner.setSelection(position);
        selectedRes = resSize.get(position);
        Log.d("NEUTRAL","Res Size Selected, Width: " + selectedRes.width + " , Height: " + selectedRes.height);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        spinner.setSelection(0);
        selectedRes =resSize.get(0);
    }

    private void getResSize(){
        Log.d("NEUTRAL", "Get Res Size Called");
        try{
            resSize = mainCamera.getParameters().getSupportedPreviewSizes();
            for (Camera.Size x: resSize){
                supportedRes.add(x.width + " x " + x.height);
            }
        }catch(Exception e){
            Log.d("NEUTRAL","Error in getting Res Size: " + e.getMessage());
        }
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
        //releaseCamera();
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

        //Log.d("NEUTRAL","Send Data Called");
        if(activeTransfer==false){

            //activeTransfer = true;
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
                count = count + 1;
                Log.d("NEUTRAL","Frame: " + count);
                clientServiceIntent = new Intent(this, ClientService.class);
                clientServiceIntent.putExtra("port",new Integer(port));
                clientServiceIntent.putExtra("wifiInfo",wifiP2pInfo);
                clientServiceIntent.putExtra("pictureData",pictureData);
                clientServiceIntent.putExtra("audioData", audioData);
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

    }

    /*
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
    */

    public void startAudioStreaming(){
        final Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("NEUTRAL","Main Activity: Started Audio Streaming Thread");
                    recorder.startRecording();
                    while(audioStatus == true) {
                        byte[] buffer = new byte[minBufSize];
                        //Log.d("NEUTRAL","Buffer created of size " + minBufSize);
                        //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.length);
                        //Log.d("NEUTRAL","Finished Reading Recorder Data");
                        audioData = buffer;
                    }

                } catch (Exception e) {
                    Log.d("NEUTRAL", "IOException: " + e.getMessage());
                }
            }
        });
        streamThread.start();
    }

}

// View
class MainView extends GLSurfaceView {
    MainRenderer mRenderer;

    private Handler mainViewHandler = null;

    public void callHandler(Handler handler){

        this.mainViewHandler = handler;

    }

    MainView (Context context, int w, int h) {
        super ( context );
        mRenderer = new MainRenderer(this, w, h);
        setEGLContextClientVersion ( 2 );
        setRenderer ( mRenderer );
        setRenderMode ( GLSurfaceView.RENDERMODE_WHEN_DIRTY );

        //Handles message coming from renderer
        mRenderer.callHandler(new Handler(){

            @Override
            public void handleMessage(Message msg)  {

                Message msg2 = mainViewHandler.obtainMessage();
                Bundle bundle = msg.getData();
                msg2.setData(bundle);
                mainViewHandler.sendMessage(msg2);

            }
        });
    }

    public void surfaceCreated ( SurfaceHolder holder ) {
        super.surfaceCreated ( holder );
    }

    public void surfaceDestroyed ( SurfaceHolder holder ) {
        mRenderer.close();
        super.surfaceDestroyed ( holder );
    }

    public void surfaceChanged (SurfaceHolder holder, int format, int w, int h ) {
        super.surfaceChanged ( holder, format, w, h );
    }
}

// Renderer
class MainRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private final String vss =
            "attribute vec2 vPosition;\n" +
                    "attribute vec2 vTexCoord;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  texCoord = vTexCoord;\n" +
                    "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
                    "}";

    private final String fss =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                    "}";

    private int[] hTex;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;
    private int hProgram;

    private Camera mCamera;
    private SurfaceTexture mSTexture;

    private boolean mUpdateST = false;

    private MainView mView;

    //Stuff to send picture
    int w = 320;
    int h = 240;
    int bitmapBuffer[] = new int[w * h];
    int bitmapSource[] = new int[w * h];
    byte[] b;

    private Bitmap bmpout;
    private Handler mainRendererHandler = null;

    public void callHandler(Handler handler){

        this.mainRendererHandler = handler;

    }


    MainRenderer (MainView view, int mainw, int mainh) {
        w = mainw;
        h = mainh;
        mView = view;
        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        float[] ttmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        pVertex = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put ( vtmp );
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pTexCoord.put ( ttmp );
        pTexCoord.position(0);
    }

    public void close()
    {
        mUpdateST = false;
        mSTexture.release();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        deleteTex();
    }

    @Override
    public void onSurfaceCreated (GL10 unused, EGLConfig config ) {
        initTex();
        mSTexture = new SurfaceTexture ( hTex[0] );
        mSTexture.setOnFrameAvailableListener(this);

        mCamera = Camera.open();
        try {
            mCamera.setPreviewTexture(mSTexture);
        } catch ( IOException ioe ) {
        }

        GLES20.glClearColor ( 1.0f, 1.0f, 0.0f, 1.0f );

        hProgram = loadShader ( vss, fss );
    }

    public void onDrawFrame ( GL10 unused ) {

        //Send picture stuff
        createBitmap();
        sendMessage();

        GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT );

        synchronized(this) {
            if ( mUpdateST ) {
                mSTexture.updateTexImage();
                mUpdateST = false;
            }
        }

        GLES20.glUseProgram(hProgram);

        int ph = GLES20.glGetAttribLocation(hProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation ( hProgram, "vTexCoord" );
        int th = GLES20.glGetUniformLocation ( hProgram, "sTexture" );

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        GLES20.glUniform1i(th, 0);

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4*2, pVertex);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoord );
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
    }

    public void onSurfaceChanged (GL10 unused, int width, int height ) {
        GLES20.glViewport( 0, 0, w, h );
        Camera.Parameters param = mCamera.getParameters();
        param.setPreviewSize(w, h);
        param.set("orientation", "landscape");
        mCamera.setParameters ( param );
        mCamera.startPreview();
    }

    private void initTex() {
        hTex = new int[1];
        GLES20.glGenTextures ( 1, hTex, 0 );
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    private void deleteTex() {
        GLES20.glDeleteTextures ( 1, hTex, 0 );
    }

    public synchronized void onFrameAvailable ( SurfaceTexture st ) {
        mUpdateST = true;
        mView.requestRender();

    }

    private static int loadShader ( String vss, String fss ) {
        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile vshader");
            Log.v("Shader", "Could not compile vshader:"+GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            vshader = 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile fshader");
            Log.v("Shader", "Could not compile fshader:"+GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(fshader);
            fshader = 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vshader);
        GLES20.glAttachShader(program, fshader);
        GLES20.glLinkProgram(program);

        return program;
    }

    private void createBitmap() {
        //Extract Byte[]
        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
            GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
            buf.rewind();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bmpout;
            bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmpout.copyPixelsFromBuffer(buf);
            bmpout.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            b = stream.toByteArray();
            //Log.d("NEUTRAL","Data length: " + b.length);

        } catch (GLException e) {
            Log.d("NEUTRAL","Error in creating Bitmap" + e);
        }
    }

    private void sendMessage(){

        Message msg = mainRendererHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putByteArray("pictureData", b);
        msg.setData(bundle);
        mainRendererHandler.sendMessage(msg);

    }

}