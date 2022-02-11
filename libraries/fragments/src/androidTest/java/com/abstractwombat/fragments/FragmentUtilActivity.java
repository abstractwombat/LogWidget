package com.abstractwombat.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

public class FragmentUtilActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout view = new LinearLayout(this);
        //view.setId(1);

        setContentView(view);
    }
}