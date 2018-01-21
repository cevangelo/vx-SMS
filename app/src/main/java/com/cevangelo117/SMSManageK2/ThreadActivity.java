package com.cevangelo117.SMSManageK2;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Vagelis on 18/10/2014.
 */
public class ThreadActivity extends Activity {

    boolean fromOrientation=false;
    SharedPreferences myPrefLogin;
    SharedPreferences.Editor prefsEditor;

    private String contact_name;
    private String contact_number;
    private int thread_id;
    private long contact_ID;

    static boolean active = false;
    ArrayList<String> sent_msgs = new ArrayList<String>();
    ArrayList<String> inconimg_msgs = new ArrayList<String>();
    ArrayList<String> datesOfMessages = new ArrayList<String>();
    short[] typeOfMessages;
    public static int LAST_ROW_ID = 0;

    private static TextView counter_tv;
    private static String SENT = "SMS_SENT";
    private static String DELIVERED = "SMS_DELIVERED";
    private static final int SMS_INCOMING = 1;
    private static final int SMS_SENT = 2;
    private static int MAX_SMS_MESSAGE_LENGTH = 160;


    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            counter_tv = (TextView) findViewById(R.id.counter_textview);
            if(s.length()<MAX_SMS_MESSAGE_LENGTH)
                counter_tv.setText(Integer.toString(s.length()));
            else{
                int num_of_msgs = s.length()/MAX_SMS_MESSAGE_LENGTH;
                counter_tv.setText(Integer.toString(s.length()-num_of_msgs*MAX_SMS_MESSAGE_LENGTH) + "/" + Integer.toString(num_of_msgs));
            }

        }

        public void afterTextChanged(Editable s) {
        }
    };

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra("new_msg");
            Log.e("IN RECEIVE", s);
            addIncomingMessageToThread(s);
            final ScrollView scrollview = ((ScrollView) findViewById(R.id.scroll_view_thread));
            scrollview.post(new Runnable() {
                @Override
                public void run() {
                    scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
            // Get instance of Vibrator from current Context
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            // Start without a delay
            // Each element then alternates between vibrate, sleep, vibrate, sleep...
            long[] pattern = {0, 100, 50, 50};

            // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
            v.vibrate(pattern, -1);
            //recreate();

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalClass g = (GlobalClass) getApplicationContext();
/*        if (g.isThemeNight()) {
            setTheme(R.style.ThemeNight);
        } else {
            setTheme(R.style.ThemeDay);
        }*/
        if(getThemeStateDB()!=null)
            if(getThemeStateDB().endsWith("DAY")) {
                g.setThemeNight(false);
                setTheme(R.style.ThemeDay);
            }
            else {
                g.setThemeNight(true);
                setTheme(R.style.ThemeNight);
            }
        setContentView(R.layout.thread_layout);

        myPrefLogin = this.getSharedPreferences("myPrefs", Context.MODE_WORLD_READABLE);
        prefsEditor = myPrefLogin.edit();
        fromOrientation = myPrefLogin.getBoolean("fromOrient", false);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Get intent that started this activity.
        Intent intent = getIntent();

        //Extract the extra passed with the above intent(that comes from the ACTION_SENDTO by other apps)
        if (Intent.ACTION_SENDTO.equals(intent.getAction())) {
            String phone_number = PhoneNumberUtils.stripSeparators(intent.getDataString().replace("smsto:", "").replace("%20", ""));
            System.out.println("intent.getDataString() = " + intent.getDataString());
            ContactInfo contact =  getContactIDAndDisplayName(phone_number);
            contact_name = contact.getContact_name();
            contact_number = intent.getDataString().replace("smsto:", "").replace("%20", "");
            System.out.println("contact_number = " + contact_number);
            thread_id =  getThread_ID(contact_number);
            contact_ID = contact.getContact_ID();
        }
        //Extract the extra passed with the above intent(that comes from the MainActivity List with threads)
        else{
            contact_name = intent.getStringExtra("contact_name");
            contact_number = intent.getStringExtra("contact_number");
            thread_id = intent.getIntExtra("thread_id", -1);
            contact_ID = intent.getLongExtra("contact_ID",-1);
        }

        // if you want cancel notification that was reffered to this thread (the notification id is the same as thread_id as I have declared in SMSReceiver)
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(thread_id);

        g.setCurrent_thread_id(thread_id);

        System.out.println("contact_ID = " + contact_ID);
        try {
            InputStream input = openPhoto(contact_ID);
            if(input != null) {
                Bitmap contact_bmp = BitmapFactory.decodeStream(input);
                ImageView thumbnail_imgv = (ImageView) findViewById(R.id.contact_thumb_thread);
                thumbnail_imgv.setImageBitmap(contact_bmp);
            }
            else{ // in case the contact exists but has no photo assigned
                ImageView thumbnail_imgv = (ImageView) findViewById(R.id.contact_thumb_thread);
                thumbnail_imgv.setImageResource(R.drawable.contact_icon);
            }
        } catch (IllegalArgumentException e) {
            ImageView thumbnail_imgv = (ImageView) findViewById(R.id.contact_thumb_thread);
            thumbnail_imgv.setImageResource(R.drawable.contact_icon);
        }

        // Prosthiki onomatos epafis panw panw
        TextView name_tv = (TextView) findViewById(R.id.contact_name_thread);
        name_tv.setTextSize(2, 25);// 2 gia sp
        if(contact_name!=null)
            name_tv.setText(contact_name);
        else
            name_tv.setText(contact_number);

    }

    public InputStream openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = getContentResolver().query(photoUri,
                new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public int getThread_ID(String phone_number) {
        Uri mSmsinboxQueryUri = Uri.parse("content://mms-sms/conversations/");
        Cursor cursor1 = getContentResolver().query(mSmsinboxQueryUri, new String[]{"thread_id", "address"}, null, null, null);
        String[] columns = new String[] { "address", "thread_id" };
        if (cursor1.getCount() > 0) {
            while (cursor1.moveToNext()){
                String address = cursor1.getString(cursor1.getColumnIndex(columns[0]));
                String thread_id = cursor1.getString(cursor1.getColumnIndex(columns[1]));
                System.out.println("thread_id = " + thread_id);
                System.out.println("address = " + address);
                if(address.equals(phone_number))
                    return Integer.parseInt(thread_id);
            }
        }
        return -1;
    }

    public ContactInfo getContactIDAndDisplayName(String phone_number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
        String contact_name = phone_number;
        long contact_ID = -1;

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                contact_name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                contact_ID = contactLookup.getLong(contactLookup.getColumnIndex(ContactsContract.Data._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        ContactInfo temp_contact = new ContactInfo(contact_name,phone_number,contact_ID);
        return  temp_contact;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("sent_msgs",sent_msgs);
        outState.putStringArrayList("inconimg_msgs", inconimg_msgs);
        outState.putStringArrayList("datesOfMessages", datesOfMessages);
        outState.putShortArray("typeOfMessages", typeOfMessages);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            sent_msgs = savedInstanceState.getStringArrayList("sent_msgs");
            inconimg_msgs = savedInstanceState.getStringArrayList("inconimg_msgs");
            datesOfMessages = savedInstanceState.getStringArrayList("datesOfMessages");
            typeOfMessages = savedInstanceState.getShortArray("typeOfMessages");
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        prefsEditor.putBoolean("fromOrient", true);
        prefsEditor.commit();
        return null;
    }

    @Override
    protected void onDestroy() {
        if(fromOrientation) {
            prefsEditor.putBoolean("fromOrient", false);
            prefsEditor.commit();
        }
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter iff= new IntentFilter(ReceiveSMSService.RESULT_SMS_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        prefsEditor.putBoolean("fromOrient", false);
        prefsEditor.commit();
        active = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
        super.onDestroy();
    }
    @Override
    protected void onResume(){
        super.onResume();
        GlobalClass g = (GlobalClass) getApplicationContext();
        long start_time = System.currentTimeMillis();// ------TIME COUNTER
        if(thread_id!=-1) { // ean yparxei hdh thread
            long finish_time = System.currentTimeMillis();// -----TIME COUNTER
            if(fromOrientation) {
                // do as per need--- logic of orientation changed.
                getThreadMessagesOrientationChanged();
            } else {
                // do as per need--- logic of default onCreate().
                getThreadMessages(thread_id);
            }
            long loading_time = finish_time - start_time;
            Log.d("Loading-Thread Time: ", String.valueOf(loading_time) + " ms");
        }

        final ScrollView scrollview = ((ScrollView) findViewById(R.id.scroll_view_thread));
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        final EditText new_msg_edittext = (EditText) findViewById(R.id.new_message_edittext);
        new_msg_edittext.addTextChangedListener(mTextEditorWatcher);
        Button send_btn = (Button) findViewById(R.id.send_button);

        send_btn.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        String message = new_msg_edittext.getText().toString();
                        Log.v("EditText", new_msg_edittext.getText().toString());

                        sendMessage(contact_number, message);
                        new_msg_edittext.getEditableText().clear();
                        final ScrollView scrollview = ((ScrollView) findViewById(R.id.scroll_view_thread));
                        scrollview.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                });
        if (!g.isThemeNight()) {
            new_msg_edittext.setTextColor(Color.WHITE);
            send_btn.setTextColor(Color.WHITE);
        }
    }

    // ---sends an SMS message to another device---
    public void sendMessage(String phoneNumber, String message) {

        if(message.isEmpty() || message.replace(" ","").isEmpty()){
            Toast.makeText(getApplicationContext(), "Empty SMS!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if(phoneNumber != null) {
            PendingIntent piSent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(SENT), 0);
            PendingIntent piDelivered = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(DELIVERED), 0);
            SmsManager smsManager = SmsManager.getDefault();

            int length = message.length();
            try {
                if (length > MAX_SMS_MESSAGE_LENGTH) {
                    ArrayList<String> messagelist = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(phoneNumber, null, messagelist, null, null);
                    Toast.makeText(getApplicationContext(), "More than one SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, piSent, piDelivered);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                }
                addSentMessageToDB(phoneNumber,message);
                addSentMessageToThread(message);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),
                        "SMS failed, please try again.",
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(getApplicationContext(),
                    "The contact does not have a phone number",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void addSentMessageToDB(String phoneNumber, String message){
        ContentValues values = new ContentValues();
        values.put("address", phoneNumber);
        values.put("body", message);
        values.put("read", 1);
        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }

    public void addIncomingMessageToThread(String message) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.thread_table);
        TableRow tableRow = new TableRow(this);
        tableRow.setId(LAST_ROW_ID+1);
        TableRow.LayoutParams table_params = new TableRow.LayoutParams(0,TableLayout.LayoutParams.WRAP_CONTENT,1.0f);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.bubble_with_date,null);

        TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
        message_txt.setText(message);
        message_txt.setTextIsSelectable(true);

        String date_str = getFormattedTimeAndDate(System.currentTimeMillis());

        TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
        date_txt.setText(date_str);

        TextView empty_tv = new TextView(this);
        empty_tv.setText("");
        empty_tv.setLayoutParams(table_params);

        v.setBackgroundResource(R.drawable.custom_sms_bubble);
        v.setLayoutParams(table_params);

        tableRow.addView(v);
        tableRow.addView(empty_tv);
        tableLayout.addView(tableRow);

    }

    public void addSentMessageToThread(String message) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.thread_table);
        TableRow tableRow = new TableRow(this);
        tableRow.setId(LAST_ROW_ID+1);
        TableRow.LayoutParams table_params = new TableRow.LayoutParams(0,TableLayout.LayoutParams.WRAP_CONTENT,1.0f);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.bubble_with_date, null);

        TextView empty_tv = new TextView(this);
        empty_tv.setText("");
        empty_tv.setLayoutParams(table_params);

        TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
        message_txt.setText(message);
        message_txt.setTextIsSelectable(true);

        String date_str = getFormattedTimeAndDate(System.currentTimeMillis());

        TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
        date_txt.setText(date_str);

        v.setBackgroundResource(R.drawable.custom_sms_bubble);
        v.setLayoutParams(table_params);

        tableRow.addView(empty_tv);
        tableRow.addView(v);
        tableLayout.addView(tableRow);
    }

    public void getThreadMessages(int thread_id) {

        TableLayout tableLayout = (TableLayout) findViewById(R.id.thread_table);
        // ean to table layout gia kapoio logo exei paidia afairese ta (ypirxe thema me diplotypa)
        if(tableLayout.getChildCount()>0) {
            tableLayout.removeAllViews();
        }
        int mItemsOnPage=100;

        // Create Inbox box URI
        Uri smsURI = Uri.parse("content://sms/");

        // List required columns
        String[] reqCols = new String[] { "body", "thread_id", "date", "_id", "read", "type"};//{ "_id", "address", "body" , "date" };

        // Selection
        String selection = "thread_id" + " = '" + thread_id + "'";

        // Sort oder and limit
        String sortOrder = "date DESC";
        String limit = "LIMIT " + String.valueOf(mItemsOnPage);

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(smsURI, reqCols, selection, null, sortOrder + " " + limit);

        //startManagingCursor(c);
        LAST_ROW_ID = c.getCount()-1;
        // Read the sms data and store it in the list
        if(c.moveToLast()) {
            typeOfMessages = new short[mItemsOnPage];
            for(int i=0; i < c.getCount(); i++) {

                String msg = c.getString(c.getColumnIndexOrThrow("body")).toString();
                int id = c.getInt(c.getColumnIndexOrThrow("thread_id"));
                int read = c.getInt(c.getColumnIndexOrThrow("read"));
                Log.d("read", Integer.toString(read));
                int type = c.getInt(c.getColumnIndexOrThrow("type"));
                Log.e("TYPE", (Integer.toString(type)));

                if(read == 0){//an kapoio mnm eiani unread kanto read afou mpikame st thread
                    String SmsMessageId = c.getString(c.getColumnIndex("_id"));
                    ContentValues values = new ContentValues();
                    values.put("read", true);
                    this.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                }

                TableRow tableRow = new TableRow(this);
                tableRow.setId(i);
                TableRow.LayoutParams table_params = new TableRow.LayoutParams(0,TableLayout.LayoutParams.WRAP_CONTENT,1.0f);
                LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.bubble_with_date,null);
                if(type == SMS_SENT){

                    sent_msgs.add(msg);
                    typeOfMessages[i] = SMS_SENT;
                    TextView empty_tv = new TextView(this);
                    empty_tv.setText("");
                    empty_tv.setLayoutParams(table_params);

                    TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
                    message_txt.setText(msg);
                    message_txt.setTextIsSelectable(true);

                    String date_str = getFormattedTimeAndDate(c.getLong(c.getColumnIndexOrThrow("date")));
                    datesOfMessages.add(date_str);

                    TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
                    date_txt.setText(date_str);

                    v.setBackgroundResource(R.drawable.custom_sms_bubble);
                    v.setLayoutParams(table_params);

                    tableRow.addView(empty_tv);
                    tableRow.addView(v);

                }
                else if(type == SMS_INCOMING){

                    inconimg_msgs.add(msg);
                    typeOfMessages[i] = SMS_INCOMING;
                    TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
                    message_txt.setText(msg);
                    message_txt.setTextIsSelectable(true);

                    String date_str = getFormattedTimeAndDate(c.getLong(c.getColumnIndexOrThrow("date")));
                    datesOfMessages.add(date_str);

                    TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
                    date_txt.setText(date_str);

                    TextView empty_tv = new TextView(this);
                    empty_tv.setText("");
                    empty_tv.setLayoutParams(table_params);

                    v.setBackgroundResource(R.drawable.custom_sms_bubble);
                    v.setLayoutParams(table_params);

                    tableRow.addView(v);
                    tableRow.addView(empty_tv);
                }

                tableLayout.addView(tableRow);

                Log.d("thread_ID", Integer.toString(id));
                //Log.d("type", Integer.toString(type));
                Log.d("msg", msg );


                c.moveToPrevious();
            }
        }
    }

    public void getThreadMessagesOrientationChanged() {

        TableLayout tableLayout = (TableLayout) findViewById(R.id.thread_table);
        int sent_counter=0,incoming_counter=0;
        for(int i=0; i < typeOfMessages.length; i++) {

            TableRow tableRow = new TableRow(this);
            tableRow.setId(i);
            TableRow.LayoutParams table_params = new TableRow.LayoutParams(0,TableLayout.LayoutParams.WRAP_CONTENT,1.0f);
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.bubble_with_date,null);
            if(typeOfMessages[i] == SMS_SENT){

                TextView empty_tv = new TextView(this);
                empty_tv.setText("");
                empty_tv.setLayoutParams(table_params);

                TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
                message_txt.setText(sent_msgs.get(sent_counter));
                sent_counter++;
                message_txt.setTextIsSelectable(true);

                String date_str = datesOfMessages.get(i);

                TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
                date_txt.setText(date_str);

                v.setBackgroundResource(R.drawable.custom_sms_bubble);
                v.setLayoutParams(table_params);

                tableRow.addView(empty_tv);
                tableRow.addView(v);

            }
            else if(typeOfMessages[i] == SMS_INCOMING){

                TextView message_txt = (TextView) v.findViewById(R.id.sms_text);
                message_txt.setText(inconimg_msgs.get(incoming_counter));
                incoming_counter++;
                message_txt.setTextIsSelectable(true);

                String date_str = datesOfMessages.get(i);

                TextView date_txt = (TextView) v.findViewById(R.id.sms_date);
                date_txt.setText(date_str);

                TextView empty_tv = new TextView(this);
                empty_tv.setText("");
                empty_tv.setLayoutParams(table_params);

                v.setBackgroundResource(R.drawable.custom_sms_bubble);
                v.setLayoutParams(table_params);

                tableRow.addView(v);
                tableRow.addView(empty_tv);
            }
            tableLayout.addView(tableRow);
        }
    }

    public String getFormattedTimeAndDate(long time_of_sms){
        String date = String.valueOf(DateUtils.getRelativeDateTimeString(this, time_of_sms, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        return date;
        //Log.d("DATE: ", date);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Intent intent = new Intent();
        intent.setClass(ThreadActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // clears all the MainActivity instances on the stack and creates a new one
        startActivity(intent);
        finish();

    }

    private String getThemeStateDB() {
        String themeState = null;
        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String sql = "SELECT * FROM " + WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME;
        Cursor data = db.rawQuery(sql, null);

        if (data.moveToFirst()) {
            for (int i = 0; i < data.getCount(); i++) {
                String stat_type = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE));
                String stat_value = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE));
                if (stat_type.endsWith(MainActivity.THEME)) {
                    themeState = stat_value;
                }
                //Log.d("getThemeStateDB - stat_type", stat_type);
                //Log.e("getThemeStateDB - stat_value", stat_value);

                data.moveToNext();
            }
        }
        data.close();
        db.close();

        return themeState;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }
}
