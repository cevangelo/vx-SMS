package com.cevangelo117.SMSManageK2;

import java.util.ArrayList;

/**
 * Created by Vagelis on 24/10/2014.
 */
public class WhatsUpInfo {

    private String ypoloipo;
    private int internet_mb,min2all = 0,smsWhatsup_total=0,minWhatsup=0;
    private ArrayList<String> smsWhatsup_list = new ArrayList<String>();
    private ArrayList<String> minWhatsup_list = new ArrayList<String>();

    public ArrayList<String> getMinWhatsup_list() {
        return minWhatsup_list;
    }

    public void setMinWhatsup_list(ArrayList<String> minWhatsup_list) {
        this.minWhatsup_list = minWhatsup_list;
    }

    public String getYpoloipo() {
        return ypoloipo;
    }

    public void setYpoloipo(String ypoloipo) {
        this.ypoloipo = ypoloipo;
    }

    public int getInternet_mb() {
        return internet_mb;
    }

    public void setInternet_mb(int internet_mb) {
        this.internet_mb = internet_mb;
    }

    public int getMin2all() {
        return min2all;
    }

    public void setMin2all(int min2all) {
        this.min2all = min2all;
    }

    public int getSmsWhatsup_total() {
        return smsWhatsup_total;
    }

    public void setSmsWhatsup_total(int smsWhatsup_total) {
        this.smsWhatsup_total = smsWhatsup_total;
    }

    public int getMinWhatsup() {
        return minWhatsup;
    }

    public void setMinWhatsup(int minWhatsup) {
        this.minWhatsup = minWhatsup;
    }

    public ArrayList<String> getSmsWhatsup_list() {
        return smsWhatsup_list;
    }

    public void setSmsWhatsup_list(ArrayList<String> smsWhatsup_list) {
        this.smsWhatsup_list = smsWhatsup_list;
    }
}
