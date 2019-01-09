package com.example.jw.magnetic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context){
        super(context, "magnetic.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mValue "+
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "magX REAL, magY REAL, magZ REAL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS mValue;");
        onCreate(db);
    }
}
