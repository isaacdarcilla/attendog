package com.termux.app;

import android.content.res.AssetManager;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/*
*
*  Save sdcard memory storage
*
* */

public class WebUtil extends MainActivity{
    public void kill() {
        try {
            String[] command = {Constants.SERVER_SHELL, "-c", "pkill php"};
            Log.e("PHP", "Killed");
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
    public void copyIndexToSdcard() {
        try {
            String[] command = {Constants.SERVER_SHELL, "-c", Constants.COPY_ASSETS};
            Log.e("PHP", "Copy assets");
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

    public void deletePackageAfterInstall() throws ErrnoException {
        String architecture = Build.CPU_ABI;

        if(architecture.equals(Constants.AARCH64)){
           Os.remove(Constants.ATTENDO_PHP_PATH + "x86_64.zip");
           Os.remove(Constants.ATTENDO_PHP_PATH + "x86.zip");
           Log.v("DELETE", "For " + architecture);
        }

        if(architecture.equals(Constants.X86_64)) {
            Os.remove(Constants.ATTENDO_PHP_PATH + "arm64-v8a.zip");
            Os.remove(Constants.ATTENDO_PHP_PATH + "x86.zip");
            Log.v("DELETE", "For " + architecture);
        }

        if(architecture.equals(Constants.I386)) {
            Os.remove(Constants.ATTENDO_PHP_PATH + "arm64-v8a.zip");
            Os.remove(Constants.ATTENDO_PHP_PATH + "x86_64.zip");
            Log.v("DELETE", "For " + architecture);
        }
    }
}
