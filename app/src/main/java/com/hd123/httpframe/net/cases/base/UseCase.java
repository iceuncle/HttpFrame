package com.hd123.httpframe.net.cases.base;

import com.hd123.httpframe.BuildConfig;
import com.hd123.httpframe.net.models.PagingReq;
import com.hd123.httpframe.net.retrofit.NetMgr;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 界面描述：
 * <p>
 * Created by tianyang on 2017/9/27.
 */

public abstract class UseCase<T> {
    //用于分页请求
    protected PagingReq pagingReq = new PagingReq();

    //普通的请求
    protected T ApiClient() {
        return NetMgr.getInstance().getRetrofit(BuildConfig.BaseUrl).create(getType());
    }


    protected <T> Observable.Transformer<T, T> normalSchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> source) {
                return source.onTerminateDetach().subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    private Class<T> getType() {
        Class<T> entityClass = null;
        Type t = getClass().getGenericSuperclass();
        Type[] p = ((ParameterizedType) t).getActualTypeArguments();
        entityClass = (Class<T>) p[0];
        return entityClass;
    }
}
