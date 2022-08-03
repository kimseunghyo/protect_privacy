package com.example.dlibmodule;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


//설정화면
public class Fragment4 extends Fragment {
    private View view;

    Button logoutBtn, dropBtn, changeInfoBtn, checkChangePwBtn, streamInfoInputBtn, cancelChangeinfoBtn, checkChangeinfoBtn, cancelStreamBtn, InputStreamBtn;
    LinearLayout linearLayout, idLayout, pwLayout, checkPwLayout, checkChangePwLayout;
    LinearLayout streamKeyLayout, checkStreamLayout;
    TextView idText, pwText, checkPwText, streamKeyText;

    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;

    //firebase
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.set_up, container, false);

        linearLayout = view.findViewById(R.id.setUpLinearLayout);
        linearLayout.setOnTouchListener(new View.OnTouchListener() //화면 클릭시 키보드 내리기...
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                hideKeyboard();
                return false;
            }
        });

        changeInfoBtn = view.findViewById(R.id.changeInfoBtn); //비번변경버튼
        streamInfoInputBtn = view.findViewById(R.id.streamInfoInputBtn); //스트림 키 입력창 버튼
        logoutBtn = view.findViewById(R.id.logOutBtn); //로그아웃 버튼
        dropBtn = view.findViewById(R.id.dropBtn); //회원탈퇴 버튼

        checkChangePwBtn = view.findViewById(R.id.checkChangeinfoBtn); //비번 변경 alertDialog에서 사용하는 btn -> 비번 확인

        //비번 변경 레이아웃 4개
        idLayout = view.findViewById(R.id.idLayout);
        pwLayout = view.findViewById(R.id.pwLayout);
        checkPwLayout = view.findViewById(R.id.checkPwLayout);
        checkChangePwLayout = view.findViewById(R.id.checkChangeinfoBtnLayout);

        idText = view.findViewById(R.id.idText); //아이디
        pwText = view.findViewById(R.id.pwText); //변경될 비번
        checkPwText = view.findViewById(R.id.editTextTextPassword4); //변경될 비번 확인
        cancelChangeinfoBtn = view.findViewById(R.id.cancelChangeinfoBtn); //비번변경 취소 버튼
        checkChangeinfoBtn = view.findViewById(R.id.checkChangeinfoBtn); //비번변경 확인 버튼


        //스트림 키 입력창 레이아웃 3개
        streamKeyLayout = view.findViewById(R.id.streamKeyLayout);
        checkStreamLayout = view.findViewById(R.id.checkStreamLayout);

        streamKeyText = view.findViewById(R.id.streamKeyText);
        cancelStreamBtn = view.findViewById(R.id.cancelStreamBtn); //스트림 키 변경 취소 버튼
        InputStreamBtn = view.findViewById(R.id.InputStreamBtn); // 스트림 키 변경 확인 버튼

        myUser = (User)getActivity().getApplication();
        idText.setText(myUser.getId());
        idText.setTextColor(Color.DKGRAY);
        streamKeyText.setText(myUser.getStreamKey());

        //비밀번호 변경
        changeInfoBtn.setOnClickListener(new View.OnClickListener() { //비밀번호 변경 입력창 visible 상태로...
            @Override
            public void onClick(View view) {
                linearLayout  = (LinearLayout)View.inflate(getActivity(), R.layout.dialog_changeinfo, null);

                new AlertDialog.Builder(getActivity())
                        .setView(linearLayout)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                EditText inputPW = (EditText) linearLayout.findViewById(R.id.ChangeInfo_checkPW);
                                String strInputPW = inputPW.getText().toString();
                                if(strInputPW.equals(myUser.getPw())){ //입력한 값과 비밀번호가 일치할 때
                                    //새 비밀번호 입력창 활성화
                                    changeInfoBtn.setEnabled(false);
                                    idLayout.setVisibility(View.VISIBLE);
                                    pwLayout.setVisibility(View.VISIBLE);
                                    checkPwLayout.setVisibility(View.VISIBLE);
                                    checkChangePwLayout.setVisibility(View.VISIBLE);

                                }
                                else{
                                    Toast.makeText(getActivity(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //아무 일도 일어나지 않음.
                            }
                        })
                        .show();
            }
        });

        cancelChangeinfoBtn.setOnClickListener(new View.OnClickListener() { //비밀번호 변경창에서 취소 버튼 클릭 시...
            @Override
            public void onClick(View view) {
                //버튼, textview 활성화/gone 상태로 돌리기
                changeInfoBtn.setEnabled(true);
                idLayout.setVisibility(View.GONE);
                pwLayout.setVisibility(View.GONE);
                checkPwLayout.setVisibility(View.GONE);
                checkChangePwLayout.setVisibility(View.GONE);

                pwText.setText("");
                checkPwText.setText("");
            }
        });

        checkChangeinfoBtn.setOnClickListener(new View.OnClickListener() { //비밀번호 변경 최종 확인 버튼
            @Override
            public void onClick(View view) {
                String strPwText = pwText.getText().toString();
                String strcheckPwText = checkPwText.getText().toString();

                if(strPwText.equals(strcheckPwText)){ //비밀번호와 비밀번호 확인 텍스트가 같으면...
                    myUser.setPw(strPwText);
                    databaseReference.child("User").child(myUser.getId()).child("pw").setValue(strPwText);

                    //버튼, textview 활성화/gone 상태로 돌리기
                    changeInfoBtn.setEnabled(true);
                    idLayout.setVisibility(View.GONE);
                    pwLayout.setVisibility(View.GONE);
                    checkPwLayout.setVisibility(View.GONE);
                    checkChangePwLayout.setVisibility(View.GONE);

                    //사용자에게 비밀번호 변경되었음을 알리기.
                    Toast.makeText(getActivity(), "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    pwText.setText("");
                    checkPwText.setText("");


                }
                else if(strPwText.equals("") || strcheckPwText.equals("")){
                    Toast.makeText(getActivity(), "모든 입력창에 입력이 필요합니다.", Toast.LENGTH_SHORT).show();
                }
                else{ //같지 않으면...
                    Toast.makeText(getActivity(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    pwText.setText("");
                    checkPwText.setText("");
                }
            }
        });

        //스트림 정보 입력창
        streamInfoInputBtn.setOnClickListener(new View.OnClickListener() { //스트림 정보 입력창 visible 상태로...
            @Override
            public void onClick(View view) {
                //정보 입력창 활성화
                streamInfoInputBtn.setEnabled(false);
                streamKeyLayout.setVisibility(View.VISIBLE);
                checkStreamLayout.setVisibility(View.VISIBLE);
            }
        });

        cancelStreamBtn.setOnClickListener(new View.OnClickListener() { //스트림 키 저장 취소...
            @Override
            public void onClick(View view) {
                //버튼, textview 활성화/gone 상태로 돌리기
                streamInfoInputBtn.setEnabled(true);
                streamKeyLayout.setVisibility(View.GONE);
                checkStreamLayout.setVisibility(View.GONE);

                streamKeyText.setText(myUser.getStreamKey());
            }
        });

        InputStreamBtn.setOnClickListener(new View.OnClickListener() { //스트림 키 & url 저장 시...
            @Override
            public void onClick(View view) {
                myUser.setStreamKey(streamKeyText.getText().toString());
                databaseReference.child("User").child(myUser.getId()).child("streamKey").setValue(myUser.getStreamKey());

                //버튼, textview 활성화/gone 상태로 돌리기
                streamInfoInputBtn.setEnabled(true);
                streamKeyLayout.setVisibility(View.GONE);
                checkStreamLayout.setVisibility(View.GONE);
            }
        });

        //로그아웃
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//로그아웃
                //회원정보 전역변수에서 삭제
                myUser.setPhotourl("");
                myUser.setPw("");
                myUser.setId("");

                Toast.makeText(getActivity(), "로그 아웃", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            }
        });
        //회원탈퇴
        dropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);

                new AlertDialog.Builder(getActivity())
                        .setTitle("회원 정보를 영구 삭제합니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //회원 정보 삭제 및 로그인 화면으로 이동...
                                delete();
                                SharedPreferences preferences = getActivity().getSharedPreferences("appData", getContext().MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.clear();
                                editor.commit();
                                Toast.makeText(getActivity(), "회원 탈퇴", Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //아무 일도 일어나지 않음.
                            }
                        })
                        .show();

            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    public void delete(){ //파이어베이스 데이터 삭제...
        //firebase storage 연결위한 코드...
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl("gs://haniumproject-88a20.appspot.com/").child("images/"+myUser.getId()+".jpg");

        databaseReference.child("User").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(myUser.getId())){ //아이디 존재하면...
                            //관련 정보 삭제
                            databaseReference.child("User").child(myUser.getId()).setValue(null);
                            //storage에 저장된 이미지 삭제...
                            storageReference.delete();
                        }else{ //아이디 존재하지 않으면...
                            Toast.makeText(getActivity(), "해당 과정을 수행 중 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                }
        );
    }

    private void hideKeyboard()
    {
        if (getActivity() != null && getActivity().getCurrentFocus() != null)
        {
            // 프래그먼트기 때문에 getActivity() 사용
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
