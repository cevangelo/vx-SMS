package com.cevangelo117.SMSManageK2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.cevangelo117.SMSManageK2.GlobalClass.getAppContext;

public class MainActivity extends FragmentActivity
{


    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isRefreshing = intent.getBooleanExtra("ui_refresh_whats_up",false);
            Log.d("IN RECEIVE", (isRefreshing) ? "refreshing" : "NOT refreshing");
            refreshWhatsUpStats();
            setWhatsUpTextViewsToGREENColor();
        }
    };

    public static final String NOTIFY = "NOTIFY";
    public static final String THEME = "THEME_STATE";
    static boolean active = false;
    static boolean isOKPressed = false;
    static boolean isCheckednotNotifing = false;
    static View checkBoxView;
    public static LinearLayout main_view;
    GlobalClass g;

    // A list that has the information of the threads
    private ArrayList<ThreadInfo> sms_contacts_list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        g = (GlobalClass) getApplicationContext();
        if(getThemeStateDB()!=null)
            if(getThemeStateDB().endsWith("DAY"))
                g.setThemeNight(false);
            else
                g.setThemeNight(true);
        if(g.isChangeOfTheme()) {
            if (g.isThemeNight()) {
                setTheme(R.style.ThemeDay);
                g.setThemeNight(false);
                updateDBwithThemeSetting("DAY");
                System.out.println("DAY");
            } else {
                setTheme(R.style.ThemeNight);
                g.setThemeNight(true);
                updateDBwithThemeSetting("NIGHT");
                System.out.println("NIGHT");
            }
            g.setChangeOfTheme(false);
        }
        if (!g.isThemeNight())
            setTheme(R.style.ThemeDay);
        setContentView(R.layout.main);

        // set the what up stats from the DB
        refreshWhatsUpStatsDB();

        // start the Service responsible for the sms receiving
        startService(new Intent(this, ReceiveSMSService.class));

        //The list of the Threads in the main activity
        ListView sms_contacts_lv = (ListView) findViewById(R.id.sms_contacts);

        // Get all the info for the threads that exist and put in a list of ThreadInfo objects
        sms_contacts_list = getContactThumbAndDisplayName(getSMSContacts());

        // and pass the list in custom adapter we made
        ThreadInfoAdapter adapter = new ThreadInfoAdapter(this,sms_contacts_list);
        // set each item to be formatted according to our adapter
        sms_contacts_lv.setAdapter(adapter);

        sms_contacts_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                //Toast.makeText(MainActivity.this,"id: "+id,Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                i.setClass(MainActivity.this, ThreadActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                ThreadInfo selected_thread = sms_contacts_list.get(position);
                i.putExtra("contact_name",selected_thread.getContact().getContact_name());
                i.putExtra("contact_number",selected_thread.getContact().getContact_number());
                i.putExtra("thread_id",selected_thread.getThread_id());
                i.putExtra("contact_ID",selected_thread.getContact().getContact_ID());
                startActivity(i);
            }
        });

        registerForContextMenu(sms_contacts_lv);

        main_view = (LinearLayout) findViewById(R.id.main_view);
    }

    public static void updateDBwithNotifingSetting(boolean notify){
        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(getAppContext());
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, NOTIFY);
        String value;
        if(notify) {
            value = "DO NOT NOTIFY";
            cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE, value);
        }
        else {
            value = "NOTIFY";
            cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE, value);
        }


        Log.d("insertWhatsUpValueToDB","value_type - " + NOTIFY);
        Log.d("insertWhatsUpValueToDB","value - " + value);

        String [] valueArray={value};
        String sql = "SELECT * FROM "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+" = '" + NOTIFY + "'";
        Cursor data = db.rawQuery(sql, null);
        if( db.getVersion()==0)
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME, WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        else

        if (data.moveToFirst()) {
            // record exists
            db.execSQL("UPDATE "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" SET "+WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE+"='"+value+"' WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+"='"+NOTIFY+"'");
            Log.d("UPDATE",valueArray[0]);
        } else {
            // record not found
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME,WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        }
        data.close();
        db.close();
    }

    @SuppressLint("ValidFragment")
    public static class SendToCosmoteDialogFragment extends DialogFragment {

        public SendToCosmoteDialogFragment() {
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            GlobalClass g = (GlobalClass) getAppContext();
            checkBoxView = View.inflate(getAppContext(), R.layout.not_checkbox, null);
            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    // Save to shared preferences
                    isCheckednotNotifing = isChecked;
                }
            });
            if (!g.isThemeNight()) {
                checkBox.setTextColor(Color.BLACK);
            }
            checkBox.setText("Do not notify me again.");
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage("We are sending an SMS to Cosmote that is free of charge. Is that ok?")
                    .setView(checkBoxView)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            isOKPressed = true;
                            sendSMSToWhatsUp();
                            setWhatsUpTextViewsToREDColor();
                            updateDBwithNotifingSetting(isCheckednotNotifing);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            isOKPressed = false;
                            updateDBwithNotifingSetting(isCheckednotNotifing);
                            //Toast.makeText(this,"Send to Cosmote CANCELED",Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private String getNotifyStateDB() {
        String notifyState = null;
        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String sql = "SELECT * FROM " + WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME;
        Cursor data = db.rawQuery(sql, null);

        if (data.moveToFirst()) {
            for (int i = 0; i < data.getCount(); i++) {
                String stat_type = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE));
                String stat_value = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE));
                if (stat_type.endsWith(NOTIFY)) {
                    notifyState = stat_value;
                }
                Log.d("getNotifyStateDBstattyp", stat_type);
                Log.e("getNotifyStateDBstatval", stat_value);

                data.moveToNext();
            }
        }
        data.close();
        db.close();

        return notifyState;
    }

    private boolean doNotNotifyUserPermanent() {
        String notifyState = getNotifyStateDB();
        if (notifyState!=null && notifyState.endsWith("DO NOT NOTIFY")) {
            return true;
        }
        else
            return false;
    }

    public void updateDBwithThemeSetting(String theme){

        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(this);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, THEME);
        String value;
        if(theme.endsWith("NIGHT")) {
            value = "NIGHT";
            cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE, value);
        }
        else {
            value = "DAY";
            cv.put(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE, value);
        }


