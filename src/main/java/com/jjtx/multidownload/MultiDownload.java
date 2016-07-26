package com.jjtx.multidownload;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Created by jjtx on 2016/7/25.
 */
public class MultiDownload {


    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            onDownLoadListener.onEnd(targetFile);
        }
    };


    private final static String DEFAULT_FILE_PATH = "/sdcard/";
    private final static int THREAD_COUNT = 3;
    private static int livedThread = THREAD_COUNT;
    private static boolean isDownloadIng = false;
    private static boolean isPause = false;


    public MultiDownload(Context context) {
        this.context = context;
        this.multiDownloadDao = new MultiDownloadDao(context);
        isPause = false;
        isDownloadIng = false;
        onDownLoadListener = new DefaultOnDownLoadListener();
    }

    private MultiDownloadDao multiDownloadDao;
    private Context context;
    private URL url;
    private OnDownLoadListener onDownLoadListener;
    private File targetFile;
    private int totalLength;

    /**
     * 根据字符串路径获取URL路径
     *
     * @param urlPath
     * @return
     * @throws MalformedURLException
     */
    private URL getUrl(String urlPath) throws MalformedURLException {
        URL url = new URL(urlPath);
        return url;
    }

    private String getDefaultFileName(URL url) {

        String urlPath = url.getFile();
        String[] paths = urlPath.split("/");
        String defaultFileName = paths[paths.length - 1];
        return defaultFileName;
    }

    /**
     * 设置连接 准备连接的属性
     *
     * @param urlPath
     * @param filePath
     * @param fileName
     * @throws IOException
     */
    public void prepare(String urlPath, String filePath, String fileName) throws IOException {

        if (urlPath == null) {
            throw new MalformedURLException();
        }

        this.url = getUrl(urlPath);

        if (filePath == null) {
            filePath = DEFAULT_FILE_PATH;
        }

        if (fileName == null) {
            fileName = getDefaultFileName(url);
        }

        this.targetFile = new File(filePath, fileName);

        doPrepare();
    }

    public void setOnDownLoadListener(OnDownLoadListener onDownLoadListener) {

        if (onDownLoadListener != null) {
            this.onDownLoadListener = onDownLoadListener;
        }

    }

    /**
     * 开启下载
     */
    public void startDownload() {

        if (isDownloadIng) {//如果已经开始下载了，就不能再下载
            return;
        }

        doStartDownload();
    }

    private void doStartDownload() {


        int eachBlock = totalLength / THREAD_COUNT;

        try {
            RandomAccessFile file = new RandomAccessFile(targetFile, "rw");
            file.setLength(totalLength);
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


        int[] startIndex = new int[THREAD_COUNT];
        int[] endIndex = new int[THREAD_COUNT];
        String threadId[] = new String[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {

            startIndex[i] = i * eachBlock;
            endIndex[i] = (i + 1) * eachBlock - 1;
            if (i == THREAD_COUNT - 1) {
                endIndex[i] = totalLength;
            }

            threadId[i] = targetFile.getName() + i + "";

            if (multiDownloadDao.hasProcess(threadId[i])) {//如果记录的有进度
                startIndex[i] = multiDownloadDao.getStartIndex(threadId[i]);
            } else {//如果记录的没有进度
                multiDownloadDao.addProcess(threadId[i], startIndex[i]);
            }


        }


        int totalStartIndex = 0;
        totalStartIndex = multiDownloadDao.getSumIndex(targetFile.getName(), THREAD_COUNT) - totalLength;
        isDownloadIng = true;
        isPause = false;
        onDownLoadListener.onStart(totalStartIndex, totalLength);

        for (int i = 0; i < THREAD_COUNT; i++) {
            DownloadThread downloadThread = new DownloadThread(startIndex[i], endIndex[i], targetFile, url.toString(), threadId[i]);
            downloadThread.start();
        }

    }

    /**
     * 配置好connection
     */
    private void doPrepare() {

        PrepareThread prepareThread = new PrepareThread();
        prepareThread.setUrl(this.url);
        Thread t1 = new Thread(prepareThread);

        t1.start();

        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.totalLength = prepareThread.getTotalLength();

    }

    private class PrepareThread implements Runnable {

        private URL url;

        private int totalLength;

        public int getTotalLength() {
            return totalLength;
        }

        public void setUrl(URL url) {
            this.url = url;

        }


        @Override
        public void run() {

            try {

                HttpURLConnection connection = (HttpURLConnection) (this.url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                int code = connection.getResponseCode();

                if (code == 200) {
                    this.totalLength = connection.getContentLength();
                    connection.disconnect();
                } else {
                    throw new ConnectException();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    private class DownloadThread extends Thread {

        private String threadId;
        private int startIndex;
        private int endIndex;
        private File targetFile;
        private String urlPath;

        DownloadThread(int startIndex, int endIndex, File targetFile, String urlPath, String threadId) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.targetFile = targetFile;
            this.threadId = threadId;
            this.urlPath = urlPath;
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;
            try {

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);

                int responseCode = connection.getResponseCode();

                if (responseCode == 206) {
                    byte[] bytes = new byte[1024];
                    int len = 0;

                    RandomAccessFile file = new RandomAccessFile(targetFile, "rwd");
                    file.seek(startIndex);
                    InputStream is = connection.getInputStream();
                    int totalProcess = startIndex;

                    while (!isPause && ((len = is.read(bytes)) != -1)) {
                        file.write(bytes, 0, len);
                        totalProcess += len;
                        multiDownloadDao.updateProcess(threadId, totalProcess);
                        onDownLoadListener.onChange(multiDownloadDao.getSumIndex(targetFile.getName(), THREAD_COUNT) - totalLength);
                    }

                    file.close();
                    is.close();


                } else {
                    throw new ConnectException();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishThread();
            }

        }


        public synchronized void finishThread() {

            if (!isPause) {
                livedThread--;
            }

            if ((livedThread == 0)) {

                if (!isPause) {//如果没有暂停
                    multiDownloadDao.deleteAll(targetFile.getName(), THREAD_COUNT);
                    handler.sendMessage(new Message());
                }

                isDownloadIng = false;
            }

        }
    }


    public void pause() {
        isPause = true;
        isDownloadIng = false;
        onDownLoadListener.onPause(multiDownloadDao.getSumIndex(targetFile.getName(), THREAD_COUNT) - totalLength, targetFile);
    }

    public void destroy() {
        pause();
        multiDownloadDao.deleteAll(targetFile.getName(), THREAD_COUNT);
        targetFile.getAbsoluteFile().delete();
        onDownLoadListener.onDestroy();
    }

    public boolean isDownloading() {
        return isDownloadIng;
    }


}
