package com.cevangelo117.SMSManageK2;

/**
 * Created by Vagelis on 21/10/2014.
 */
public class ContactInfo {

    private String contact_name;
    private String contact_number;
    private long contact_ID;

    public ContactInfo(String contact_name, String contact_number, long contact_ID) {
        this.contact_name = contact_name;
        this.contact_number = contact_number;
        this.contact_ID = contact_ID;
    }

    public String getContact_name() {
        return contact_name;
    }

    public void setContact_name(String contact_name) {
        this.contact_name = contact_name;
    }

    public String getContact_number() {
        return contact_number;
    }

    public void setContact_number(String contact_number) {
        this.contact_number = contact_number;
    }

    public long getContact_ID() {
        return contact_ID;
    }

    public void setContact_ID(long contact_ID) {
        this.contact_ID = contact_ID;
    }
}
