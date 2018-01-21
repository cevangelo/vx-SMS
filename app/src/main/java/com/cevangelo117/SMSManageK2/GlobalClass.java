package com.cevangelo117.SMSManageK2;

import android.app.Application;
import android.content.Context;

import java.io.Serializable;

/**
 * This is a helper class that will make all the necessary variables available
 * at all classes by getting the application context. The class contains only getters and setters.
 *
 * @author Evangelos Christelis
 */
public class GlobalClass extends Application implements Serializable {

    private String internet_mb = "-";
    private String min2all = "-";
    private String smsWhatsup_total = "-";
    private String smsAll = "-";
    private String minWhatsup = "-";
    private String ypoloipo = "-";
    private int current_thread_id;
    private boolean isThemeNight = true;
    private boolean changeOfTheme = false;

    private static Context context;

    public void onCreate() {
        super.onCreate();
        GlobalClass.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return GlobalClass.context;
    }

    public String getInternet_mb() {
        return internet_mb;
    }

    public void setInternet_mb(String internet_mb) {
        this.internet_mb = internet_mb;
    }

    public String getMin2all() {
        return min2all;
    }

    public void setMin2all(String min2all) {
        this.min2all = min2all;
    }

    public String getSmsWhatsup_total() {
        return smsWhatsup_total;
    }

    public void setSmsWhatsup_total(String smsWhatsup_total) {
        this.smsWhatsup_total = smsWhatsup_total;
    }

    public String getMinWhatsup() {
        return minWhatsup;
    }

    public void setMinWhatsup(String minWhatsup) {
        this.minWhatsup = minWhatsup;
    }

    public String getYpoloipo() {
        return ypoloipo;
    }

    public void setYpoloipo(String ypoloipo) {
        this.ypoloipo = ypoloipo;
    }

    public int getCurrent_thread_id() {
        return current_thread_id;
    }

    public void setCurrent_thread_id(int current_thread_id) {
        this.current_thread_id = current_thread_id;
    }

    public String getSmsAll() {
        return smsAll;
    }

    public void setSmsAll(String smsAll) {
        this.smsAll = smsAll;
    }

    public boolean isThemeNight() {
        return isThemeNight;
    }

    public void setThemeNight(boolean isThemeNight) {
        this.isThemeNight = isThemeNight;
    }

    public boolean isChangeOfTheme() {
        return changeOfTheme;
    }

    public void setChangeOfTheme(boolean changeOfTheme) {
        this.changeOfTheme = changeOfTheme;
    }
}