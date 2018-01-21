package com.cevangelo117.SMSManageK2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Vagelis on 27/11/2014.
 */
public class SMSReceiver extends BroadcastReceiver
{
    private final String TAG = this.getClass().getSimpleName();
    GlobalClass global = null;
    private int mId = 0;

    Context receiver_context;
    @Override
    public void onReceive(Context context, Intent intent) {
        receiver_context = context;
        global = (GlobalClass) context.getApplicationContext();

        Bundle extras = intent.getExtras();
        // Variable that holds the whole message
        String strMessageWhole = "";
        // Variable that holds a part of the message (if the message was to large)
        String strMsgBody;
        // We keep the first part of the message so we can show it to the notification
        String first_part_of_multiple_msg = null;
        // The source of the incoming SMS
        String strMsgSrc = null;

        if (extras != null) { // If the SMS is not empty
            Object[] smsextras = (Object[]) extras.get("pdus");

            for (int i = 0; i < smsextras.length; i++) {
                // We create an SmsMessage Object from the pdu data
                SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) smsextras[i]);

                // We store the first part of the message
                if (i == 0)
                    first_part_of_multiple_msg = smsmsg.getMessageBody().toString();

                strMsgBody = smsmsg.getMessageBody().toString();
                strMsgSrc = smsmsg.getOriginatingAddress();

                // we accumulate the complete message on the variable strMessageWhole
                strMessageWhole += strMsgBody;
                Log.i(TAG, strMessageWhole);

                // If the SMS is coming from the WhatsUp and is not e reloadit code
                if (strMsgSrc.equals("WhatsUp") && !strMsgBody.contains("WHAT'S UP RELOAD IT")) {
                    //Toast.makeText(context.getApplicationContext(), "SMS from Whats Up", Toast.LENGTH_LONG).show();
                    handleWhatsUpSMS(strMsgBody);
                    this.abortBroadcast(); //so the default app not showning notification and more specifiacally not seeing the incoming sms
                    return;
                }
                // If the SMS is coming from Cosmote
                else if (strMsgSrc.equals("Cosmote")) {
                    //Toast.makeText(context.getApplicationContext(), "SMS from Cosmote", Toast.LENGTH_LONG).show();
                    handleCosmoteSMS(strMsgBody);
                    this.abortBroadcast(); //so the default app not showning notification and more specifiacally not seeing the incoming sms
                    return;
                }

                this.abortBroadcast(); //so the default app not showing notification and more specifically not seeing the incoming sms
            }
            writeSMSToDB(strMsgSrc, strMessageWhole);

        }

        // Create Inbox box URI
        Uri inboxURI = Uri.parse("content://sms/inbox");

        // List required columns
        String[] reqCols = new String[]{"_id", "address", "body", "thread_id"};

        // Selection
        String selection = "address" + " = '" + strMsgSrc + "'";

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = context.getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(inboxURI, reqCols, selection, null, null);

        int thread_id = 0;
        if (c.moveToFirst()) {
            String SmsMessageId = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
            thread_id = c.getInt(c.getColumnIndexOrThrow("thread_id"));
            Log.d("Address", c.getString(c.getColumnIndexOrThrow("address")).toString());
            Log.d("Thread_ID", Integer.toString(thread_id));
            if(ThreadActivity.active && global.getCurrent_thread_id()==thread_id){
                ContentValues values = new ContentValues();
                values.put("read", true);
                cr.update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
            }
        }
        ContactInfo contactInfo = getContactInfo(strMsgSrc.toString().replace(" ", "").replace("+30", ""));

        if (!ThreadActivity.active ){
            int num_of_unread_msgs = getNumOfUnreadMsgs(thread_id);
            String contentTitle,contextText;
            if(num_of_unread_msgs == 1) { // the list of unread messages has only the mesage that now has been received
                contentTitle = contactInfo.getContact_name();
                contextText = first_part_of_multiple_msg;
            }
            else if(num_of_unread_msgs >= 10) {
                contentTitle = "New messages ";
                contextText = "from " + contactInfo.getContact_name();
            }
            else {
                contentTitle = num_of_unread_msgs + " new messages ";
                contextText = "from " + contactInfo.getContact_name();
            }
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context.getApplicationContext())
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle(contentTitle)
                            .setContentText(contextText)
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setVibrate(new long[]{0, 100, 50, 100})
                            //.setPriority(Notification.PRIORITY_HIGH) // IF WE WANT HEADS UP NOTIFICATIONS
                            .setAutoCancel(true);
            // Creates an explicit intent for an Activity in your app

            Intent resultIntent = new Intent(context.getApplicationContext(), ThreadActivity.class);
            resultIntent.putExtra("contact_name", contactInfo.getContact_name());
            resultIntent.putExtra("contact_number", contactInfo.getContact_number());
            resultIntent.putExtra("thread_id", thread_id);
            resultIntent.putExtra("contact_ID", contactInfo.getContact_ID());

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(ThreadActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            // we use thread_id as a unique identifier
            mId = thread_id;
            mNotificationManager.notify(mId, mBuilder.build());
        } else if(ThreadActivity.active && global.getCurrent_thread_id()==thread_id){

            Log.d("SMSManage","onHandleIntent called");
            Intent in=new Intent(ReceiveSMSService.RESULT_SMS_RECEIVED);  //you can put anything in it with putExtra
            in.putExtra("new_msg",strMessageWhole);
            Log.d("SMSManage","sending broadcast");
            LocalBroadcastManager.getInstance(receiver_context).sendBroadcast(in);
        }

        PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(( PowerManager.FULL_WAKE_LOCK| PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                wakeLock.release();
            }
        }, 2000);

