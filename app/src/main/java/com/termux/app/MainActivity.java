package com.termux.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.system.ErrnoException;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.termux.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    TermuxService mTermService;

    public String mTypes;
    public String mSections;
    public String mSubjects;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"CheckResult", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String cpu = Build.CPU_ABI;
        Log.e("CPU", "Arch " + cpu);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTypes();
                Log.d("FAB", "Clicked");
            }
        });

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE) // ask single or multiple permission once
            .subscribe(granted -> {
                if (granted) {
                    // All requested permissions are granted
                    TermuxInstaller.setupIfNeeded(MainActivity.this, () -> {
                        ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.HOME_LOCATION);
                        ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.ATTENDO_PATH);
                        ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.ATTENDO_PACKAGE_PATH);
                        ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.ATTENDO_PHP_PATH);
                        ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.DOC_ROOT);
                        copyFileOrDir(Constants.ASSETS_PATH, Constants.DESTINATION_ASSETS);

                        File dir = new File(Constants.WWW_PHP_PATH + "/tmp"); //locate temporary directory
                        if (!dir.exists() && !dir.isDirectory()) {
                            /* Fix issue https://github.com/isaacdarcilla/attendo/issues/3 */
                            ensureDirectories(Constants.MAKE_DIR_COMMAND, Constants.WWW_PHP_PATH + "/tmp");
                            getPhpAssets(Constants.WWW_PATH, Constants.WWW_ASSETS);
                            WebUtil webUtil = new WebUtil();
                            try {
                                WebUtil.extractPhpPackage(new File(Constants.WWW_PHP_ZIP), new File(Constants.WWW_PHP_PATH));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            progressInstalling();
                            Log.e("Not Found", "Not Found - Creating ");
                        } else {
                            //Just start server & launch webview
                            Log.e("Found", "Found - Ignore ");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    startServer();
                                }
                            }).start();
                            launchWebView();
                        }

                        if (mTermService == null) return; // Activity might have been destroyed.

                        try {
                            Bundle bundle = getIntent().getExtras();
                            boolean launchFailsafe = false;
                            if (bundle != null) {
                                launchFailsafe = bundle.getBoolean(Constants.TERMUX_FAILSAFE_SESSION_ACTION, false);
                            }
                        } catch (WindowManager.BadTokenException e) {

                        }
                    });
                } else {
                    finish();
                }
            });
    }

    private void progressInstalling() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Attendo is starting the server. Please wait...");
        progressDialog.show();

        checkArchitecture();
        WebUtil webUtil = new WebUtil();
        try {
            webUtil.deletePackageAfterInstall();
            webUtil.deleteZip();
        } catch (ErrnoException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                copyLibFiles();
                changeMode();
                startServer();
            }
        }).start();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progressDialog.cancel();
            }
        };

        launchWebView();

        Handler handler = new Handler();
        handler.postDelayed(runnable, 5000);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void launchWebView() {
        /*Intent intent = new Intent(this,ScanActivity.class);
        startActivity(intent);*/

        WebView myWebView = findViewById(R.id.web_view_viewer);

        Runnable runnable = new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {

                WebSettings webSettings = myWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowContentAccess(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setAllowFileAccessFromFileURLs(true);
                webSettings.setAllowUniversalAccessFromFileURLs(true);
                webSettings.setMediaPlaybackRequiresUserGesture(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setSupportZoom(false);

                myWebView.setWebViewClient(new WebViewClient());
                myWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onPermissionRequest(final PermissionRequest request) {
                        Log.d("onPermissionRequest", "Camera");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @TargetApi(Build.VERSION_CODES.M)
                            @Override
                            public void run() {
                                if (request.getOrigin().toString().equals("http://0.0.0.0:3000/")) {
                                    Log.d("onPermissionRequest", "ACCESS");
                                    request.grant(request.getResources());
                                } else {
                                    request.deny();
                                }
                            }
                        });
                    }
                });

                myWebView.loadUrl("http://0.0.0.0:3000/");
            }
        };

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                getSupportActionBar().show();
                return true;
            }
        });

        myWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        /*Toast.makeText(getApplicationContext(),"Service started",Toast.LENGTH_LONG).show();*/

        Handler handler = new Handler();
        handler.postDelayed(runnable, 7000);
    }

    private void executeInstall(String architecture) {
        String arch = Build.CPU_ABI;

        if (arch.equals(Constants.AARCH64)) {
            try {
                extractPackage(new File(Constants.AARCH64_ZIP), new File(Constants.ZIP_DEST));
                Log.e("AARCH", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (arch.equals(Constants.X86_64)) {
            try {
                extractPackage(new File(Constants.X86_64_ZIP), new File(Constants.ZIP_DEST));
                Log.e("X86_64", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (arch.equals(Constants.I386)) {
            try {
                extractPackage(new File(Constants.X86_ZIP), new File(Constants.ZIP_DEST));
                Log.e("X86", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkArchitecture() {
        String abi = Build.CPU_ABI;
        Toast.makeText(getApplicationContext(), abi, Toast.LENGTH_LONG);

        if (abi.equals(Constants.AARCH64)) {
            executeInstall(Constants.AARCH64);
        }

        if (abi.equals(Constants.X86_64)) {
            executeInstall(Constants.X86_64);
        }

        if (abi.equals(Constants.I386)) {
            executeInstall(Constants.I386);
        }
    }

    public void copyLibFiles() {
        String[] command = {Constants.SERVER_SHELL, "-c", Constants.COPY_LIB};
        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            process.waitFor();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extractPackage(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
            new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                        dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    public void changeMode() {
        try {
            Process proc = Runtime.getRuntime()
                .exec(Constants.CHANGE_MOD);
            proc.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void startServer() {
        try {
            String[] command = {Constants.SERVER_SHELL, "-c", Constants.SERVER_START};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void ensureDirectories(String command, String directory) {
        try {
            Process process = Runtime.getRuntime().exec(command + " " + directory);
            Log.e("Make - ", "Directory");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            createPackageDir();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPackageDir() {
        try {
            Process process = Runtime.getRuntime().exec(Constants.MAKE_DIR_COMMAND + " " + Constants.PACKAGE_LOCATION);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            process.waitFor();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void getPhpAssets(String path, String destinationDir) {
        AssetManager assetManager = this.getAssets();
        String[] assets = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyPhpFile(path, destinationDir);
            } else {
                String fullPath = destinationDir + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdir();
                for (int i = 0; i < assets.length; ++i) {
                    getPhpAssets(path + "/" + assets[i], destinationDir + path + "/" + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyPhpFile(String filename, String destinationDir) {
        AssetManager assetManager = this.getAssets();
        String newFileName = destinationDir;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("FILE - ", e.getMessage());
        }
        new File(newFileName).setExecutable(true, false);
    }

    public void copyFileOrDir(String path, String destinationDir) {
        AssetManager assetManager = this.getAssets();
        String[] assets = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path, destinationDir);
            } else {
                String fullPath = destinationDir + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdir();
                for (int i = 0; i < assets.length; ++i) {
                    copyFileOrDir(path + "/" + assets[i], destinationDir + path + "/" + assets[i]);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename, String destinationDir) {
        AssetManager assetManager = this.getAssets();
        String newFileName = destinationDir;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("FILE - ", e.getMessage());
        }
        new File(newFileName).setExecutable(true, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean mIsVisible = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            Log.d("Refresh", "Refresh activated");
            WebUtil webUtil = new WebUtil();
            webUtil.kill();
            progressInstalling();
            return true;
        }
        if (id == R.id.settings) {
            Log.d("Settings - ", "Settings activated");
            launchSettings();
            return true;
        }
        if (id == R.id.exit) {
            Log.d("Exit", "Exit activated");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void launchSettings() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void getTypes() {
        String URL = Constants.TYPES_URL;
        JSONObject jsonObject = new JSONObject();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String[] type = {response.getString("data").replace("\":\"", "~")};
                    final String[] item = Arrays.toString(type)
                        .replace("[{", "")
                        .replace("}]", "")
                        .replace("\"", "")
                        .replaceAll("[0-9]*[~]", "")
                        .split(",");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Select Type");
                    builder.setCancelable(false);

                    int checkedItem = -1;
                    builder.setSingleChoiceItems(item, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mTypes = item[which];
                            Toast.makeText(MainActivity.this, item[which] + " selected", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getSubjects();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Log.i("Volley Type Result ", response.getString("data"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley Type Result", String.valueOf(error));
            }
        });
        Volley.newRequestQueue(getBaseContext()).add(jsonObjectRequest);
    }

    public void getSubjects() {
        String URL = Constants.SUBJECTS_URL;
        JSONObject jsonObject = new JSONObject();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String[] type = {response.getString("data").replace("\":\"", "~")};
                    final String[] item = Arrays.toString(type)
                        .replace("[{", "")
                        .replace("}]", "")
                        .replace("\"", "")
                        .replaceAll("[0-9]*[~]", "")
                        .split(",");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Select Subject");
                    builder.setCancelable(false);

                    int checkedItem = -1;
                    builder.setSingleChoiceItems(item, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSubjects = item[which];
                            Toast.makeText(MainActivity.this, item[which] + " selected", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getSections();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Log.i("Volley Type Result ", response.getString("data"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley Subject Result", String.valueOf(error));
            }
        });
        Volley.newRequestQueue(getBaseContext()).add(jsonObjectRequest);
    }

    public void getSections() {
        String URL = Constants.SECTIONS_URL;
        JSONObject jsonObject = new JSONObject();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    final String[] type = {response.getString("data").replace("\":\"", "~")};
                    final String[] item = Arrays.toString(type)
                        .replace("[{", "")
                        .replace("}]", "")
                        .replace("\"", "")
                        .replaceAll("[0-9]*[~]", "")
                        .split(",");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Select Section");
                    builder.setCancelable(false);

                    int checkedItem = -1;
                    builder.setSingleChoiceItems(item, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSections = item[which];
                            Toast.makeText(MainActivity.this, item[which] + " selected", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scanContinuous();
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Log.i("Volley Type Result ", Arrays.toString(item));
                    Log.i("Volley Type Result ", response.getString("data"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley Section Result", String.valueOf(error));
            }
        });
        Volley.newRequestQueue(getBaseContext()).add(jsonObjectRequest);
    }

    public void scanContinuous() {
        Intent intent = new Intent(this, ContinuousCaptureActivity.class);
        intent.putExtra("mTypes", mTypes);
        intent.putExtra("mSections", mSections);
        intent.putExtra("mSubjects", mSubjects);
        startActivity(intent);
    }
}
