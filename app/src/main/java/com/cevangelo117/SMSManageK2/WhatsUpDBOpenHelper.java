package com.cevangelo117.SMSManageK2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Vagelis on 1/11/2014.
 */
public class WhatsUpDBOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smsmanage_DB.db";
    public static final String WHATSUP_STATS_TABLE_NAME = "whats_up_stats";
    public static final String WHATSUP_STAT_TYPE = "whatsup_stat_type";
    public static final String WHATSUP_STAT_VALUE = "whatsup_stat_value";
    private static final String WHATSUP_STATS_TABLE_CREATE =
            "CREATE TABLE " + WHATSUP_STATS_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    WHATSUP_STAT_TYPE + " TEXT, " +
                    WHATSUP_STAT_VALUE + " TEXT);";

    public WhatsUpDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WHATSUP_STATS_TABLE_CREATE);
        Log.d("DB_WHATSUP","on Create");

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS whats_up_stats");
        this.onCreate(db);
        Log.d("DB_WHATSUP","on Upgrade");

    }


}
