package com.example.dlibmodule;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.dlibmodule.Utils.PermissionSupport;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    TextView id, pw;
    String strID, strPW;

    Intent intent;

    //자동 로그인
    CheckBox autoLogin;
    SharedPreferences appData;
    boolean saveLoginData;

    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;

    //firebase
    private FirebaseDatabase database = FirebaseDatabase.getInstance(); //파이어베이스 데이터베이스 연동
    //databaseReference는 DB의 특정 위치로 연결하는 것과 비슷.
    private DatabaseReference databaseReference = database.getReference();

    private PermissionSupport permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        //자동로그인 : 설정값 불러오기
        appData = getSharedPreferences("appData", MODE_PRIVATE);
        load();

        id = findViewById(R.id.idText);
        pw = findViewById(R.id.pwText);
        autoLogin = findViewById(R.id.checkBox);

        myUser = (User)getApplication();

        //이전 로그인 정보 기록이 있다면
        if(saveLoginData){
            id.setText(strID);
            pw.setText(strPW);
            autoLogin.setChecked(saveLoginData);
        }

        permissionCheck();
    }

    public void login(View v) {
        strID = id.getText().toString();
        strPW = pw. getText().toString();

        intent = new Intent(this, MainActivity.class);

        databaseReference.child("User").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(strID)){ //아이디 존재하면...
                            String valueID = snapshot.child(strID).child("id").getValue(String.class);
                            String valuePW = snapshot.child(strID).child("pw").getValue(String.class);
                            String valuePROFILE = snapshot.child(strID).child("profile").getValue(String.class);
                            String valueStreamKey = snapshot.child(strID).child("streamKey").getValue(String.class);

                            if(strPW.equals(valuePW)){
                                //전역변수 user 객체에 정보 담기
                                myUser.setId(valueID);
                                myUser.setPw(valuePW);
                                myUser.setPhotourl(valuePROFILE);
                                myUser.setStreamKey(valueStreamKey);

                                //로그인 성공시 저장 처리.
                                save();

                                startActivity(intent);
                            }else{
                                Toast.makeText(LoginActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                                pw.setText("");
                            }
                        }else{ //아이디 존재하지 않으면...
                            Toast.makeText(LoginActivity.this, "아이디가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                            id.setText("");
                            pw.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }
        );
    }

    private void load(){ // 설정값을 불러오는 함수
        saveLoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
        strID = appData.getString("id", "");
        strPW = appData.getString("pw", "");
    }

    private void save(){ //설정값을 저장하는 함수
        SharedPreferences.Editor editor = appData.edit();
        editor.putBoolean("SAVE_LOGIN_DATA", autoLogin.isChecked());
        editor.putString("id", id.getText().toString().trim());
        editor.putString("pw", pw.getText().toString().trim());

        editor.apply();

    }

    public void register(View v) {
        Intent intent = new Intent(this, com.example.dlibmodule.RegisterActivity.class);
        startActivity(intent);
    }

    private void permissionCheck() {
        // SDK 23버전 이하 버전에서는 Permission이 필요하지 않다
        if(Build.VERSION.SDK_INT >= 23){
            // 방금 전 만들었던 클래스 객체 생성
            permission = new PermissionSupport(this, this);

            // 권한 체크한 후에 리턴이 false로 들어온다면
            if (!permission.checkPermission()){
                // 권한 요청을 해줍니다.
                permission.requestPermission();
            }
        }
    }

    // Request Permission에 대한 결과 값을 받아올 수 있습니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 여기서도 리턴이 false로 들어온다면 (사용자가 권한 허용을 거부하였다면)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); ////////
        if (!permission.permissionResult(requestCode, permissions, grantResults)) {
            permission.requestPermission();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("종료하시겠습니까?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ActivityCompat.finishAffinity(LoginActivity.this);
                    }})
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }})
                .show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) && view instanceof EditText && !view.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom())
                ((InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow((this.getWindow().getDecorView().getApplicationWindowToken()), 0);
        }
        return super.dispatchTouchEvent(ev);
    }
}