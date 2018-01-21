package com.cevangelo117.SMSManageK2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Vagelis on 22/11/2014.
 *
 * Dummy service to make sure this app can be default SMS app.
 */
public class HeadlessSmsSendService extends Service {
    @Override
    public IBinder onBind(Intent intent) {return null;}
}
