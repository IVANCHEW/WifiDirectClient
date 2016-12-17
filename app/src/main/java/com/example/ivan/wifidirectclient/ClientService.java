package com.example.ivan.wifidirectclient;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ivan on 07/07/16.
 */
public class ClientService extends IntentService {

    private Boolean serviceEnabled=false;
    private int port;
    private ResultReceiver clientResult;
    private WifiP2pDevice targetDevice;
    private WifiP2pInfo wifiP2pInfo;
    private byte[] pictureData, audioData;

    public ClientService(){
        super("ClientService");
        serviceEnabled=true;
        //Log.d("NEUTRAL","Client Service Class: Called");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Log.d("NEUTRAL","Client Intent Received");
        port = ((Integer) intent.getExtras().get("port")).intValue();
        clientResult = (ResultReceiver) intent.getExtras().get("clientResult");
        wifiP2pInfo = (WifiP2pInfo) intent.getExtras().get("wifiInfo");
        pictureData =(byte[])intent.getExtras().get("pictureData");
        audioData = (byte[])intent.getExtras().get("audioData");

        if(!wifiP2pInfo.isGroupOwner){
            //Log.d("NEUTRAL","Begin sending data");
            InetAddress targetIP = wifiP2pInfo.groupOwnerAddress;
            Socket clientSocket=null;
            OutputStream os=null;
            byte[] transfer = null;


            try{

                transfer = new byte[audioData.length + pictureData.length];
                System.arraycopy(audioData,0 , transfer, 0, audioData.length);
                System.arraycopy(pictureData,0 , transfer, audioData.length, pictureData.length);
                //Log.d("NEUTRAL", "Length of picture array: " + pictureData.length);
                //Log.d("NEUTRAL", "Length of audio array: " + audioData.length);

            }catch (Exception e){
                Log.d("NEUTRAL","Client Service Error, data compilation problem: " + e.getMessage());
            }


            try{
                clientSocket = new Socket(targetIP,port);
                os = clientSocket.getOutputStream();

                os.write(transfer,0 ,transfer.length);
                //os.write(pictureData, 0, pictureData.length);

                //Log.d("NEUTRAL","Data Length: " + transfer.length);
                //os.write(audioData,0,audioData.length);
                //os.write(pictureData,0,pictureData.length);

                clientResult.send(port,null);
                os.flush();

                os.close();
                clientSocket.close();
            }catch (IOException e){
                Log.d("NEUTRAL","Client Service Error, IO Exception: " + e.getMessage());
            }catch (Exception e){
                Log.d("NEUTRAL","Client Service Error: " + e.getMessage());
            }
        }else{
            signalActivity("Target device is a group owner");
            //Log.d("NEUTRAL","Target device is a group owner");
        }
        //Log.d("NEUTRAL","Sent Client Result");
    }

    public void signalActivity(String message){
        Bundle b = new Bundle();
        b.putString("message",message);
        clientResult.send(port,b);
        //Log.d("NEUTRAL","Client Service: Signaled Activity");
    }

    public void onDestoy(){
        serviceEnabled=false;
        Log.d("NEUTRAL","Client Service Destroyed");
        stopSelf();
    }

    public static byte[][] divideArray(byte[] source, int chunksize) {

        byte[][] ret = new byte[(int)Math.ceil(source.length / (double)chunksize)][chunksize];

        int start = 0;

        for(int i = 0; i < ret.length; i++) {
            ret[i] = Arrays.copyOfRange(source,start, start + chunksize);
            start += chunksize ;
        }

        return ret;
    }

    public static byte[] combineArray(byte[] A, byte[] B){

        byte[] C = new byte[A.length+B.length];

        for (int i = 0; i < (A.length+B.length); ++i)
        {
            C[i] = i < A.length ? A[i] : B[i - A.length];
        }

        return C;
    }

}

