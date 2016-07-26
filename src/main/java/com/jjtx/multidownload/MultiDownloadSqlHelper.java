package com.jjtx.multidownload;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jjtx on 2016/7/25.
 */
public class MultiDownloadSqlHelper extends SQLiteOpenHelper {
    public MultiDownloadSqlHelper(Context context) {
        super(context, "multiDownload.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table multi_process(id Integer primary key autoincrement,threadId var not null,process int default 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
