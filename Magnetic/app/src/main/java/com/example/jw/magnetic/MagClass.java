package com.example.jw.magnetic;

import java.util.ArrayList;

public class MagClass {
    private ArrayList<Float> magX, magY, magZ;
    private boolean printed;

    public MagClass() {
        magX = new ArrayList<Float>();
        magY = new ArrayList<Float>();
        magZ = new ArrayList<Float>();

        printed = false;
    }

    public void setPrinted(boolean b) {
        this.printed = b;
    }

    public boolean isPrinted() {
        return printed;
    }

    public int getSize() {
        return magX.size() & magY.size() & magZ.size();
    }

    public void addValue(float X, float Y, float Z) {
        if (getSize() > 10)
            return;
        magX.add(X);
        magY.add(Y);
        magZ.add(Z);
    }

    public float[] getValue() {
        float sumX = 0, sumY = 0, sumZ = 0;

        for (int i = 0; i < 10; i++) {
            sumX += magX.get(i);
            sumY += magY.get(i);
            sumZ += magZ.get(i);
        }

        float[] result = {sumX / 10, sumY / 10, sumZ / 10};

        return result;
    }
}
