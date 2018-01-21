package com.cevangelo117.SMSManageK2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Vagelis on 26/10/2014.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())){
            Intent myIntent = new Intent(context, ReceiveSMSService.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(myIntent);
        }
    }
}
