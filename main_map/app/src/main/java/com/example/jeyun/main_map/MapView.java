package com.example.jeyun.main_map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

public class MapView extends Fragment {

    MoveObject moveMap;

    public MapView() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        moveMap = new MoveObject(getContext(), R.drawable.main_3);
        return moveMap;
    }


}

