package com.jjtx.multidownload;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jjtx on 2016/7/25.
 */
public class MultiDownloadDao {
    private MultiDownloadSqlHelper sqlHelper;

    public MultiDownloadDao(Context context) {
        sqlHelper = new MultiDownloadSqlHelper(context);
    }


    public boolean hasProcess(String threadId) {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        Cursor cursor = db.query("multi_process", new String[]{"process"}, "threadId=?", new String[]{threadId}, null, null, null);

        return cursor.moveToNext();

    }

    public int getStartIndex(String threadId) {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();
        Cursor cursor = db.query("multi_process", new String[]{"process"}, "threadId=?", new String[]{threadId}, null, null, null);
        cursor.moveToNext();
        int startIndex = cursor.getInt(cursor.getColumnIndex("process"));
        return startIndex;

    }


    public void addProcess(String threadId, int startIndex) {
        SQLiteDatabase db = sqlHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("threadId", threadId);
        values.put("process", startIndex);
        db.insert("multi_process", null, values);
    }


    public int getSumIndex(String fileName, int id) {

        int sum = 0;
        for (int i = 0; i < id; i++) {
            sum += getStartIndex(fileName + i);
        }
        return sum;

    }

    public void updateProcess(String threadId, int totalProcess) {
        SQLiteDatabase db = sqlHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("process", totalProcess);
        db.update("multi_process", values, "threadId=?", new String[]{threadId});
    }


    public void deleteAll(String fileName, int id) {

        for (int i = 0; i < id; i++) {
            delete(fileName + i);
        }
    }


    private void delete(String threadId) {
        SQLiteDatabase db = sqlHelper.getWritableDatabase();
        db.delete("multi_process", "threadId=?", new String[]{threadId});

    }


}
