package com.termux.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.termux.R;
import com.termux.app.Constants;

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

public class MainActivity extends FragmentActivity {

    TermuxService mTermService;
    @SuppressLint("SdCardPath")
    public static final String TERMUX_FAILSAFE_SESSION_ACTION = "com.termux.app.failsafe_session";
    @SuppressLint("SdCardPath")
    public static final String DESTINATION_ASSETS = "/mnt/sdcard/.attendo/packages/";
    public static final  String ASSETS_PATH = "php";
    @SuppressLint("SdCardPath")
    public static final String HOME_LOCATION = "/data/data/com.termux/files/home/";
    @SuppressLint("SdCardPath")
    public static final String PACKAGE_LOCATION = "/data/data/com.termux/files/home/packages";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PATH = "/mnt/sdcard/.attendo/";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PACKAGE_PATH = "/mnt/sdcard/.attendo/packages/";
    @SuppressLint("SdCardPath")
    public static final String ATTENDO_PHP_PATH = "/mnt/sdcard/.attendo/packages/php/";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) // ask single or multiple permission once
            .subscribe(granted -> {
                if (granted) {
                    // All requested permissions are granted
                } else {
                    // At least one permission is denied
                }
            });

        TermuxInstaller.setupIfNeeded(MainActivity.this, () -> {

            runCommand();
            makeAttendoDir();
            makeAttendoPackageDir();
            makeAttendoPhpDir();
            copyFileOrDir(ASSETS_PATH, DESTINATION_ASSETS);
            dialogStartServer();

            if (mTermService == null) return; // Activity might have been destroyed.

            try {
                Bundle bundle = getIntent().getExtras();
                boolean launchFailsafe = false;
                if (bundle != null) {
                    launchFailsafe = bundle.getBoolean(TERMUX_FAILSAFE_SESSION_ACTION, false);
                }
               /*addNewSession(launchFailsafe, null);*/
            } catch (WindowManager.BadTokenException e) {
                // Activity finished - ignore.
            }
        });
    }

    private void executeInstall(String architecture){
        String arch = Build.CPU_ABI;

        if(arch.equals(Constants.AARCH64)){
            try {
                extractPackage(new File("/mnt/sdcard/.attendo/packages/php/arm64-v8a.zip"),new File("/data/data/com.termux/files/home/packages/"));
                Log.e("AARCH", "Installing" + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(arch.equals(Constants.X86_64)) {
            try {
                extractPackage(new File("/mnt/sdcard/.attendo/packages/php/x86_64.zip"),new File("/data/data/com.termux/files/home/packages/"));
                Log.e("X86_64", "Installing" + arch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkArchitecture() {
        String abi = Build.CPU_ABI;
        Toast.makeText(getApplicationContext(), abi,Toast.LENGTH_LONG);

        if(abi.equals(Constants.AARCH64)) {
            /*
             *  Run AARCH64 installer
             */
            executeInstall(Constants.AARCH64);


        }

        if(abi.equals(Constants.X86_64)){
            /*
            *  Run x86_64 installer
            */
            executeInstall(Constants.X86_64);
        }
    }

    public void dialogStartServer() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
            .setTitle("Server")
            .setMessage("Attendo will now start the server. Do you want to execute?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //set what would happen when positive button is clicked
                    checkArchitecture();
                    /*progress();*/
                    copyLibFiles();
                    changeMode();
                   /* startServer();
                    web();*/
             /*       */
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //set what should happen when negative button is clicked


                    finish();
                }
            })
            .show();
    }

    public void copyLibFiles() {
        String[] command = {"/data/data/com.termux/files/usr/bin/sh", "-c", "cd /data/data/com.termux/files/home/packages/libs ; cp * /data/data/com.termux/files/usr/lib"};
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec(command);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

      /*      // Call webView
            web();*/


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void progress() {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Start");
        progress.setMessage("Starting web server. Please wait...");
        progress.show();

        Runnable progressRunnable = new Runnable() {

            @Override
            public void run() {

                startServer();
                Toast.makeText(getApplicationContext(),"Service started",Toast.LENGTH_LONG).show();
            }
        };

        Handler pdCanceller = new Handler();
        pdCanceller.postDelayed(progressRunnable, 5000);
    }

    /*/data/data/com.termux/files/usr/bin/unzip /mnt/sdcard/Attendo/packages/php/php.zip*/



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
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } finally {
            zis.close();
        }
    }

    public void changeMode() {
        try {
            Process proc = Runtime.getRuntime()
                .exec("chmod 777 -R /data/data/com.termux/files/home");
            proc.waitFor();
            Log.v("CHMOD", proc.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.v("TAG", "exec failed");
        }
    }

    public void startServer() {
        try {
            // Executes the command.
            String[] command = {"/data/data/com.termux/files/usr/bin/sh", "-c", "cd /data/data/com.termux/files/home/packages/php-bin ; ./php -S 0.0.0.0:3000"};

            Process process = Runtime.getRuntime().exec(command);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

      /*      // Call webView
            web();*/


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeAttendoDir(){
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec("/data/data/com.termux/files/usr/bin/mkdir " + ATTENDO_PATH);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();
            createPackageDir();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeAttendoPackageDir(){
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec("/data/data/com.termux/files/usr/bin/mkdir " + ATTENDO_PACKAGE_PATH);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();
            createPackageDir();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeAttendoPhpDir(){
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec("/data/data/com.termux/files/usr/bin/mkdir " + ATTENDO_PHP_PATH);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();
            createPackageDir();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void runCommand(){
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec("/data/data/com.termux/files/usr/bin/mkdir " + HOME_LOCATION);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
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
            // Executes the command.
            Process process = Runtime.getRuntime().exec("/data/data/com.termux/files/usr/bin/mkdir " + PACKAGE_LOCATION);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

/*    public void web() {
        WebView myWebView = (WebView) findViewById(R.id.web_view);
        myWebView.loadUrl("http://0.0.0.0:3000");
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.getAllowContentAccess();
        webSettings.getAllowFileAccess();
        webSettings.getCacheMode();
    }*/

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
            Log.e("PATH - ", newFileName);
        }
        new File(newFileName).setExecutable(true, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean mIsVisible = true;
    }
}
