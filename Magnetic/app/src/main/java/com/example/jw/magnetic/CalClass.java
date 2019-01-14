package com.example.jw.magnetic;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class CalClass {
    private HashMap<Integer, Double> DBvalue = new HashMap<>();
    SQLiteDatabase db;

    public CalClass(SQLiteDatabase db) {
        this.db = db;

        readDB();
    }

    public void readDB() {
        String sql = "SELECT * FROM mValue;";
        Cursor result = db.rawQuery(sql, null);

        //Log.i("Calclass+ReadDB", result.getCount()+"");
        while (!result.isLast()) {
            result.moveToNext();
            DBvalue.put(result.getInt(0), result.getDouble(4));
        }
        result.close();
    }

    public void calOpt(double input) {
        HashMap<Integer, Double> hashtemp = new HashMap<>();

        Log.i("Calclass+DBsize", DBvalue.size() + "");
        for (int i = 0; i < DBvalue.size(); i++) {
            double temp = DBvalue.get(i);

       //     Log.i("Calclass", i + " " + (int) input + "\\\\" + (int) temp);
            if ((int) input == (int) temp) {
//                Log.i("Calclass", (int) input + "\\\\" + (int) temp);
//
                hashtemp.put(i, Math.abs(input - temp));
//            }
            }
        }

        double min = 1.0;
        int minIndex = -1;

        Iterator<Integer> iter = hashtemp.keySet().iterator();
        while(iter.hasNext()){
            int key = iter.next();
            if (min > hashtemp.get(key)) {
                min = hashtemp.get(key);
                minIndex = key;
            }
        }

        Log.i("CalClass", minIndex + " " + min);
    }
}

