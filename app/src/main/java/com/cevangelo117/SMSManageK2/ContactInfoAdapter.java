package com.cevangelo117.SMSManageK2;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactInfoAdapter extends ArrayAdapter<ContactInfo> {
    private ArrayList<ContactInfo> contacts;
    private ArrayList<ContactInfo> contactsAll;
    private ArrayList<ContactInfo> suggestions;
    private int viewResourceId;

    @SuppressWarnings("unchecked")
    public ContactInfoAdapter(Context context, int viewResourceId,
                                ArrayList<ContactInfo> contacts) {
        super(context, viewResourceId, contacts);
        this.contacts = contacts;
        this.contactsAll = (ArrayList<ContactInfo>) contacts.clone();
        this.suggestions = new ArrayList<ContactInfo>();
        this.viewResourceId = viewResourceId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(viewResourceId, null);
        }
        ContactInfo contact = contacts.get(position);
        if (contact != null) {
            GlobalClass g = (GlobalClass) getContext().getApplicationContext();
            TextView name_tv = (TextView)  v.findViewById(R.id.contact_name_autocomplete);
            if (name_tv != null) {
                name_tv.setText(contact.getContact_name());
            }
            TextView number_tv = (TextView)  v.findViewById(R.id.contact_number_autocomplete);
            if (number_tv != null) {
                number_tv.setText(contact.getContact_number());
            }
            if (!g.isThemeNight()) {
                name_tv.setTextColor(Color.BLACK);
                number_tv.setTextColor(Color.BLACK);
            }
        }
        return v;
    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    Filter nameFilter = new Filter() {
        public String convertResultToString(Object resultValue) {
            String str = ((ContactInfo) (resultValue)).getContact_name();
            return str;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                for (ContactInfo contact : contactsAll) {
                    if (contact.getContact_name().toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                        suggestions.add(contact);
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            @SuppressWarnings("unchecked")
            ArrayList<ContactInfo> filteredList = (ArrayList<ContactInfo>) results.values;
            if (results != null && results.count > 0) {
                clear();
                for (ContactInfo c : filteredList) {
                    add(c);
                }
                notifyDataSetChanged();
            }
        }
    };

}