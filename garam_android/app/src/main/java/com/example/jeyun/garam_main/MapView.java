package com.example.jeyun.garam_main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

public class MapView extends Fragment {

    MoveObject moveMap;
    int currentDirection; // 방향

    public MapView() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        moveMap = new MoveObject(getContext(), R.drawable.main_3);
        return moveMap;
    }


    public void buttonPressed(String event) {
        if( event.equals("UP") ) {
            System.out.println("MapView Press UP");
            moveMap.pressButton("UP");
        } else if (event.equals("DOWN")) {
            System.out.println("MapView Press DOWN");
            moveMap.pressButton("DOWN");
        } else if (event.equals("LEFT")) {
            System.out.println("MapView Press LEFT");
            moveMap.pressButton("LEFT");
        } else if (event.equals("RIGHT")) {
            System.out.println("MapView Press RIGHT");
            moveMap.pressButton("RIGHT");
        }
    }

    public void stepDectect(int x ,int y) {
        moveMap.moveStep(x, y);
    }

    public void moveToPosition(float X, float Y) {

    }

    public void changeDirecton(float input) {
        //0~3 : UP, RIGHT, DOWN, LEFT
        moveMap.rotateMarkerImage(input);
    }

   public int getCurrentDirection() {
        currentDirection = ((MainActivity) getActivity()).getCurrentDirection();
        return currentDirection;
    }

    public void addExpectedCircle(double x, double y) {
        moveMap.addExpectedCircle((int) x, (int) y);
    }

    public void drawImage() {
        moveMap.invalidate();
    }

}
