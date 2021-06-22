package com.app.izoototest;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import com.izooto.NotificationWebViewListener;
import com.izooto.iZooto;
import com.izooto.NotificationHelperListener;
import com.izooto.Payload;
import com.izooto.TokenReceivedListener;

public class AppController extends Application implements TokenReceivedListener,NotificationHelperListener, NotificationWebViewListener

{

    @Override
    public void onCreate() {
        super.onCreate();
        iZooto.initialize(this)
                .setNotificationReceiveListener(this)
                .setTokenReceivedListener(this)
                .setLandingURLListener(this)
                .build();
        iZooto.setNotificationSound("miringtone");


    }

    @Override
    public void onTokenReceived(String token) {
        Log.e("TokenData",token);

    }

    @Override
    public void onNotificationReceived(Payload payload) {
        Log.e("Payload",payload.getTitle());

    }

    @Override
    public void onNotificationOpened(String data) {
     Log.e("Data",data);
         Intent intent=new Intent(this,MainActivity.class);// launch activity name
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         startActivity(intent);
    }


    @Override
    public void onWebView(String landingUrl) {
        Log.e("LandingURL",landingUrl);
        Intent intent=new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}