package com.serafimtech.serafimaudio.FileData;

import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

public class FTP {
    private static final String TAG = FTP.class.getSimpleName();
    private final FTPClient ftpClient;
    private final String ftpUrl;
    private final int ftpPort;
    private final String userName;
    private final String userPassword;

    public FTP() {
        ftpUrl = "160.251.17.13";
        ftpPort = 21;
        userName = "app";
        userPassword = "RdnmhsB8csfWAGDd";
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8"); //一定要放在connect之前, 不然第一次一定是亂碼
        ftpClient.setConnectTimeout(5000);//設定timeout, 預設為0, 沒有timeout, 會一直等待連線
    }

    public FTPFile[] listDir(String path) {
        if (!ftpClient.isConnected()) {
            if (!initFtp()) return null;
        }
        try {
            ftpClient.setBufferSize(1024);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            return ftpClient.listFiles(path);
        } catch (IOException e) {
            Log.e(TAG, "listFile error : " + e.getMessage());
        }
        return null;
    }

    private boolean initFtp() {
        int reply;
        try {
            ftpClient.connect(ftpUrl, ftpPort);
            ftpClient.login(userName, userPassword);
            reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                return false;
            }
            return true;
        } catch (SocketException e) {
            Log.e(TAG, "initFtp SocketException : " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "initFtp IOException : " + e.getMessage());
        }
        return false;
    }

    public boolean downLoadFile(File remoteFile, File localFile) {
        listDir(remoteFile.getPath());
        if (!ftpClient.isConnected()) {
            if (!initFtp()) {
                return false;
            }
        }
        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
            ftpClient.retrieveFile(remoteFile.getPath(), outputStream);
            outputStream.close();
            ftpClient.logout();
            ftpClient.disconnect();
            Log.i(TAG, "下載成功");
        } catch (IOException e) {
            Log.e(TAG, "downloadFile error : " + e.getMessage());
        }
        return true;
    }

    public long getFileSize(String filePath) {
        long fileSize = 0;

        FTPFile[] files = listDir(filePath);

        if (files.length == 1 && files[0].isFile()) {
            fileSize = files[0].getSize();
        }

        Log.i(TAG, "File size = " + fileSize);

        return fileSize;
    }

    //<editor-fold desc="<not yet>">
    public boolean chdir(String path) {
        //todo
        return true;
    }

    private boolean deleteSingleFile(String filePath$Name) {
        File file = new File(filePath$Name);
        // 如果檔案路徑所對應的檔案存在，並且是一個檔案，則直接刪除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.i(TAG, "Copy_Delete.deleteSingleFile: 刪除單個檔案" + filePath$Name + "成功！");
                return true;
            } else {
                Log.e(TAG,"刪除失敗");
                return false;
            }
        } else {
            Log.e(TAG,"刪除失敗,檔案不存在");
            return false;
        }
    }

    public long uploadFile(String sourcePath, String sourceFile, String targetPath, String targetFile) {
        long size = 0;
        if (!initFtp()) {
            return 0;
        }
        try {
            //檢查遠端伺服器是否有相同的文件, 有的話表示上次沒傳成功, 需先砍掉
            FTPFile[] files = ftpClient.listFiles(targetPath + "/" + targetFile);
            if (files.length == 1) ftpClient.deleteFile(targetPath + "/" + targetFile);
            ftpClient.setBufferSize(1024);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            String[] paths = targetPath.split("/");
            for (String s : paths) {
                ftpClient.makeDirectory(s);
                ftpClient.changeWorkingDirectory(s);
            }
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(sourcePath, sourceFile)));

            ftpClient.storeFile(targetFile, bis);
            bis.close();
            size = ftpClient.listFiles(targetPath + "/" + targetFile)[0].getSize();
            ftpClient.logout();
            ftpClient.disconnect();

        } catch (IOException e) {
            Log.e(TAG, "upLoadFile error : " + e.getMessage());
        }
        return size;
    }
    //</editor-fold>
}