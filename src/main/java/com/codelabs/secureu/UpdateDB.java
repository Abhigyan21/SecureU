package com.codelabs.secureu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by abhigyan on 15/8/16.
 */
public class UpdateDB extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "ContactsDB.db";
    public static final String CONTACTS_TABLE_NAME = "CONTACTS";
    public static final String CONTACTS_COLUMN_NAME = "NAME";
    public static final String CONTACTS_COLUMN_PHONE = "NUMBER";

    public UpdateDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("Create table IF NOT EXISTS CONTACTS(ID INTEGER PRIMARY KEY AUTOINCREMENT , NAME TEXT NOT NULL , NUMBER TEXT NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS CONTACTS");
        onCreate(db);
    }

    public void insert(String name, String number) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(CONTACTS_COLUMN_NAME, name);
        contentValues.put(CONTACTS_COLUMN_PHONE, number);

        db.insert(CONTACTS_TABLE_NAME, null, contentValues);
        db.close();
    }

    public String[] extract() {
        Cursor c;
        SQLiteDatabase db;
        String[] result = null;

        try {
            db = this.getReadableDatabase();
            c = db.rawQuery("SELECT * FROM CONTACTS", null);
            result = new String[c.getCount()];

            while (c.moveToNext()) {
                try {
                    int numID = c.getColumnIndex("NUMBER");
                    int nameID = c.getColumnIndex("NAME");
                    result[c.getPosition()] = new StringBuilder(String.valueOf(c.getString(nameID))).append(": ").append(c.getString(numID)).toString();
                } catch (Exception e) {
                    Log.e("UpdateDB", e.getMessage());
                }
            }

            if (c.getCount() <= 0) {
                return null;
            } else {
                return result;
            }
        } catch (SQLException e) {
            Log.e("Update", e.getMessage());
        }

        return result;
    }

    public void remove(String number){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CONTACTS_TABLE_NAME, "NUMBER = ?", new String[] {number});
        db.close();
    }

    public ArrayList<String> retrieve(){
        ArrayList<String> contactsList = new ArrayList<>();
        Cursor c;
        SQLiteDatabase db;
        try {
            db = this.getReadableDatabase();
            c = db.rawQuery("SELECT * FROM CONTACTS", null);

            while (c.moveToNext()) {
                try {
                    int numID = c.getColumnIndex("NUMBER");
                    contactsList.add(c.getString(numID));
                } catch (Exception e) {
                    Log.e("UpdateDB", e.getMessage());
                }
            }
        } catch (SQLException e) {
            Log.e("Update", e.getMessage());
        }
        return contactsList;
    }
}
