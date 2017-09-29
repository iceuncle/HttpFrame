package com.hd123.httpframe.net.extension;

import rx.Subscriber;

/**
 * 界面描述： BaseSubscriber基类，处理返回数据
 * <p>
 * Created by tianyang on 2017/9/27.
 */

public abstract class BaseSubscriber<T> extends Subscriber<T> {

    @Override
    public void onCompleted() {

    }
}
