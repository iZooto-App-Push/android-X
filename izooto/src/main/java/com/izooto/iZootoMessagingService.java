/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izooto;

import static com.izooto.NewsHubAlert.newsHubDBHelper;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONObject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class iZootoMessagingService extends FirebaseMessagingService {
    private  Payload payload = null;
    private final String IZ_TAG_NAME="iZootoMessagingService";
    private final String IZ_METHOD_NAME = "handleNow";
    private final String IZ_METHOD_PUSH_NAME ="contentPush";
    private  final String IZ_ERROR_NAME ="Payload Error";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        executeBackgroundTask(remoteMessage);
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            executorService.execute(runnable);
        } catch (Exception ex){
            Log.e(AppConstant.FIREBASEEXCEPTION, IZ_TAG_NAME + ex);
            Util.handleExceptionOnce(this, remoteMessage + ex.toString(), IZ_TAG_NAME, "onMessageReceived");
        }
    }

    private void executeBackgroundTask(RemoteMessage remoteMessage) {
        try {
            if (remoteMessage.getData().size() > 0) {
                Log.v("Push Type", "fcm");
                PreferenceUtil preferenceUtil = PreferenceUtil.getInstance(this);
                if (preferenceUtil.getEnableState(AppConstant.NOTIFICATION_ENABLE_DISABLE)) {
                    Map<String, String> data = remoteMessage.getData();
                    handleNow(data);
                }
            }
            if (remoteMessage.getNotification() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sendNotification(remoteMessage);
                }
            }
        } catch (Exception ex) {
            Log.e(AppConstant.FIREBASEEXCEPTION, IZ_TAG_NAME + ex);
            Util.handleExceptionOnce(this, remoteMessage + ex.toString(), IZ_TAG_NAME, "executeBackgroundTask");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void sendNotification(RemoteMessage remoteMessage) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(remoteMessage.getNotification().getTitle())
                        .setContentText(remoteMessage.getNotification().getBody())
                        .setAutoCancel(true)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    AppConstant.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());


    }

