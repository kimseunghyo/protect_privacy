package com.example.dlibmodule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;


//영상화면
public class Fragment3 extends Fragment {
    private View view;

    //로그인 회원 정보를 다른 Activity에 전달하도록 전역 변수 설정....
    User myUser;

    private WebView mWebView; // 웹뷰 선언
    private WebSettings mWebSettings; //웹뷰서링

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.movie_list, container, false);

        mWebView = (WebView) view.findViewById(R.id.webView);

        mWebView.setWebViewClient(new WebViewClient()); //클릭시 새창 안뜨게
        mWebSettings = mWebView.getSettings(); //세부 세팅 등록
        mWebSettings.setJavaScriptEnabled(true); //웹페이지 자바스클비트 허용 여부
        mWebSettings.setSupportMultipleWindows(false); //새창 띄우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false); //자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        mWebSettings.setLoadWithOverviewMode(true); //메타태그 허용 여부
        mWebSettings.setUseWideViewPort(true); //화면 사이즈 맞추기 허용 여부
        mWebSettings.setSupportZoom(true); //화면 줌 허용 여부
        mWebSettings.setBuiltInZoomControls(true); //화면 확대 축소 허용 여부
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); //컨텐츠 사이즈 맞추기
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); //브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true); //로컬저장소 허용 여부


        mWebView.loadUrl("https://studio.youtube.com");

        // Inflate the layout for this fragment
        return view;
    }
}