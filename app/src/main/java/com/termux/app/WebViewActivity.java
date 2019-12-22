package com.termux.app;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.termux.R;

import static android.app.PendingIntent.getActivity;
import static android.os.Build.*;

public class WebViewActivity extends AppCompatActivity {

    @SuppressLint({"SetJavaScriptEnabled", "CheckResult"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
            .request(Manifest.permission.CAMERA,
                /* Error after API 29
                Manifest.permission.CAPTURE_VIDEO_OUTPUT,
                Manifest.permission.CAPTURE_SECURE_VIDEO_OUTPUT,*/
                Manifest.permission.RECORD_AUDIO) // ask single or multiple permission once
            .subscribe(granted -> {
                if (granted) {
                    // All requested permissions are granted
                    Log.e("RxPermission", "Camera Perm");
                } else {
                    // At least one permission is denied
                }
            });

        WebView myWebView = (WebView) findViewById(R.id.web_view_viewer);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setDomStorageEnabled(true);

        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d("onPermissionRequest","Camera");
                WebViewActivity.this.runOnUiThread(new Runnable() {
                    @TargetApi(VERSION_CODES.M)
                    @Override
                    public void run() {
                        if(request.getOrigin().toString().equals("http://localhost:3000/www")) {
                            Log.d("onPermissionRequest","ACCESS");
                            request.grant(request.getResources());
                        } else {
                            request.deny();
                        }
                    }
                });
            }
        });

        myWebView.loadUrl("http://localhost:3000/www");

    }
}

