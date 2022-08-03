package com.example.dlibmodule;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


//메인화면
public class Fragment1 extends Fragment {
    private View view;
    //로그인 정보 유지
    User myUser;
    TextView test;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = database.getReference();

    private ImageView camera_button;

    //이미지 등록 확인
    private boolean value = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.home, container, false);
        camera_button=view.findViewById(R.id.liveStBtn);

        //로그인 정보 유지
        myUser = (User)getActivity().getApplication();
        test = view.findViewById(R.id.IDPWtest);
        test.setText(myUser.getId()+"님 어서오세요");

        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                String strStreamKey = myUser.getStreamKey();
                if(strStreamKey.length() == 24){
                    if(myUser.getPhotourl() == null){ //사용자 이미지가 등록되지 않으면...
                        Toast.makeText(getActivity(),"사용자 이미지를 등록해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                    else if(myUser.getStreamKey().equals("")){ //streamKey 입력되어있지 않으면...
                        Toast.makeText(getActivity(),"streamKey를 등록해 주세요.", Toast.LENGTH_SHORT).show();
                    }else if(strStreamKey.charAt(4) != '-' || strStreamKey.charAt(9) != '-' || strStreamKey.charAt(14) != '-' || strStreamKey.charAt(19) != '-'){
                        Toast.makeText(getActivity(),"streamKey가 형식에 맞지 않습니다. 다시 확인해 주세요.", Toast.LENGTH_SHORT).show();
                    }else{ //사용자 이미지가 등록되어 있다면...
                        startActivity(intent);
                    }
                }else {
                    Toast.makeText(getActivity(),"streamKey가 형식에 맞지 않습니다. 다시 확인해 주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Inflate the layout for this fragment
        return view;
    }
}