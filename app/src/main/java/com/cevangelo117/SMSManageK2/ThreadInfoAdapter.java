package com.cevangelo117.SMSManageK2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Vagelis on 18/10/2014.
 */
public class ThreadInfoAdapter extends ArrayAdapter<ThreadInfo> {

    private final Context context;
    private final ArrayList<ThreadInfo> values;

    public ThreadInfoAdapter(Context context, ArrayList<ThreadInfo> values) {
        super(context, R.layout.sms_list_item, values);
        this.context = context;
        this.values = values;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // We call the LayoutInflater to specify the properties of our custom view.
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.sms_list_item, parent, false);
        try{
            ImageView cont_thumb_imgview = (ImageView) rowView.findViewById(R.id.contact_icon);
            InputStream input = openPhoto(values.get(position).getContact().getContact_ID());
            if(input != null) {
                Bitmap contact_bmp = BitmapFactory.decodeStream(input);
                cont_thumb_imgview.setImageBitmap(contact_bmp);
            }
            else{ // in case the contact exists but has no photo assigned
                cont_thumb_imgview = (ImageView) rowView.findViewById(R.id.contact_icon);
                cont_thumb_imgview.setImageResource(R.drawable.contact_icon);
            }
        } catch (IllegalArgumentException e) {
            ImageView cont_thumb_imgview = (ImageView) rowView.findViewById(R.id.contact_icon);
            cont_thumb_imgview.setImageResource(R.drawable.contact_icon);
        }

        TextView cont_name_tv = (TextView) rowView.findViewById(R.id.contact_name);
        GlobalClass g = (GlobalClass) context.getApplicationContext();
        if(g.isThemeNight()){
            cont_name_tv.setTextColor(Color.WHITE);
        } else{
            cont_name_tv.setTextColor(Color.BLACK);
        }
        String name = values.get(position).getContact().getContact_name();
        if(name!=null) {
            cont_name_tv.setText(name);
            Log.d("Contact - Name", name);
        }
        else{
            String num = values.get(position).getContact().getContact_number();
            cont_name_tv.setText(num);
            Log.d("Contact - Num", num);
        }
        Log.d("Contact - ID", String.valueOf(values.get(position).getContact().getContact_ID()));

        TextView msg_pre_tv = (TextView) rowView.findViewById(R.id.message_preview);
        if(g.isThemeNight()){
            msg_pre_tv.setTextColor(Color.WHITE);
        } else{
            msg_pre_tv.setTextColor(Color.BLACK);
        }
        String msg = values.get(position).getLast_message();
        String preview;
        if(msg.length()>=64)
            preview = msg.substring(0,64) + "...";
        else
            preview = msg;
        msg_pre_tv.setText(preview);

        TextView date_tv = (TextView) rowView.findViewById(R.id.date_preview);
        if(g.isThemeNight()){
            date_tv.setTextColor(Color.WHITE);
        } else{
            date_tv.setTextColor(Color.BLACK);
        }
        String date = values.get(position).getDate();
        date_tv.setText(date);

        return rowView;
    }

    public InputStream openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = this.context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
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
}