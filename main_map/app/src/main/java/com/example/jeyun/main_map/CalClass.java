package com.example.jeyun.main_map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class CalClass {
    private HashMap<Integer, Double> DBvalue = new HashMap<>();
    ArrayList<String[]> wifi = new ArrayList<>();   // block - macid - rss
    private static CalClass myObject = null;
    SQLiteDatabase db;

    public CalClass(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getReadableDatabase();

        readDB();

        myObject = this;
    }

    public static CalClass getCalObject(){
        return myObject;
    }

    public void readDB() {
        String sql = "SELECT * FROM mValue;";
        Cursor result = db.rawQuery(sql, null);

        if (result.isFirst() != result.isLast()) {   // DB 비었을때 체크
            while (!result.isLast()) {
                result.moveToNext();
                DBvalue.put(result.getInt(5), result.getDouble(4)); // block num, magT
            }
        }


        sql = "SELECT * FROM wValue;";
        result = db.rawQuery(sql, null);

        if (result.isFirst() != result.isLast()) {   // DB 비었을때 체크
            while (!result.isLast()) {
                result.moveToNext();

                wifi.add(new String[]{result.getString(3), result.getString(1), result.getString(2)});
            }
        }
        result.close();
    }

    public int calMag(double input) {
        HashMap<Integer, Double> hashtemp = new HashMap<>();

        Log.i("Calclass+DBsize", DBvalue.size() + " "+input);
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
        while (iter.hasNext()) {
            int key = iter.next();
            if (min > hashtemp.get(key)) {
                min = hashtemp.get(key);
                minIndex = key;
            }
        }

        Log.i("CalClass", minIndex + " " + min);

        return minIndex;
    }

    public int calWifi(String[][] input) {   // [0] : macid [1] : rss
        int inputsize = input.length;

        Iterator<String[]> iter = wifi.iterator();

        int prevBlock = 0;
        String[] temp = null;

        ArrayList<Double[]> arr = new ArrayList<>();
        ArrayList<String[]> RSS = new ArrayList<>();

        while (iter.hasNext()) {
            int count = 0;
            RSS.clear();

            if (prevBlock != 0)   // temp 초기화하기전에 저장
                RSS.add(new String[]{temp[1], temp[2]});   // [1] : macid [2] : rss

            temp = null;

            while (iter.hasNext()) {
                temp = iter.next();
                if (Integer.parseInt(temp[0]) != prevBlock)   // 블럭 달라지면 끝
                    break;
                RSS.add(new String[]{temp[1], temp[2]});
            }

            double A = 0, B = 0, AB = 0;   // 같은 블럭끼리 모아놓은 RSS 리스트로 값 계산

            for (int i = 0; i < inputsize; i++) {
                for (int j = 0; j < RSS.size(); j++) {
                    if (input[i][0].equals(RSS.get(j)[0])) {
                        count++;   // TODO : 카운트 세서 몇 퍼센트 이하로 일치하면 비교하지도 않음
                        A += Math.pow(Double.parseDouble(input[i][1]), 2);
                        B += Math.pow(Double.parseDouble(RSS.get(j)[1]), 2);
                        AB += Math.abs(Double.parseDouble(input[i][1]) * Double.parseDouble(RSS.get(j)[1]));
                        break;
                    }
                }
            }

//            if (RSS.size() * 0.5 <= count)   // 일단은 50퍼 이상
            arr.add(new Double[]{(double) prevBlock, AB / (A + B - AB)});

            if (temp != null)
                prevBlock = Integer.parseInt(temp[0]);
        }

        double max = 0;
        double maxIndex = -1;
        for (int i = 0; i < arr.size(); i++) {
            if (max < arr.get(i)[1]) {
                max = arr.get(i)[1];
                maxIndex = arr.get(i)[0];
            }
        }

        return (int) maxIndex;
    }


}