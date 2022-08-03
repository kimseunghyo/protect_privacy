package com.example.dlibmodule;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

//한나:정보담기위해 만든 객체
public class User extends Application {
    private String photourl;
    private String id;
    private String pw;
    private String streamKey;

    @Override
    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public User(){}

    public String getPhotourl() {
        return photourl;
    }

    public void setPhotourl(String profile) {
        this.photourl = profile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getStreamKey() { return streamKey; }

    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
}
