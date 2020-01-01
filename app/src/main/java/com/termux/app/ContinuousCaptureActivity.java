package com.termux.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.termux.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContinuousCaptureActivity extends AppCompatActivity {

    private String jTypes;
    private String jSections;
    private String jSubjects;

    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private String lastText;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)){
                return;
            }
            lastText = result.getText();
            showToast(result.getText());
            verifyStudentId(result.getText(), jTypes, jSections, jSubjects);
            barcodeView.setStatusText("Place inside the viewfinder\n");
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };

    public void showToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void verifyStudentId(String id, String types, String sections, String subjects) throws RuntimeException {
        String URL = Constants.VERIFY_URL;
        JSONObject jsonObject = new JSONObject();

        final String getUrl = URL + "/?value=" + id + "&type=" + types + "&section=" + sections + "&subject=" + subjects;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getUrl, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String[] respond = {response.getString("status")};
                    final String success = Arrays.toString(respond);

                    if (success.equals("[200]")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ContinuousCaptureActivity.this);
                        builder.setTitle("Success");
                        builder.setMessage("Student data successfully verified.");
                        builder.setCancelable(false);
                        playSuccessSound();
                        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ContinuousCaptureActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage("Verifying student data has failed.");
                        builder.setCancelable(false);
                        playErrorSound();
                        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    Log.i("RESPONSE", response.toString());
                    Log.d("URL", getUrl);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Error Result", String.valueOf(error));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                final Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(getBaseContext()).add(jsonObjectRequest);

    }

    private void playErrorSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.failed);
        mediaPlayer.setVolume(300, 300);
        mediaPlayer.start();
    }

    private void playSuccessSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.success);
        mediaPlayer.setVolume(300, 300);
        mediaPlayer.start();
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_capture);

        Intent intent = getIntent();
        jTypes = intent.getStringExtra("mTypes");
        jSections = intent.getStringExtra("mSections");
        jSubjects = intent.getStringExtra("mSubjects");

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
            .request(Manifest.permission.CAMERA) // ask single or multiple permission once
            .subscribe(granted -> {
                if (granted) {
                    barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
                    Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
                    barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
                    barcodeView.initializeFromIntent(getIntent());
                    barcodeView.decodeContinuous(callback);
                    barcodeView.setStatusText("Place inside the viewfinder\n");
                }else {
                    finish();
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
