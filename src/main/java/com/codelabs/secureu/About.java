package com.codelabs.secureu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ScrollView;

/**
 * Created by abhigyan on 24/8/16.
 */
public class About extends AppCompatActivity{
ScrollView layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        layout = (ScrollView) findViewById(R.id.terms_layout);
        layout.setBackgroundResource(R.drawable.about);
    }
}