/*        Log.d("updateDBwithThemeSetting","value_type - " + THEME);
        Log.d("updateDBwithThemeSetting","value - " + value);*/

        String [] valueArray={value};
        String sql = "SELECT * FROM "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+" = '" + THEME + "'";
        Cursor data = db.rawQuery(sql, null);
        if( db.getVersion()==0)
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME, WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        else

        if (data.moveToFirst()) {
            // record exists
            db.execSQL("UPDATE "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME+" SET "+WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE+"='"+value+"' WHERE "+WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE+"='"+THEME+"'");
            Log.d("UPDATE",valueArray[0]);
        } else {
            // record not found
            db.insert(WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME,WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE, cv);
        }
        data.close();
        db.close();
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
                if (stat_type.endsWith(THEME)) {
                    themeState = stat_value;
                }
             /*   Log.d("getThemeStateDB - stat_type", stat_type);
                Log.e("getThemeStateDB - stat_value", stat_value);*/

                data.moveToNext();
            }
        }
        data.close();
        db.close();

        return themeState;
    }

    private static void sendSMSToWhatsUp() {
            SmsManager smsManager = SmsManager.getDefault();
            try {
                smsManager.sendTextMessage("1330", null, "Y", null, null);
                smsManager.sendTextMessage("1314", null, "YP", null, null);
                Toast.makeText(getAppContext(), "WhatsUP SMS sent.",
                        Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                Toast.makeText(getAppContext(),
                        "Whats Up SMS failed, please try again.",
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
    }

    public static void setWhatsUpTextViewsToREDColor(){

        TextView ypol_tv = (TextView) main_view.findViewById(R.id.ypol_UI);
        ypol_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

        TextView min2wu_tv = (TextView) main_view.findViewById(R.id.min2wu_UI);
        min2wu_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

        TextView sms2wu_tv = (TextView) main_view.findViewById(R.id.sms2wu_UI);
        sms2wu_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

        TextView min2all_tv = (TextView) main_view.findViewById(R.id.min2all_UI);
        min2all_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

        TextView sms2all_tv = (TextView) main_view.findViewById(R.id.sms2all_UI);
        sms2all_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

        TextView data_tv = (TextView) main_view.findViewById(R.id.data_UI);
        data_tv.setTextColor(GlobalClass.getAppContext().getResources().getColor(R.color.red));

    }

    public void setWhatsUpTextViewsToGREENColor(){

        TextView ypol_tv = (TextView) findViewById(R.id.ypol_UI);
        ypol_tv.setTextColor(getResources().getColor(R.color.green));

        TextView min2wu_tv = (TextView) findViewById(R.id.min2wu_UI);
        min2wu_tv.setTextColor(getResources().getColor(R.color.green));

        TextView sms2wu_tv = (TextView) findViewById(R.id.sms2wu_UI);
        sms2wu_tv.setTextColor(getResources().getColor(R.color.green));

        TextView min2all_tv = (TextView) findViewById(R.id.min2all_UI);
        min2all_tv.setTextColor(getResources().getColor(R.color.green));

        TextView sms2all_tv = (TextView) findViewById(R.id.sms2all_UI);
        sms2all_tv.setTextColor(getResources().getColor(R.color.green));

        TextView data_tv = (TextView) findViewById(R.id.data_UI);
        data_tv.setTextColor(getResources().getColor(R.color.green));

    }

    public void refreshWhatsUpStats() {

        GlobalClass g = (GlobalClass) getApplicationContext();

        TextView ypol_tv = (TextView) findViewById(R.id.ypol_UI);
        ypol_tv.setText(g.getYpoloipo());

        TextView min2wu_tv = (TextView) findViewById(R.id.min2wu_UI);
        min2wu_tv.setText(g.getMinWhatsup());

        TextView sms2wu_tv = (TextView) findViewById(R.id.sms2wu_UI);
        sms2wu_tv.setText(g.getSmsWhatsup_total());

        TextView min2all_tv = (TextView) findViewById(R.id.min2all_UI);
        min2all_tv.setText(g.getMin2all() + "'");

        TextView sms2all_tv = (TextView) findViewById(R.id.sms2all_UI);
        sms2all_tv.setText(g.getSmsAll());

        TextView data_tv = (TextView) findViewById(R.id.data_UI);
        data_tv.setText(g.getInternet_mb()+" MB");
    }

    /**
     * A function that retrieves the WhatsUp Info from the database and sets them to the suitable TextView
     */
    private void refreshWhatsUpStatsDB() {
        GlobalClass g = (GlobalClass) getApplicationContext();
        WhatsUpDBOpenHelper databaseHelper = new WhatsUpDBOpenHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String sql = "SELECT * FROM "+WhatsUpDBOpenHelper.WHATSUP_STATS_TABLE_NAME;
        Cursor data = db.rawQuery(sql, null);

        if(data.moveToFirst()) {
            for(int i=0; i < data.getCount(); i++) {
                String stat_type = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_TYPE));
                String stat_value = data.getString(data.getColumnIndexOrThrow(WhatsUpDBOpenHelper.WHATSUP_STAT_VALUE));
                if(stat_type.endsWith(ReceiveSMSService.YPOLOIPO)){
                    TextView ypol_tv = (TextView) findViewById(R.id.ypol_UI);
                    ypol_tv.setText(stat_value);
                    g.setYpoloipo(stat_value);
                    Log.d("ypol_tv", "IN");
                }
                else if(stat_type.endsWith(ReceiveSMSService.MINWHATSUP)){
                    TextView min2wu_tv = (TextView) findViewById(R.id.min2wu_UI);
                    min2wu_tv.setText(stat_value);
                    g.setMinWhatsup(stat_value);
                    Log.d("min2wu_tv", "IN");
                }
                else if(stat_type.endsWith(ReceiveSMSService.SMSWHATSUP)){
                    TextView sms2wu_tv = (TextView) findViewById(R.id.sms2wu_UI);
                    sms2wu_tv.setText(stat_value);
                    g.setSmsWhatsup_total(stat_value);
                    Log.d("sms2wu_tv", "IN");
                }
                else if(stat_type.endsWith(ReceiveSMSService.MIN2ALL)){
                    TextView min2all_tv = (TextView) findViewById(R.id.min2all_UI);
                    min2all_tv.setText(stat_value);
                    g.setMin2all(stat_value);
                    Log.d("min2all_tv", "IN");
                }
                else if(stat_type.endsWith(ReceiveSMSService.SMS2ALL)){
                    TextView sms2all_tv = (TextView) findViewById(R.id.sms2all_UI);
                    sms2all_tv.setText(stat_value);
                    g.setSmsAll(stat_value);
                    Log.d("sms2all_tv", "IN");
                }
                else if(stat_type.endsWith(ReceiveSMSService.DATA)){
                    TextView data_tv = (TextView) findViewById(R.id.data_UI);
                    data_tv.setText(stat_value);
                    g.setInternet_mb(stat_value);
                    Log.d("data_tv", "IN");
                }
             /*   Log.d("refreshWhatsUpStatsDB - stat_type", stat_type);
                Log.e("refreshWhatsUpStatsDB - stat_value", stat_value);*/

                data.moveToNext();
            }
        }
        data.close();
        db.close();
    }

    public ArrayList<ThreadInfo> getSMSContacts() {

        ArrayList<String> sms_contacts = new ArrayList<String>();

        ArrayList<ThreadInfo> threads = new ArrayList<ThreadInfo>();

        // Create Inbox box URI
        Uri smsURI = Uri.parse("content://sms/");

        // List required columns
        String[] reqCols = new String[] { "DISTINCT address", "body", "thread_id", "date"};//{ "_id", "address", "body" , "date" };

        //Gia na exw ta threads me xronologiki seira.
        String sortOrder = "date DESC";

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(smsURI, reqCols, null, null, sortOrder);

        String temp_contact_number = "null_num";
        // Read the sms data and store it in the list
        if(c.moveToFirst()) {
            for(int i=0; i < c.getCount(); i++) {
                String contact_number = c.getString(c.getColumnIndexOrThrow("address")).toString().replace(" ", "").replace("+30", "");
                if(!contact_number.endsWith(temp_contact_number)) {
                    String last_msg = c.getString(c.getColumnIndexOrThrow("body")).toString();
                    int thread_id = c.getInt(c.getColumnIndexOrThrow("thread_id"));
                    long date_long = c.getLong(c.getColumnIndexOrThrow("date"));
                    String date_str = getFormattedTimeAndDate(date_long);
                    ThreadInfo temp_thread = new ThreadInfo(contact_number,last_msg,thread_id,date_str);

                    temp_contact_number = contact_number;
                    //Log.d("Thread_ID", Integer.toString(thread_id) );
                    if (!sms_contacts.contains(contact_number)) {
                        threads.add(temp_thread);
                        sms_contacts.add(contact_number);
                    }
                }

                c.moveToNext();
            }
        }

        return threads;
    }

    public String getFormattedTimeAndDate(long time_of_sms){
        String date = String.valueOf(DateUtils.getRelativeDateTimeString(this, time_of_sms, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,DateUtils.FORMAT_ABBREV_RELATIVE));
        return date;
        //Log.d("DATE: ", date);
    }

    public ArrayList<ThreadInfo> getContactThumbAndDisplayName(ArrayList<ThreadInfo> threadList) {
        ArrayList<ThreadInfo> tempThreadList = new ArrayList<ThreadInfo>();

        for(ThreadInfo thread: threadList) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(thread.getContact().getContact_number()));
            String contact_name = thread.getContact().getContact_number();
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

            thread.getContact().setContact_name(contact_name);
            thread.getContact().setContact_ID(contact_ID);

            tempThreadList.add(thread);
        }

        return tempThreadList;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ProgressDialog showProgress;
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete_thread_item:
                //showProgress = ProgressDialog.show(MainActivity.this, "","Deleting...", true);//Spinner starts...
                new Thread(new Runnable() {
                    public void run() {
                        deleteThread(info.position);
                        //showProgress.dismiss();
                    }
                }).start();
                recreate();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void deleteThread(int position) {

        ThreadInfo selected_thread = sms_contacts_list.get(position);
        String thread_id = String.valueOf(selected_thread.getThread_id());

        // Create Inbox box URI
        Uri smsURI = Uri.parse("content://sms/");

        // List required columns
        String[] reqCols = new String[] { "_id", "thread_id" };//{ "_id", "address", "body" , "date" };

        // Selection
        String selection = "thread_id" + " = '" + thread_id + "'";

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(smsURI, reqCols, selection, null, null);

        // Read the sms data and store it in the list
        if(c.moveToFirst()) {
            for(int i=0; i < c.getCount(); i++) {
                long msg_id = c.getLong(c.getColumnIndexOrThrow("_id"));
                long thread_iDD = c.getLong(c.getColumnIndexOrThrow("thread_id"));
                Log.d("Thread-ID: ", String.valueOf(thread_iDD));
                Log.e("Deleting SMS with id: ", String.valueOf(msg_id));
                cr.delete(Uri.parse("content://sms/" + msg_id), null, null);

                c.moveToNext();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter iff= new IntentFilter(ReceiveSMSService.RESULT_REFRESH_WU_STATS);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
            Intent intent =
                    new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    myPackageName);
            startActivity(intent);
        } else {
            // App is the default.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_theme_state:
                GlobalClass g = (GlobalClass) getApplicationContext();
                if(g.isThemeNight())
                    updateDBwithThemeSetting("NIGHT");
                else
                    updateDBwithThemeSetting("DAY");
                g.setChangeOfTheme(true);
                recreate();
                return true;
            case R.id.action_send_wu:
                if(doNotNotifyUserPermanent()){
                    sendSMSToWhatsUp();
                    setWhatsUpTextViewsToREDColor();
                }
                else{
                    SendToCosmoteDialogFragment notif_cosmote = new SendToCosmoteDialogFragment();
                    notif_cosmote.show(getSupportFragmentManager(), "NoticeDialogFragment");
                }
                return true;
            case R.id.action_refresh_wu:
                refreshWhatsUpStats();
                //refreshWhatsUpStatsDB();
                return true;
            case R.id.action_new_thread:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, NewThreadActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
