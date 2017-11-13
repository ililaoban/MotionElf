package com.game.motionelf.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Objects;

import com.game.motionelf.R;

public class ActivityStart extends Activity {

    private String copiedPath;

    private static final int MODE = 00755;

    private static final String TAG = "MotionElf";

    private static final String BREVENT = "me.piebridge.brevent";

    private static final int DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File brevent = getBootstrapFile();
        if (brevent != null) {
            String path = brevent.getPath();
            if (!Objects.equals(copiedPath, path) && copyBrevent(path) != null) {
                copiedPath = path;
            }
            PackageManager packageManager = getPackageManager();
            startActivity(packageManager.getLaunchIntentForPackage(BREVENT));
            ComponentName cn = new ComponentName(getPackageName(),
                    "com.game.motionelf.activity.ActivityAlias");
            if (packageManager.getComponentEnabledSetting(cn) != DISABLED) {
                packageManager.setComponentEnabledSetting(cn, DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
        finish();
    }

    private File getBootstrapFile() {
        try {
            PackageManager packageManager = getPackageManager();
            String nativeLibraryDir = packageManager
                    .getApplicationInfo(BREVENT, 0).nativeLibraryDir;
            File file = new File(nativeLibraryDir, "libbrevent.so");
            if (file.exists()) {
                return file;
            } else {
                d("Can't find libbrevent.so");
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            d("Can't find " + BREVENT, e);
            return null;
        }
    }

    public String copyBrevent(String breventPath) {
        File files = getFilesDir();
        File parent = files.getParentFile();
        String father = parent.getParent();
        try {
            father = Os.readlink(father);
            parent = new File(father, parent.getName());
            files = new File(parent, files.getName());
        } catch (ErrnoException e) {
            d("Can't read link for " + father, e);
        }
        File output = new File(files, "motionelf_server");
        try (
                InputStream is = getResources().openRawResource(R.raw.brevent);
                OutputStream os = new FileOutputStream(output);
                PrintWriter pw = new PrintWriter(os)
        ) {
            pw.println("path=" + breventPath);
            pw.println("abi64=" + breventPath.contains("64"));
            pw.println();
            pw.flush();
            byte[] bytes = new byte[0x400];
            int length;
            while ((length = is.read(bytes)) != -1) {
                os.write(bytes, 0, length);
            }
            copiedPath = breventPath;
        } catch (IOException e) {
            d("Can't copy brevent", e);
            return null;
        }
        try {
            Os.chmod(output.getPath(), MODE);
            Os.chmod(files.getPath(), MODE);
            Os.chmod(parent.getPath(), MODE);
        } catch (ErrnoException e) {
            d("Can't chmod brevent", e);
            return null;
        }
        return output.getPath();
    }

    private void d(String s) {
        android.util.Log.d(TAG, s);
    }

    private void d(String s, Throwable t) {
        android.util.Log.d(TAG, s, t);
    }

}