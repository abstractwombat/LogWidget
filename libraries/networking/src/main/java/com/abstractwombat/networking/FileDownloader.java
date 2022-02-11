package com.abstractwombat.networking;

import android.os.AsyncTask;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 4/23/2015.
 */
public class FileDownloader extends AsyncTask<Pair<String,String>, Void, String[]> {
    public interface FileDownloadListener{
        void fileDownloadComplete(String[] filesDownloaded);
    }

    private File mFile;
    private FileDownloadListener mFileDownloadListener;

    public FileDownloader(FileDownloadListener listener){
        mFileDownloadListener = listener;
    }

    @Override
    protected String[] doInBackground(Pair<String,String>... urlsAndFiles) {
        List<String> filesDownloaded = new ArrayList<String>();
        for (int i=0; i<urlsAndFiles.length; i++) {
            Pair<String,String> p = urlsAndFiles[i];
            try {
                InputStream in = new java.net.URL(p.first).openStream();
                File file = new File(p.second);
                copyInputStreamToFile(in, file);
                filesDownloaded.add(p.second);
            } catch (IOException e) {
            }
        }
        return filesDownloaded.toArray(new String[filesDownloaded.size()]);
    }

    @Override
    protected void onPostExecute(String[] filesDownloaded) {
        mFileDownloadListener.fileDownloadComplete(filesDownloaded);
    }

    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
