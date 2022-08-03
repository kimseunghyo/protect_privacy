package com.example.dlibmodule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity { //회원가입 화면에서...

    private FirebaseDatabase database = FirebaseDatabase.getInstance(); //파이어베이스 데이터베이스 연동
    //databaseReference는 DB의 특정 위치로 연결하는 것과 비슷.
    private DatabaseReference databaseReference = database.getReference();

    private EditText mEtId, mEtPwd, mEtCheckPw, mEtStreamKey; //회원가입 입력필드
    private String strId, strPwd, strCheckPw, strStreamKey;
    private Button nextBtn;
    private ImageView helpRegisterBtn;

    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;

    @Override
    protected void onCreate(Bundle  savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_register);

        mEtId = findViewById(R.id.idText);
        mEtPwd = findViewById(R.id.pwText);
        mEtCheckPw = findViewById(R.id.editTextTextPassword4);
        mEtStreamKey = findViewById(R.id.editTextTextPassword5);
        nextBtn = findViewById(R.id.nextBtn);
        helpRegisterBtn = findViewById(R.id.helpRegisterBtn);

        /////수정/////
        nextBtn.setBackgroundColor(Color.DKGRAY);
        nextBtn.setTextColor(Color.GRAY);
        nextBtn.setClickable(false);
        /////////////
        myUser = (User)getApplication();

        helpRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                builder.setMessage("<stream Key 찾는 방법>\n" +
                        "1. YouTube로 이동합니다.\n" +
                        "2. 오른쪽 상단에서 만들기->실시간 스트리밍 시작을 클릭합니다.\n" +
                        "3. 스트림 카테고리에서 스트림 키를 복사합니다.");
                builder.setPositiveButton("알겠습니다.", null);
                builder.create().show();
            }
        });

    }

    public void next(View v) { //회원가입 시 다음 단계로 가기위한 버튼

        strId = mEtId.getText().toString();
        strPwd = mEtPwd.getText().toString();
        strCheckPw = mEtCheckPw.getText().toString();
        strStreamKey = mEtStreamKey.getText().toString();

        if (strPwd.equals(strCheckPw)) {
            //database에 user 추가
            addUsers(strId, strPwd, strStreamKey);
            if (strStreamKey.equals("")) {
                Toast.makeText(RegisterActivity.this, "방송 시작 전 사용자의 사진과 streamKey를 추가해 주세요.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RegisterActivity.this, "방송 시작 전 사용자의 사진을 추가해 주세요.", Toast.LENGTH_SHORT).show();
            }
            SharedPreferences preferences = getSharedPreferences("appData", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        } else {
            Toast.makeText(RegisterActivity.this, "비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
            mEtCheckPw.setText("");
            return;
        }
    }

    public void CheckIdOverlap(View v){
        //예은 : 버튼 enabled일때 이미지 바꾸기 시도...
        strId = mEtId.getText().toString();
        //아이디 중복 검사
        databaseReference.child("User").child(strId).child("id").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String value = snapshot.getValue(String.class);
                        ///// 수정
                        if(strId.equals("")) {
                            Toast.makeText(RegisterActivity.this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        }
                        else if(value!=null){
                            Toast.makeText(RegisterActivity.this, "중복된 아이디입니다.", Toast.LENGTH_SHORT).show();
                            mEtId.setText("");
                            /////
                            nextBtn.setBackgroundColor(Color.DKGRAY);
                            nextBtn.setTextColor(Color.GRAY);
                            nextBtn.setClickable(false);
                            ////
                            return;
                        }
                        /////
                        else{
                            /////수정/////
                            nextBtn.setBackgroundColor(Color.rgb(255, 195, 216));
                            nextBtn.setTextColor(Color.WHITE);
                            nextBtn.setClickable(true);
                            mEtId.setEnabled(false);
                            mEtId.setTextColor(Color.DKGRAY);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }
        );

    }

    //값을 파이어베이스 realtime database로 넘기는 함수
    public void addUsers(String id, String pw, String streamKey){
        myUser.setId(id);
        myUser.setPw(pw);
        myUser.setStreamKey(streamKey);

        //child는 해당 키 위치로 이동하는 함수.
        //키가 없는데 값을 지정한 경우 자동으로 생성된다.
        databaseReference.child("User").child(id).child("id").setValue(myUser.getId());
        databaseReference.child("User").child(id).child("pw").setValue(myUser.getPw());
        databaseReference.child("User").child(id).child("streamKey").setValue(myUser.getStreamKey());
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