package com.example.dlibmodule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        bottomNavigationView = findViewById(R.id.navigation);

        getSupportFragmentManager().beginTransaction().add(R.id.layout, new Fragment1()).commit();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.home:
                        getSupportFragmentManager().beginTransaction().replace(R.id.layout, new Fragment1()).commit();
                        break;
                    case R.id.photo:
                        getSupportFragmentManager().beginTransaction().replace(R.id.layout, new Fragment2()).commit();
                        break;
                    case R.id.video:
                        getSupportFragmentManager().beginTransaction().replace(R.id.layout, new Fragment3()).commit();
                        break;
                    case R.id.setting:
                        getSupportFragmentManager().beginTransaction().replace(R.id.layout, new Fragment4()).commit();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // 홈화면이면 뒤로가기 눌렀을 때 앱 종료, 그 외의 화면이면 홈화면으로 이동
        if(bottomNavigationView.getSelectedItemId()==R.id.home) {
            new AlertDialog.Builder(this)
                    .setMessage("종료하시겠습니까?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ActivityCompat.finishAffinity(MainActivity.this);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        }
        else {
            bottomNavigationView.setSelectedItemId(R.id.home);
            getSupportFragmentManager().beginTransaction().replace(R.id.layout, new Fragment1()).commit();
        }
    }
}