package com.hd123.httpframe;

import android.app.Application;

import com.hd123.httpframe.net.extension.BaseNetProvider;
import com.hd123.httpframe.net.retrofit.NetMgr;

/**
 * 界面描述：
 * <p>
 * Created by tianyang on 2017/9/27.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        NetMgr.getInstance().registerProvider(new BaseNetProvider());
    }
}
