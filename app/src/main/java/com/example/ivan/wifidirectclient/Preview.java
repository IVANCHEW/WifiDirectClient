package com.example.ivan.wifidirectclient;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;

//Preview class used to start the camera preview
public class Preview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    SurfaceHolder mHolder;
    Camera mCamera;
    Camera.Size previewSize;
    Camera.Parameters param;
    Boolean TrackTarget =false;
    int[] colorSample = new int[3];
    int[] pixels;
    int count= 0;
    int targetPosition=0;
    int hexColor;
    double[] labColor2= new double[3];
    double[] labColor1= new double[3];

    //Prepare handler to send message
    private Handler previewHandler=null;

    public void callHandler(Handler handler){

        this.previewHandler = handler;

    }

    public Preview(Context context, Camera camera) {
        super(context);

        mCamera = camera;

        mHolder=getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
        }catch(Exception e) {
            Log.d("NEUTRAL", "Error setting holder");
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        //Sets Smallest Preview Size
        param = mCamera.getParameters();
        previewSize = getSmallestPreviewSize(width,height);
        param.setPreviewSize(previewSize.width,previewSize.height);
        //Constant for NV21 format is 17
        param.setPreviewFormat(17);

        mCamera.setDisplayOrientation(0);
        mCamera.setParameters(param);
        pixels = new int[previewSize.width* previewSize.height];
        Log.d("NEUTRAL3","Preview Size: Width=" + previewSize.width + " Height=" + previewSize.height);

        try{
            mCamera.startPreview();
        }catch(Exception e){
            Log.d("NEUTRAL","Error starting preview");
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }catch(Exception e){
            Log.d("NEUTRAL","Error stoppipng camera on surface destroy");
        }
    }

    private Camera.Size getBestPreviewSize (int width, int height){

        Camera.Size result = null;
        Camera.Parameters param = mCamera.getParameters();
        for (Camera.Size size : param.getSupportedPreviewSizes()){
            if (result==null){result=size;}
            else{
                int resultArea = result.width*result.height;
                int newArea = size.width*size.height;

                if (newArea>resultArea){
                    result = size;
                }
            }
        }

        return result;
    }

    private Camera.Size getSmallestPreviewSize (int width, int height){

        Camera.Size result = null;
        Camera.Parameters param = mCamera.getParameters();
        for (Camera.Size size : param.getSupportedPreviewSizes()){
            if (result==null){result=size;}
            else{
                int resultArea = result.width*result.height;
                int newArea = size.width*size.height;

                if (newArea<resultArea){
                    result = size;
                }
            }
        }

        return result;
    }

    public void sendColor(int data, double[] getLAB){

        hexColor = data;
        labColor1[0] = getLAB[0];
        labColor1[1] = getLAB[1];
        labColor1[2] = getLAB[2];

        TrackTarget = true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //count = count + 1;
        //Log.d("NEUTRAL", "Frame received" + count);

        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

        byte[] bytes = out.toByteArray();

        previewHandler.removeCallbacksAndMessages(null);
        Message msg = Message.obtain();
        msg.obj =bytes;
        msg.setTarget(previewHandler);
        msg.sendToTarget();
    }

    void findTarget(int[] pixels, byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;
        int rgb,r2, b2, g2;
        double DE;
        targetPosition=0;

        //Log.d("NEUTRAL2", "Processing decode");
        outerLoop:
        for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)                  r = 0;               else if (r > 262143)
                    r = 262143;
                if (g < 0)                  g = 0;               else if (g > 262143)
                    g = 262143;
                if (b < 0)                  b = 0;               else if (b > 262143)
                    b = 262143;

                rgb = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

                //Conversion from HEX to RBG
                r2 = (rgb >> 16)& 0xff;
                g2 = (rgb >> 8)& 0xff;
                b2 = (rgb) & 0xff;

                ColorUtils.RGBToLAB(r2,g2,b2,labColor2);

                //String showValues = "Sample: " + labColor1[0] + "," + labColor1[1] + "," + labColor1[2] + " Pixel: " + labColor2[0] + "," + labColor2[1] + "," + labColor2[2];
                //Log.d("NEUTRAL2", showValues);

                DE = Math.sqrt(Math.pow(labColor2[0]-labColor1[0],2) + Math.pow(labColor2[1]-labColor1[1],2) + Math.pow(labColor2[2]-labColor1[2],2));

                if (DE<8){

                    targetPosition = yp+1;
                    //pixels[yp] = 0xffffffff;
                    //Log.d("NEUTRAL2","Matched Color" + hexColor + "," + " Delta: " + DE + " Position: " + yp);
                    break outerLoop;

                }else{

                    //pixels[yp]=0xff000000;

                }
            }

        }
    }



}

