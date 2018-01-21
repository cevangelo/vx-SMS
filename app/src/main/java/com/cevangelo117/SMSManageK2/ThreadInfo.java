package com.cevangelo117.SMSManageK2;

/**
 * Created by Vagelis on 18/10/2014.
 */
public class ThreadInfo {

    private String last_message;
    private int thread_id;
    private String date;
    ContactInfo contact;

    public ThreadInfo(String contact_number, String last_message, int thread_id, String date) {
        contact = new ContactInfo(null,contact_number,0);
        this.last_message = last_message;
        this.thread_id = thread_id;
        this.date = date;
    }

    public ThreadInfo(String contact_name, String contact_number, String last_message, String date) {
        contact = new ContactInfo(contact_name,contact_number,0);
        this.last_message = last_message;
        this.date = date;
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public int getThread_id() {
        return thread_id;
    }

    public void setThread_id(int thread_id) {
        this.thread_id = thread_id;
    }

    public ContactInfo getContact() {
        return contact;
    }

    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
