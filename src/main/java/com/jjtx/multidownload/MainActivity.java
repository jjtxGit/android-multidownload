package com.jjtx.multidownload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar = null;
    private MultiDownload download = new MultiDownload(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    public void click(View view) {

        String urlPath = "http://msoftdl.360.cn/mobile/shouji360/360safe/360MobileSafe_7.0.0.1062.apk";

        try {

            download.prepare(urlPath, null, null);

            Toast.makeText(MainActivity.this, "before setOndownloadListener", Toast.LENGTH_SHORT).show();

            download.setOnDownLoadListener(new OnDownLoadListener() {
                @Override
                public void onStart(int startIndex, int totalIndex) {
                    progressBar.setMax(totalIndex);
                    progressBar.setProgress(startIndex);
                    Toast.makeText(MainActivity.this, "start", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onChange(int currentProcess) {
                    progressBar.setProgress(currentProcess);
                }

                @Override
                public void onEnd(File file) {
                    Toast.makeText(MainActivity.this, "end", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onPause(int currentProcess, File file) {
                    Toast.makeText(MainActivity.this, "listener :onPause", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDestroy() {
                    Toast.makeText(MainActivity.this, "destroy", Toast.LENGTH_SHORT).show();

                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }


        if (download.isDownloading()) {
            download.pause();
            Toast.makeText(MainActivity.this, "暂定下载", Toast.LENGTH_SHORT).show();
            return;
        }


        download.startDownload();


    }
}
