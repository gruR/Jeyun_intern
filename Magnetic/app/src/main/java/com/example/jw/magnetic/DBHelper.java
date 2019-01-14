package com.example.jw.magnetic;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context){
        super(context, "navi.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mValue "+
                "(blockNum INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "magX REAL, magY REAL, magZ REAL, magT REAL);");
        db.execSQL("CREATE TABLE wValue "+
                "(blockNum INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "macId TEXT, wifi REAL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS mValue;");
        db.execSQL("DROP TABLE IF EXISTS wValue;");
        onCreate(db);
    }
}
