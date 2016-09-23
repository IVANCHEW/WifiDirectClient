package com.example.ivan.wifidirectclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by ivan on 06/07/16.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    WifiP2pManager.PeerListListener myPeerListListener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity){
        super();
        this.mManager=manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //Check to see if Wifi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                //Wifi P2P is enabled
                Log.d("NEUTRAL", "Wifi P2P is enabled");
            }else{
                //Wifi P2P is disabled
                Log.d("NEUTRAL", "Wifi P2P is disabled");
            }

        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //Call WifiP2pManager.requestPeers() to get a list of current peers

            /*Request available peer from the wifi p2p manager. This is an asynchronous call and
            the calling activity is notified with a callback on PeerListListener.onPeersAvailable()
             */
            if (mManager != null) {
                Log.d("NEUTRAL", "Peer Changed Intent received with Wifi Manager Active");
                mManager.requestPeers(mChannel, mActivity);

            }

        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //Respond to new connections or disconnection
            NetworkInfo networkState = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo wifiInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            if(networkState.isConnected()){
                Log.d("NEUTRAL","Wifi P2P Connection Changed Intent passed, connected");
                mActivity.setClientStatus("Connection Status: Connection Established");
                mActivity.setNetworkToReadyState(true,wifiInfo,device);

            }else{
                mActivity.setNetworkToPendingState(false);
                mActivity.setClientStatus("Connection Status: No connection");
                mManager.cancelConnect(mChannel,null);
            }


        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //Respond to this device's wifi state changing
        }
    }



}
