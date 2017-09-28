package com.hd123.httpframe.net.cases;

import com.hd123.httpframe.net.cases.base.UseCase;
import com.hd123.httpframe.net.models.City;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

/**
 * 界面描述：
 * <p>
 * Created by tianyang on 2017/9/27.
 */

public class GetCitiesCase extends UseCase<GetCitiesCase.Api> {
    interface Api {
        @GET("api/china/")
        Observable<List<City>> getCitiesCase();
    }


    public Observable<List<City>> getCities() {
        return ApiClient().getCitiesCase()
                .compose(this.<List<City>>normalSchedulers());
    }

}