/* Handle the iZooto Push Notification Payload*/
    public   void handleNow(final Map<String, String> data) {
        Log.d(AppConstant.APP_NAME_TAG, AppConstant.NOTIFICATIONRECEIVED);
        PreferenceUtil preferenceUtil =PreferenceUtil.getInstance(this);
        try {
            if(data.get(AppConstant.AD_NETWORK) !=null || data.get(AppConstant.GLOBAL)!=null || data.get(AppConstant.GLOBAL_PUBLIC_KEY)!=null)
            {
                if(data.get(AppConstant.GLOBAL_PUBLIC_KEY)!=null)
                {
                    try
                    {
                        JSONObject jsonObject=new JSONObject(Objects.requireNonNull(data.get(AppConstant.GLOBAL)));
                        String urlData=data.get(AppConstant.GLOBAL_PUBLIC_KEY);
                        if(jsonObject.toString()!=null && urlData!=null && !urlData.isEmpty()) {
                            String cid = jsonObject.optString(ShortPayloadConstant.ID);
                            String rid = jsonObject.optString(ShortPayloadConstant.RID);
                            int cfg=jsonObject.optInt(ShortPayloadConstant.CFG);
                            String cfgData=Util.getIntegerToBinary(cfg);
                            if(cfgData!=null && !cfgData.isEmpty()) {
                                String impIndex = String.valueOf(cfgData.charAt(cfgData.length() - 1));
                                if(impIndex.equalsIgnoreCase("1"))
                                {
                                    NotificationEventManager.impressionNotification(RestClient.IMPRESSION_URL, cid, rid, -1,AppConstant.PUSH_FCM);
                                }
                            }

                            AdMediation.getMediationGPL(this, jsonObject, urlData);
                            preferenceUtil.setBooleanData(AppConstant.MEDIATION, false);

                        }
                        else
                        {
                            NotificationEventManager.handleNotificationError(IZ_ERROR_NAME,data.toString(),IZ_TAG_NAME,IZ_METHOD_NAME);
                        }
                    }
                    catch (Exception ex)
                    {

                        DebugFileManager.createExternalStoragePublic(iZooto.appContext,IZ_METHOD_PUSH_NAME,data.toString());
                        Util.handleExceptionOnce(this,ex.toString()+IZ_ERROR_NAME+data,IZ_TAG_NAME,IZ_METHOD_NAME);
                    }

                }
                else {
                    try {
                        JSONObject jsonObject = new JSONObject(data.get(AppConstant.GLOBAL));
                        String cid = jsonObject.optString(ShortPayloadConstant.ID);
                        String rid = jsonObject.optString(ShortPayloadConstant.RID);
                        int cfg=jsonObject.optInt(ShortPayloadConstant.CFG);
                        String cfgData=Util.getIntegerToBinary(cfg);
                        if(cfgData!=null && !cfgData.isEmpty()) {
                            String impIndex = String.valueOf(cfgData.charAt(cfgData.length() - 1));
                            if(impIndex.equalsIgnoreCase("1"))
                            {
                                NotificationEventManager.impressionNotification(RestClient.IMPRESSION_URL, cid, rid, -1,AppConstant.PUSH_FCM);

                            }

                        }
                        JSONObject jsonObject1=new JSONObject(data.toString());
                        AdMediation.getMediationData(this, jsonObject1,"fcm","");
                        preferenceUtil.setBooleanData(AppConstant.MEDIATION, true);
                    }
                    catch (Exception ex)
                    {
                        Util.handleExceptionOnce(this,ex+IZ_ERROR_NAME+data,IZ_TAG_NAME,IZ_METHOD_NAME);
                        DebugFileManager.createExternalStoragePublic(iZooto.appContext,IZ_METHOD_PUSH_NAME,data.toString());

                    }
                }
            }
            else {
                preferenceUtil.setBooleanData(AppConstant.MEDIATION, false);
                JSONObject payloadObj = new JSONObject(data);
                if (payloadObj.optLong(ShortPayloadConstant.CREATEDON) > PreferenceUtil.getInstance(this).getLongValue(AppConstant.DEVICE_REGISTRATION_TIMESTAMP)) {
                    payload = new Payload();
                    payload.setCreated_Time(payloadObj.optString(ShortPayloadConstant.CREATEDON));
                    payload.setFetchURL(payloadObj.optString(ShortPayloadConstant.FETCHURL));
                    payload.setKey(payloadObj.optString(ShortPayloadConstant.KEY));
                    payload.setId(payloadObj.optString(ShortPayloadConstant.ID).replace("['","").replace("']","").replace("~",""));
                    payload.setRid(payloadObj.optString(ShortPayloadConstant.RID));
                    payload.setLink(payloadObj.optString(ShortPayloadConstant.LINK).replace("['","").replace("']",""));
                    payload.setTitle(payloadObj.optString(ShortPayloadConstant.TITLE).replace("['","").replace("']",""));
                    payload.setMessage(payloadObj.optString(ShortPayloadConstant.NMESSAGE).replace("['","").replace("']",""));
                    payload.setIcon(payloadObj.optString(ShortPayloadConstant.ICON).replace("['","").replace("']",""));
                    payload.setReqInt(payloadObj.optInt(ShortPayloadConstant.REQINT));
                    payload.setTag(payloadObj.optString(ShortPayloadConstant.TAG));
                    payload.setBanner(payloadObj.optString(ShortPayloadConstant.BANNER).replace("['","").replace("']",""));
                    payload.setAct_num(payloadObj.optInt(ShortPayloadConstant.ACTNUM));
                    payload.setBadgeicon(payloadObj.optString(ShortPayloadConstant.BADGE_ICON).replace("['","").replace("']",""));
                    payload.setBadgecolor(payloadObj.optString(ShortPayloadConstant.BADGE_COLOR));
                    payload.setSubTitle(payloadObj.optString(ShortPayloadConstant.SUBTITLE));
                    payload.setGroup(payloadObj.optInt(ShortPayloadConstant.GROUP));
                    payload.setBadgeCount(payloadObj.optInt(ShortPayloadConstant.BADGE_COUNT));
                    // Button 2
                    payload.setAct1name(payloadObj.optString(ShortPayloadConstant.ACT1NAME));
                    payload.setAct1link(payloadObj.optString(ShortPayloadConstant.ACT1LINK).replace("['","").replace("']",""));
                    payload.setAct1icon(payloadObj.optString(ShortPayloadConstant.ACT1ICON));
                    payload.setAct1ID(payloadObj.optString(ShortPayloadConstant.ACT1ID));
                    // Button 2
                    payload.setAct2name(payloadObj.optString(ShortPayloadConstant.ACT2NAME));
                    payload.setAct2link(payloadObj.optString(ShortPayloadConstant.ACT2LINK).replace("['","").replace("']",""));
                    payload.setAct2icon(payloadObj.optString(ShortPayloadConstant.ACT2ICON));
                    payload.setAct2ID(payloadObj.optString(ShortPayloadConstant.ACT2ID));

                    payload.setInapp(payloadObj.optInt(ShortPayloadConstant.INAPP));
                    payload.setTrayicon(payloadObj.optString(ShortPayloadConstant.TARYICON));
                    payload.setSmallIconAccentColor(payloadObj.optString(ShortPayloadConstant.ICONCOLOR));
                    payload.setFromProjectNumber(payloadObj.optString(ShortPayloadConstant.PROJECTNUMBER));
                    payload.setCollapseId(payloadObj.optString(ShortPayloadConstant.COLLAPSEID));
                    payload.setRawPayload(payloadObj.optString(ShortPayloadConstant.RAWDATA));
                    payload.setAp(payloadObj.optString(ShortPayloadConstant.ADDITIONALPARAM));
                    payload.setCfg(payloadObj.optInt(ShortPayloadConstant.CFG));
                    payload.setPush_type(AppConstant.PUSH_FCM);
                    payload.setMaxNotification(payloadObj.optInt(ShortPayloadConstant.MAX_NOTIFICATION));
                    payload.setFallBackDomain(payloadObj.optString(ShortPayloadConstant.FALL_BACK_DOMAIN));
                    payload.setFallBackSubDomain(payloadObj.optString(ShortPayloadConstant.FALLBACK_SUB_DOMAIN));
                    payload.setFallBackPath(payloadObj.optString(ShortPayloadConstant.FAll_BACK_PATH));
                    payload.setDefaultNotificationPreview(payloadObj.optInt(ShortPayloadConstant.TEXTOVERLAY));
                    payload.setNotification_bg_color(payloadObj.optString(ShortPayloadConstant.BGCOLOR));
                    payload.setRc(payloadObj.optString(ShortPayloadConstant.RC));
                    payload.setRv(payloadObj.optString(ShortPayloadConstant.RV));
                    payload.setExpiryTimerValue(payloadObj.optString(ShortPayloadConstant.EXPIRY_TIMER_VALUE));
                    payload.setMakeStickyNotification(payloadObj.optString(ShortPayloadConstant.MAKE_STICKY_NOTIFICATION));
                    payload.setOfflineCampaign(payloadObj.optString(ShortPayloadConstant.OFFLINE_CAMPAIGN));

                    // notification channel payload
                    payload.setPriority(payloadObj.optInt(ShortPayloadConstant.PRIORITY));
                    payload.setGroupKey(payloadObj.optString(ShortPayloadConstant.GKEY));
                    payload.setGroupMessage(payloadObj.optString(ShortPayloadConstant.GMESSAGE));
                    payload.setSound(payloadObj.optString(ShortPayloadConstant.SOUND));
                    payload.setLedColor(payloadObj.optString(ShortPayloadConstant.LEDCOLOR));
                    payload.setLockScreenVisibility(payloadObj.optInt(ShortPayloadConstant.VISIBILITY));
                    payload.setChannel(payloadObj.optString(ShortPayloadConstant.NOTIFICATION_CHANNEL));
                    payload.setVibration(payloadObj.optString(ShortPayloadConstant.VIBRATION));
                    payload.setBadge(payloadObj.optInt(ShortPayloadConstant.BADGE));
                    payload.setOtherChannel(payloadObj.optString(ShortPayloadConstant.OTHER_CHANNEL));
                    try {
                        if (payload.getRid() != null && !payload.getRid().isEmpty()) {
                            preferenceUtil.setIntData(ShortPayloadConstant.OFFLINE_CAMPAIGN, Util.getValidIdForCampaigns(payload));
                        } else {
                            DebugFileManager.createExternalStoragePublic(iZooto.appContext, IZ_METHOD_PUSH_NAME, data.toString());
                        }
                        if (payload.getLink() != null && !payload.getLink().isEmpty()) {
                            int campaigns = preferenceUtil.getIntData(ShortPayloadConstant.OFFLINE_CAMPAIGN);
                            if (campaigns == AppConstant.CAMPAIGN_SI || campaigns == AppConstant.CAMPAIGN_SE) {
                                DebugFileManager.createExternalStoragePublic(iZooto.appContext, IZ_METHOD_PUSH_NAME, data.toString());
                            } else {
                                newsHubDBHelper.addNewsHubPayload(payload);
                            }

                        }
                    } catch (Exception e) {
                        DebugFileManager.createExternalStoragePublic(iZooto.appContext, IZ_METHOD_PUSH_NAME, e.toString());
                    }
                    if (iZooto.appContext == null) {
                        iZooto.appContext = this;
                    }

                    final Handler mainHandler = new Handler(Looper.getMainLooper());
                    final Runnable myRunnable = () -> {
                        NotificationEventManager.handleImpressionAPI(payload, AppConstant.PUSH_FCM);
                        iZooto.processNotificationReceived(iZooto.appContext, payload);
                    };

                    try {
                        NotificationExecutorService notificationExecutorService = new NotificationExecutorService(this);
                        notificationExecutorService.executeNotification(mainHandler, myRunnable, payload);

                    } catch (Exception e) {
                        Util.handleExceptionOnce(iZooto.appContext, e.toString(), IZ_TAG_NAME, IZ_METHOD_NAME + "notificationExecutorService");
                    }
                    DebugFileManager.createExternalStoragePublic(iZooto.appContext, data.toString(), " Log-> ");
                } else {
                    String updateDaily = NotificationEventManager.getDailyTime(this);
                    if (!updateDaily.equalsIgnoreCase(Util.getTime())) {
                        preferenceUtil.setStringData(AppConstant.CURRENT_DATE_VIEW_DAILY, Util.getTime());
                        if (!preferenceUtil.getBoolean(AppConstant.IZ_PAYLOAD_ERROR)) {
                            NotificationEventManager.handleNotificationError(IZ_ERROR_NAME + payloadObj.optString("t"), payloadObj.toString(), IZ_TAG_NAME, IZ_METHOD_NAME);
                        } else {
                            preferenceUtil.setBooleanData(AppConstant.IZ_PAYLOAD_ERROR, true);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Util.handleExceptionOnce(this, data+e.toString(), IZ_TAG_NAME, IZ_METHOD_NAME);

        }
    }
}

