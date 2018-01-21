package com.cevangelo117.SMSManageK2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NavUtils;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;

/**
 * Created by Vagelis on 26/10/2014.
 */
public class NewThreadActivity extends Activity {
    private static TextView counter_tv;
    private static int MAX_SMS_MESSAGE_LENGTH = 160;
    private static String SENT = "SMS_SENT";
    private static String DELIVERED = "SMS_DELIVERED";
    ArrayList<ContactInfo> contacts_list ;
    ContactInfo selected_contact;

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            counter_tv = (TextView) findViewById(R.id.counter_textview_new_thread);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final GlobalClass g = (GlobalClass) getApplicationContext();
        /*if (g.isThemeNight()) {
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
        setContentView(R.layout.new_thead_layout);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        //contacts_list = getAllContacts();
        //ContactInfoAdapter adapter =new ContactInfoAdapter(this,R.layout.contact_autocomplete_item,contacts_list);
        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.pros_autocompleteview);
        autoCompleteTextView.setThreshold(2);
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {

             public void onTextChanged(final CharSequence s, int start, int before, int count) {
                 if (s.length() >= 2 )
                 {
                     new AsyncTask<String, Void, ArrayList<ContactInfo>>(){

                         @Override
                         protected ArrayList<ContactInfo> doInBackground(String... params) {
                             contacts_list = getSuggestedContacts(s.toString());
                             return contacts_list;
                         }

                         @Override
                         protected void onPostExecute(ArrayList<ContactInfo> contacts) {
                             ContactInfoAdapter adapter =new ContactInfoAdapter(NewThreadActivity.this,R.layout.contact_autocomplete_item,contacts);
                             adapter.notifyDataSetChanged();
                             autoCompleteTextView.setAdapter(adapter);
                         }
                     }.execute(s.toString());
                 }
             }

             public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

             public void afterTextChanged(Editable s) {
                 Log.d("After Text Changed",s.toString());
                 if(contacts_list!=null) {
                     for (ContactInfo contact : contacts_list) {
                         if (contact.getContact_name().equals(s.toString())) {
                             selected_contact = contact;
                             //Log.d("After Text Changed - selected_contact", selected_contact.getContact_name());
                             //sender = new ContactInfo(contacts_list.get(0).getContact_name(),contacts_list.get(0).getContact_number(),contacts_list.get(0).getContact_ID());
                         }
                     }
                 }
                 /*if(contacts_list!=null && contacts_list.size()!=0) { // an exei epilegei epafi ap tn lista
                     Log.d("After Text Changed - contact", contacts_list.get(0).getContact_name());
                     sender = new ContactInfo(contacts_list.get(0).getContact_name(),contacts_list.get(0).getContact_number(),contacts_list.get(0).getContact_ID());
                 }*/
                 else{
                     //Log.d("After Text Changed - NEW", s.toString());
                    // sender = new ContactInfo(s.toString(),s.toString(),-1);
                 }
             }
         }
        );

        final EditText new_msg_edittext = (EditText) findViewById(R.id.new_message_edittext_new_thread);
        new_msg_edittext.addTextChangedListener(mTextEditorWatcher);
        Button send_btn = (Button) findViewById(R.id.send_button_new_thread);

        send_btn.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        String message = new_msg_edittext.getText().toString();
                        //Log.v("EditText", new_msg_edittext.getText().toString());

                        //if message is empty show toast
                        if(message.isEmpty() || message.replace(" ","").isEmpty()){
                            Toast.makeText(getApplicationContext(), "Empty SMS!",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent();
                        intent.setClass(NewThreadActivity.this, ThreadActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //Send message and call ThreadActivity with suitable parameters
                        if (selected_contact != null) { // if the phone number belongs to someone from the contact list
                            Log.d("State_of_input_contact", "selected_contact-INNN");
                            sendMessage(selected_contact.getContact_number(), message);
                            intent.putExtra("contact_name", selected_contact.getContact_name());
                            intent.putExtra("contact_number", selected_contact.getContact_number());
                            intent.putExtra("thread_id", getThread_ID(selected_contact.getContact_number()));//intent.putExtra("thread_id", getThread_ID(selected_contact.getContact_number()));
                            intent.putExtra("contact_ID", selected_contact.getContact_ID());
                        } else if (isInteger(autoCompleteTextView.getEditableText().toString())) { // if the phone doesn't belong to somebody and it is a number
                            Log.d("State_of_input_contact", "autoCompleteTextView-INNN");
                            sendMessage(autoCompleteTextView.getEditableText().toString(), message);
                            intent.putExtra("contact_name", autoCompleteTextView.getEditableText().toString());
                            intent.putExtra("contact_number", autoCompleteTextView.getEditableText().toString());
                            //intent.putExtra("thread_id", getThreadID(autoCompleteTextView.getEditableText().toString()));
                        }
                        new_msg_edittext.getEditableText().clear();
                        startActivity(intent);
                    }

                });
        if (!g.isThemeNight()) {
            autoCompleteTextView.setTextColor(Color.WHITE);
            new_msg_edittext.setTextColor(Color.WHITE);
            send_btn.setTextColor(Color.WHITE);
        }
        //autoCompleteTextView.setAdapter(adapter);
        /*ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, getAllContacts());
        AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.pros_autocompleteview);
        textView.setAdapter(adapter);*/
    }


    public int getThread_ID(String phone_number) {
        Uri mSmsinboxQueryUri = Uri.parse("content://mms-sms/conversations/");
        Cursor cursor1 = getContentResolver().query(mSmsinboxQueryUri,new String[] { "thread_id", "address" }, null, null, null);
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
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    public int getThreadID(String name) {

        ArrayList<String> sms_contacts = new ArrayList<String>();

        ArrayList<ThreadInfo> threads = new ArrayList<ThreadInfo>();

        // Create Inbox box URI
        Uri smsURI = Uri.parse("content://sms/");

        // List required columns
        String[] reqCols = new String[] { "DISTINCT address", "thread_id", "body"};//{ "_id", "address", "body" , "date" };

        // Get Content Resolver object, which will deal with Content Provider
        ContentResolver cr = getContentResolver();

        // Fetch Inbox SMS Message from Built-in Content Provider
        Cursor c = cr.query(smsURI, reqCols, null, null, null);

        // Read the sms data and store it in the list
        if(c.moveToFirst()) {
            for(int i=0; i < c.getCount(); i++) {
                String contact_name = c.getString(c.getColumnIndexOrThrow("address")).toString().replace(" ","").replace("+30","");
                String message = c.getString(c.getColumnIndexOrThrow("body")).toString();
                int thread_id = c.getInt(c.getColumnIndexOrThrow("thread_id"));
                if(name.equals(contact_name)){
                    Log.d("Name From DB", contact_name);
                    Log.d("Name Passes", name);
                    return thread_id;
                }


                c.moveToNext();
            }
        }
        return -1;
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

    public ArrayList<ContactInfo> getSuggestedContacts(String user_input) {

        ArrayList<ContactInfo> tempContactList = new ArrayList<ContactInfo>();

        ContentResolver cr = getContentResolver();
        String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, "display_name LIKE '"+user_input+"%'", null, null);
        //Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
        Log.d("INNNN", String.valueOf(cursor.moveToFirst()));
        if (cursor.moveToFirst()) {
            for(int i=0; i < cursor.getCount(); i++) {
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Log.d("Contact-ID", contactId);
                Log.d("Contact-name", contactName);

                cursor.moveToNext();

                //
                //  Get all phone numbers.
                //
                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                String contact_number=null;
                while (phones.moveToNext()) {
                    contact_number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (type) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            // do something with the Home number here...
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            Log.d("Mobile-num", contact_number);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            // do something with the Work number here...
                            break;
                    }
                }
                phones.close();
                tempContactList.add(new ContactInfo(contactName,contact_number,-1));
                //tempContactList.add(contactName);
            }

        }
        return tempContactList;
    }

    public ArrayList<ContactInfo> getAllContacts() {

        ArrayList<ContactInfo> tempContactList = new ArrayList<ContactInfo>();

        ContentResolver cr = getContentResolver();
        String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
       // Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, "display_name LIKE '%"+user_input+"%'", null, null);
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
        Log.d("INNNN", String.valueOf(cursor.moveToFirst()));
        if (cursor.moveToFirst()) {
            for(int i=0; i < cursor.getCount(); i++) {
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Log.d("Contact-ID", contactId);
                Log.d("Contact-name", contactName);

                cursor.moveToNext();

                //
                //  Get all phone numbers.
                //
                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                String contact_number=null;
                while (phones.moveToNext()) {
                    contact_number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (type) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            // do something with the Home number here...
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            Log.d("Mobile-num", contact_number);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            // do something with the Work number here...
                            break;
                    }
                }
                phones.close();
                tempContactList.add(new ContactInfo(contactName,contact_number,1));
                //tempContactList.add(contactName);
            }

        }
        return tempContactList;
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
}
