package com.cevangelo117.SMSManageK2;

import android.app.Service;
import android.content.*;
import android.os.IBinder;

/**
 * Created by Vagelis on 21/10/2014.
 */
public class ReceiveSMSService extends Service
{
    public static boolean[] receivedSMSFromWhatsUp = {false,false,false,false,false,false};
    public static final int TYPE_YPOLOIPO = 0;
    public static final int TYPE_MIN2WU = 1;
    public static final int TYPE_SMS2WU = 2;
    public static final int TYPE_MIN2ALL = 3;
    public static final int TYPE_SMS2ALL = 4;
    public static final int TYPE_DATA = 5;

    static String RESULT_SMS_RECEIVED = "com.evangecp.SMSManage.SMS_RECEIVED";
    static String RESULT_REFRESH_WU_STATS = "com.evangecp.SMSManage.REFRESH_WU_STATS";
    private SMSReceiver mSMSreceiver;
    private IntentFilter mIntentFilter;
    GlobalClass global = null;
    public static final String YPOLOIPO = "ypoloipo";
    public static final String MINWHATSUP = "minWhatsup";
    public static final String SMSWHATSUP = "smsWhatsup";
    public static final String MIN2ALL = "min2all";
    public static final String SMS2ALL = "sms2all";
    public static final String DATA = "data";

    @Override
    public void onCreate()
    {
        super.onCreate();

        global = (GlobalClass) getApplicationContext();

        //Register the SMSReceiver with the proper IntentFilter
        mSMSreceiver = new SMSReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(mSMSreceiver, mIntentFilter);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Unregister the SMS receiver
        unregisterReceiver(mSMSreceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}