//later
        //screenLock.release();

    }

    public int getNumOfUnreadMsgs(int thread_id){
        // Create Inbox box URI
        Uri inboxURI = Uri.parse("content://sms/inbox");

        // List required columns
        String[] reqCols = new String[]{"_id", "thread_id", "read"};

        // Selection
        String selection = "thread_id" + " = '" + thread_id + "'";

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = receiver_context.getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(inboxURI, reqCols, selection, null, null);

        int messages_scanned = 0;
        int unread_msgs = 0;
        try {
            while(c.moveToNext() && messages_scanned <=10) {
                if (c != null && c.getCount() > 0) {
                    int read_var = c.getInt(c.getColumnIndex(Telephony.TextBasedSmsColumns.READ));
                    messages_scanned++;
                    if(read_var == 0) //if message is unread increase the value of the counter
                        unread_msgs++;
                    System.out.println("read_var = " + read_var);
                }
            }
        } finally {
            if ( c != null) {
                c.close();
            }
        }
        return unread_msgs;
    }

    public ArrayList<String> getSMSWhatsUpAnalysis(String message){

        ArrayList<String> portions = new ArrayList<String>();
        try {
            while (message.replace(" ", "") != "") {
                int start = message.indexOf("(");
                int end = message.indexOf(")");
                String portion = message.substring(start, end);
                portions.add(portion);
                message = message.substring(end + 1, message.length());
                Log.d("SMS ANAΛYTIKA", "portion: " + portion);
                Log.d("SMS ANAΛYTIKA", "message: " + message);
            }
        }catch(StringIndexOutOfBoundsException e){
            Log.e("End of parsing","---");
        }

        return portions;
    }

    public ArrayList<String> getMinutesWhatsUpAnalysis(String message){

        ArrayList<String> portions = new ArrayList<String>();
        try {
            while (message.replace(" ", "") != "") {
                int start = message.indexOf("(");
                int end = message.indexOf(")");
                String portion = message.substring(start, end);
                portions.add(portion);
                message = message.substring(end + 1, message.length());
                Log.d("Minutes ANAΛYTIKA", "portion: " + portion);
                Log.d("Minutes ANAΛYTIKA", "message: " + message);
            }
        }catch(StringIndexOutOfBoundsException e){
            Log.e("End of parsing","---");
        }

        return portions;
    }

    public ContactInfo getContactInfo(String contact_number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact_number));
        String contact_name;
        long contact_ID;
        ContactInfo contact = null;

        ContentResolver contentResolver = receiver_context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                String name_retrieved = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                Log.e("NAMEEE",name_retrieved);
                if(name_retrieved!=null)
                    contact_name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                else
                    contact_name = contact_number;

                contact_ID = contactLookup.getLong(contactLookup.getColumnIndex(ContactsContract.Data._ID));

                contact = new ContactInfo(contact_name,contact_number,contact_ID);

            }
            else{
                contact = new ContactInfo(contact_number,contact_number,-1);
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return contact;
    }

    public void writeSMSToDB(String sender, String msg){

        ContentValues values = new ContentValues();
        values.put("address", sender);
        values.put("body", msg);
        values.put("read", false);
        receiver_context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);

    }

    public void handleCosmoteSMS(String message){
        String ypoloipo = "-",lixi = "-";

        try{
            String[] tokens = message.split(" ");
            ypoloipo = tokens[1];
            lixi = tokens[3];
        }catch (Exception e){
            Log.e("CosmoteYpoloipo", "ERROR!!!");
        }
        Log.d("CosmoteYpoloipo","Ypol: "+ ypoloipo + " - " + lixi);
        //Toast.makeText(receiver_context,"Ypol: "+ ypoloipo,Toast.LENGTH_LONG).show();

        ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_YPOLOIPO] = true;
        insertWhatsUpValueToDB(ReceiveSMSService.YPOLOIPO,ypoloipo);
        global.setYpoloipo(ypoloipo);
        refreshWUStatsIfAllAreSet();
    }

    public void handleWhatsUpSMS(String message){
        String internet_mb="-",min2all = "-",sms2all = "-",smsWhatsup_total = "-",minWhatsup = "-";
        ArrayList<String> smsWhatsup_list = new ArrayList<String>();
        ArrayList<String> minWhatsup_list = new ArrayList<String>();
        String internet_mb_expires = "-";

        if(message.contains("ENEPΓOΠOIHΘHKE") || message.contains("ΦΟΙΤΗΤΙΚΗ") || message.contains("TO YΠOΛOIΠO ΣOY EINAI KATΩ AΠO 1E")){ // otan eneropoieitai yphresia apo tn efarmogh
            return;
        }

        if (message.contains("INTERNET")) {
            String[] tokens = message.split(" ");
            if(message.contains("ΔEN")){
                internet_mb = "0";
            }
            else if(message.contains("ANAΛYTIKA")) {
                internet_mb = tokens[2];
            }
            else{
                internet_mb = tokens[1];
                internet_mb_expires = tokens[tokens.length-2] + " " + tokens[tokens.length-1];
            }
            Log.d("WhatsUpStats","MB: "+ internet_mb + " - " + internet_mb_expires);
            //Toast.makeText(receiver_context,"MB: "+ internet_mb,Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_DATA] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.DATA,internet_mb);
            global.setInternet_mb(internet_mb);
            refreshWUStatsIfAllAreSet();
        }
        else if(message.contains("ΔEN EXEIΣ SMS KAI MB")){
            internet_mb = "0";
            smsWhatsup_total = "0";

            Log.d("MB:","0");
            Log.d("SMS pros whats up", "SMS: " + smsWhatsup_total);
            //Toast.makeText(receiver_context,"Lepta pros olous: "+ min2all,Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_DATA] = true;
            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2WU] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.DATA,internet_mb);
            insertWhatsUpValueToDB(ReceiveSMSService.SMSWHATSUP,smsWhatsup_total);
            global.setInternet_mb(internet_mb);
            global.setSmsWhatsup_total(smsWhatsup_total);
            refreshWUStatsIfAllAreSet();
        }
        else if(message.contains("ΠPOΣ OΛA TA ΔIKTYA")){
            String[] tokens = message.split(" ");
            if(message.contains("ΔEN")){
                min2all = "0";
            }
            else{
                min2all = tokens[2];
            }
            Log.d("Lepta pros olous","Min: "+ min2all);
            //Toast.makeText(receiver_context,"Lepta pros olous: "+ min2all,Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2ALL] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.MIN2ALL,min2all.replace("'",""));
            global.setMin2all(min2all.replace("'",""));
            refreshWUStatsIfAllAreSet();
        }
        else if(message.contains("SMS ΠPOΣ EΘNIKA ΔIKTYA")){
            String[] tokens = message.split(" ");
            if(message.contains("ΔEN")){
                sms2all = "0";
            }
            else{
                sms2all = tokens[1];
            }
            Log.d("SMS pros olous","Min: "+ sms2all);
            //Toast.makeText(receiver_context,"SMS pros olous: "+ sms2all,Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2ALL] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.SMS2ALL,sms2all);
            global.setSmsAll(sms2all);
            refreshWUStatsIfAllAreSet();
        }
        else if(message.contains("SMS ΠPOΣ")) {
            Log.d("SMS pros whats up", "IN");
            String[] tokens = message.split(" ");
            if (message.contains("ΔEN EXEIΣ SMS ΠPOΣ WHAT'S UP.")) {
                smsWhatsup_total = "0";
            } else if(message.contains("ANAΛYTIKA") && message.contains("WHAT'S UP")) {
                smsWhatsup_total = tokens[2];

                smsWhatsup_list = getSMSWhatsUpAnalysis(message);

            }else{
                smsWhatsup_total = tokens[1];
            }
            Log.d("SMS pros whats up", "SMS: " + smsWhatsup_total);
            //Toast.makeText(receiver_context, "SMS pros whats up: " + smsWhatsup_total, Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2WU] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.SMSWHATSUP,smsWhatsup_total);
            global.setSmsWhatsup_total(smsWhatsup_total);
            refreshWUStatsIfAllAreSet();
        }
        else if(message.contains("' ΠPOΣ") && message.contains("WHAT'S UP")) {
            Log.d("Lepta pros whats up", "IN");
            String[] tokens = message.split(" ");
            if (message.contains("ΔEN EXEIΣ ΛEΠTA ΠPOΣ WHAT'S UP")) {
                minWhatsup = "0";
            } else if(message.contains("ANAΛYTIKA")){
                minWhatsup = tokens[2].replace("'", "");

                minWhatsup_list = getMinutesWhatsUpAnalysis(message);
            }else{
                minWhatsup = tokens[1].replace("'", "");
            }
            Log.d("Lepta pros whats up", "LEpta: " + minWhatsup);
            //Toast.makeText(receiver_context, "Lepta pros whats up: " + minWhatsup, Toast.LENGTH_LONG).show();

            ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2WU] = true;
            insertWhatsUpValueToDB(ReceiveSMSService.MINWHATSUP,minWhatsup);
            global.setMinWhatsup(minWhatsup);
            refreshWUStatsIfAllAreSet();
        }
    }

    public void refreshWUStatsIfAllAreSet(){
        //Log.d("refreshWUStatsIfAllAreSet","IS CALLED");
        // an exw kai ta 6 pedia
        if(ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_YPOLOIPO] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2WU] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2WU] &&
                ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2ALL] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_DATA] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2ALL]){
            if(MainActivity.active){
                Log.d("SMSManage","onHandleIntent called");
                Intent in=new Intent(ReceiveSMSService.RESULT_REFRESH_WU_STATS);  //you can put anything in it with putExtra
                in.putExtra("ui_refresh_whats_up",true);
                Log.d("SMSManage","sending broadcast");
                LocalBroadcastManager.getInstance(receiver_context).sendBroadcast(in);
                for(int i=0;i<ReceiveSMSService.receivedSMSFromWhatsUp.length;i++)
                    ReceiveSMSService.receivedSMSFromWhatsUp[i] = false;
            }
        }
        // an den exw to sms2all
        if(ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_YPOLOIPO] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2WU] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_SMS2WU] &&
                ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_MIN2ALL] && ReceiveSMSService.receivedSMSFromWhatsUp[ReceiveSMSService.TYPE_DATA]){
            if(MainActivity.active){
                Log.d("SMSManage","onHandleIntent called");
                Intent in=new Intent(ReceiveSMSService.RESULT_REFRESH_WU_STATS);  //you can put anything in it with putExtra
                in.putExtra("ui_refresh_whats_up",true);
                Log.d("SMSManage","sending broadcast");
                LocalBroadcastManager.getInstance(receiver_context).sendBroadcast(in);
                for(int i=0;i<ReceiveSMSService.receivedSMSFromWhatsUp.length;i++)
                    ReceiveSMSService.receivedSMSFromWhatsUp[i] = false;
            }
        }

    }

    public void insertWhatsUpValueToDB(String value_type, String value){
        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(receiver_context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, value_type);
        cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE, value);

        Log.d("insertWhatsUpValueToDB","value_type - " + value_type);
        Log.d("insertWhatsUpValueToDB","value - " + value);

        String [] valueArray={value};
        String sql = "SELECT * FROM "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+" = '" + value_type + "'";
        Cursor data = db.rawQuery(sql, null);
        if( db.getVersion()==0)
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME, WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        else

        if (data.moveToFirst()) {
            // record exists
            db.execSQL("UPDATE "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" SET "+WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE+"='"+value+"' WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+"='"+value_type+"'");
            Log.d("UPDATE",valueArray[0]);
        } else {
            // record not found
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME,WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        }
        data.close();
        db.close();
    }
}