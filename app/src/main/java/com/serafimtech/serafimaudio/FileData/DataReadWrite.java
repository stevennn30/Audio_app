package com.serafimtech.serafimaudio.FileData;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DataReadWrite {
    private static final String TAG = DataReadWrite.class.getSimpleName();
    Context context;

    public final String DirPresetProgram = "DirPresetProgram";
    public final String DirCustomProgram = "DirCustomProgram";
    public final String FilePreset = "FilePreset";
    public final String FileCustom = "FileCustom";

    public DataReadWrite(Context context) {
        this.context = context;
    }

    public void WriteFile(String strContent, String dirName, String fileName) {
        FileOutputStream fos = null;
        try {
            File mypath = new File(WriteFiledir(dirName), fileName);
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            fos.write(strContent.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                Objects.requireNonNull(fos).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String ReadFile(String dirName, String fileName) {
        String content = "";
        try {
            File f = new File(getFiledir(dirName), fileName);
            FileInputStream input = new FileInputStream(f);
            InputStreamReader inputReader = new InputStreamReader(input, StandardCharsets.UTF_8);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            while ((line = buffReader.readLine()) != null) {
                content = content + line + "\n";
            }
            input.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, fileName + " doesn't exist.");
            return "";
        } catch (IOException | NullPointerException e) {
            Log.d(TAG, e.getMessage());
            return "";
        }
        return content;
    }

    public File WriteFiledir(String dirName) {
        try {
            if (!getFiledir(dirName).exists()) {
                getFiledir(dirName).mkdirs();
            }
            // Use the compress method on the BitMap object to write image to the OutputStream
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getFiledir(dirName);
    }

    private File getFiledir(String dirName) {
        File directory = new File(context.getFilesDir().getPath());
        if (dirName.split("/").length > 0) {
            for (int i = 0; i < dirName.split("/").length; i++) {
                directory = new File(directory, dirName.split("/")[i]);
            }
        } else {
            directory = new File(directory, dirName);
        }
        return directory;
    }

    public long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis;
            fis = new FileInputStream(file);
            size = fis.available();
            Log.e("獲取檔案大小", String.valueOf(size));
        } else {
            Log.e("獲取檔案大小", "檔案不存在!");
        }
        return size;
    }
}
