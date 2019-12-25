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
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.termux.R;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    TermuxService mTermService;

    private Button mWebLaunchButton;
    private RadioButton mStartServerButton;
    private RadioButton mStopServerButton;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"CheckResult", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String cpu = Build.CPU_ABI;
        Log.e("CPU", "Arch " + cpu);

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
                        progressInstalling();

                        if (mTermService == null) return; // Activity might have been destroyed.

                        try {
                            Bundle bundle = getIntent().getExtras();
                            boolean launchFailsafe = false;
                            if (bundle != null) {
                                launchFailsafe = bundle.getBoolean(Constants.TERMUX_FAILSAFE_SESSION_ACTION, false);
                            }
                        } catch (WindowManager.BadTokenException e) {
                            // Activity finished - ignore.
                        }
                    });
                } else {
                    // At least one permission is denied
                }
            });

        /* Redesigned

        mWebLaunchButton = (Button) findViewById(R.id.view_web_view);
        mWebLaunchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchWebView();
            }
        });

        mStopServerButton = (RadioButton) findViewById(R.id.stop_server);
        mStopServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killPhpProcess();
                Toast.makeText(getApplicationContext(),"Service stopped",Toast.LENGTH_LONG).show();
            }
        });

        mStartServerButton = (RadioButton) findViewById(R.id.start_server);
        mStartServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkArchitecture();
                Toast.makeText(getApplicationContext(),"Service started",Toast.LENGTH_LONG).show();
                new Thread( new Runnable() { @Override public void run() {
                    copyLibFiles();
                    changeMode();
                    startServer();
                } } ).start();
            }
        });*/
    }

    private  void progressInstalling() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Attendo is starting the server. Please wait...");
        progressDialog.show();

        checkArchitecture();
        WebUtil webUtil = new WebUtil();
        try {
            webUtil.deletePackageAfterInstall();
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
        getPhpAssets(Constants.WWW_PATH, Constants.WWW_ASSETS);
        webUtil.copyIndexToSdcard();

        new Thread( new Runnable() { @Override public void run() {
            copyLibFiles();
            changeMode();
            startServer();
        } } ).start();

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

        WebView myWebView = (WebView) findViewById(R.id.web_view_viewer);

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
                myWebView.setWebChromeClient(new WebChromeClient(){
                    @Override
                    public void onPermissionRequest(final PermissionRequest request) {
                        Log.d("onPermissionRequest","Camera");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @TargetApi(Build.VERSION_CODES.M)
                            @Override
                            public void run() {
                                if(request.getOrigin().toString().equals("http://0.0.0.0:3000/")) {
                                    Log.d("onPermissionRequest","ACCESS");
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

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
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

    private void executeInstall(String architecture){
        String arch = Build.CPU_ABI;

        if(arch.equals(Constants.AARCH64)){
            try {
                extractPackage(new File("/mnt/sdcard/.attendo/packages/php/arm64-v8a.zip"),new File("/data/data/com.termux/files/home/packages/"));
                Log.e("AARCH", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(arch.equals(Constants.X86_64)) {
            try {
                extractPackage(new File("/mnt/sdcard/.attendo/packages/php/x86_64.zip"),new File("/data/data/com.termux/files/home/packages/"));
                Log.e("X86_64", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(arch.equals(Constants.I386)) {
            try {
                extractPackage(new File("/mnt/sdcard/.attendo/packages/php/x86.zip"),new File("/data/data/com.termux/files/home/packages/"));
                Log.e("X86", "Installing " + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkArchitecture() {
        String abi = Build.CPU_ABI;
        Toast.makeText(getApplicationContext(), abi,Toast.LENGTH_LONG);

        if(abi.equals(Constants.AARCH64)) {
            executeInstall(Constants.AARCH64);
        }

        if(abi.equals(Constants.X86_64)){
            executeInstall(Constants.X86_64);
        }

        if(abi.equals(Constants.I386)){
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
                copyPhpFile(path,destinationDir);
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
        String assets[] = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path,destinationDir);
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
        int id =item.getItemId();
        if(id == R.id.scan) {
            Log.d("Scan", "Scan activated");
            return true;
        }
        if(id == R.id.fullscreen_button) {
            Log.d("Fullscreen", "Fullscreen activated");
            getSupportActionBar().hide();
            return true;
        }
        if(id == R.id.refresh) {
            Log.d("Refresh", "Refresh activated");
            WebUtil webUtil = new WebUtil();
            webUtil.kill();
            progressInstalling();
            return true;
        }
        if(id == R.id.network_stat) {
            Log.d("Network - ", "IP " + getIp());
            ipDialog();
            return true;
        }
        if(id == R.id.exit) {
            Log.d("Exit", "Exit activated");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void launchFullScreen() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    public void ipDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setCancelable(false);
        dialog.setTitle("Network Information");
        dialog.setMessage("Access attendo on your laptop by visiting http://" + getIp() + ":3000");
        dialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    public String getIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return ip;
    }
}
