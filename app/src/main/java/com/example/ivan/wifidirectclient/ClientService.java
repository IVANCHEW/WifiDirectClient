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
            ObjectOutputStream oos = null;

            try{
                //STANDARD INITIATION CODE
                clientSocket = new Socket(targetIP,port);
                os = clientSocket.getOutputStream();

                os.write(pictureData,0,pictureData.length);
                clientResult.send(port,null);
                os.flush();

                os.close();
                clientSocket.close();


                clientSocket = new Socket(targetIP,port);
                os = clientSocket.getOutputStream();

                os.write(audioData,0,audioData.length);
                clientResult.send(port,null);
                os.flush();

                os.close();
                clientSocket.close();

                //THE FOLLOWING CHUNK OF CODE USE DATA PACKAGING METHODS
                /*
                int pLen = pictureData.length;
                int minLen = 2000;
                int spaces = 2;
                int maxLen = minLen + spaces*1024;
                byte[] byte3 = new byte[2048];
                byte[] byte4  =new byte[1024];
                byte[] byte5  =new byte[1024];
                byte[] byte1 = new byte[4];
                byte[] combined1, combined2;

                Log.d("NEUTRAL","Picture Data Length: " + pLen);
                //Min and Max pictureData length:
                if (pLen>minLen && pLen <maxLen){
                    //First component
                    Log.d("NEUTRAL","Picture Length Sent: " + pLen);
                    ByteBuffer b = ByteBuffer.allocate(4);
                    b.putInt(pLen);
                    byte1=b.array();

                    //Third Commponent
                    int sLen = maxLen - pLen;
                    //Only one space needed
                    if (sLen<=(1024)){
                        //Log.d("NEUTRAL","Space Data Length: " + sLen);
                        String space ="";
                        int x = 0;
                        while (x<sLen){
                            space = space + "0";
                            x+=1;
                        }
                        try {
                            byte3 = (space).getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            //Log.d("NEUTRAL","Error: " + e.getMessage());
                        }
                        //Log.d("NEUTRAL","Space Byte Length: " + byte3.length);
                    }else{
                        String space ="";
                        String space2 = "";
                        sLen=sLen-1024;
                        for(int i=0; i<1024 ; i++){
                            space = space + "0";
                        }
                        for(int i=0; i<sLen ; i++){
                            space2 = space2 + "0";
                        }
                        try {
                            byte5 = (space).getBytes("UTF-8");
                            byte4 = (space2).getBytes("UTF-8");

                            byte3 = combineArray(byte4,byte5);

                        } catch (UnsupportedEncodingException e) {
                            //Log.d("NEUTRAL","Error: " + e.getMessage());
                        }
                    }
                    try{

                        combined1 = combineArray(pictureData,byte3);
                        combined2 = combineArray(byte1,combined1);
                        os.write(combined2,0,combined2.length);

                        //Log.d("NEUTRAL","Length of packaged byte: " + combined2.length);

                        clientResult.send(port,null);

                    }catch (Exception e) {
                        Log.d("NEUTRAL","Error: " + e.getMessage());
                    }
                }
                */

                /*
                oos.write(pictureData);
                clientResult.send(port,null);
                oos.flush();
                */

                //STANDARD CLOSURE CODES
                //oos.close();

            }catch (IOException e){
                //signalActivity(e.getMessage());
                Log.d("NEUTRAL","Client Service Error, IO Exception: " + e.getMessage());
            }catch (Exception e){
                //signalActivity(e.getMessage());
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

