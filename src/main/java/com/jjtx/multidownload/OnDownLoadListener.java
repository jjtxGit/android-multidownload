package com.jjtx.multidownload;

import java.io.File;

/**
 * Created by jjtx on 2016/7/25.
 */
public interface OnDownLoadListener {
    public void onStart(int startIndex, int totalIndex);

    /**
     * 该方法将在子线程里被调用 请不要在该方法里更新ui界面（但可以改变进度条的进程）
     *
     * @param currentProcess
     */
    public void onChange(int currentProcess);

    public void onEnd(File file);

    public void onPause(int currentProcess, File file);

    public void onDestroy();
}
