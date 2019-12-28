package com.termux.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
*
*  Save sdcard memory storage
*  Verify Student ID
*
* */

public class WebUtil extends AppCompatActivity {
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

    public static void extractPhpPackage(File zipFile, File targetDirectory) throws IOException {
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

    public void deleteZip() {
        try {
            Os.remove(Constants.WWW_PHP_ZIP);
        } catch (ErrnoException e) {
            e.printStackTrace();
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
