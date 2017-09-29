想必Retrifit+Rxjava的使用，如今已经非常的普及了吧。在此介绍一种比较优雅的有关Retrifit+Rxjava封装的方法。

原本的步骤应该是这样，首先要创建OKHttpClient ，在其中添加一些拦截和超时处理，然后创建Retrofit对象并注入OKHttpClient对象，再获取接口实例Observable对象，然后绑定生命周期（防止内存泄漏）并订阅观察者Subscriber处理返回信息。

那现在应该如何封装，才能比较优雅，并且能够尽量的解耦呢？

* **1、提出Retrofits实现类，提供设置超时时间、添加拦截等处理的接口**

首先应该将Retrofit这一块提出来，而创建Retrofit需要注入OKHttpClient，其中有很多与业务相关的处理，比如需要设置超时时间，拦截头部添加Header等等。那么这一块就可以写一个接口回调，在外部实现后注入。看一下这一块的代码吧

```java
public class NetMgr {
    private final long connectTimeoutMills = 10 * 1000L;
    private final long readTimeoutMills = 10 * 1000L;
    private NetProvider sProvider = null;
    private static NetMgr instance;
    private Map<String, NetProvider> providerMap = new HashMap<>();
    private Map<String, Retrofit> retrofitMap = new HashMap<>();
    private Map<String, OkHttpClient> clientMap = new HashMap<>();


    public static NetMgr getInstance() {
        if (instance == null) {
            synchronized (NetMgr.class) {
                if (instance == null) {
                    instance = new NetMgr();
                }
            }
        }
        return instance;
    }


    public <S> S get(String baseUrl, Class<S> service) {
        return getInstance().getRetrofit(baseUrl).create(service);
    }

    public void registerProvider(NetProvider provider) {
        this.sProvider = provider;
    }

    public void registerProvider(String baseUrl, NetProvider provider) {
        getInstance().providerMap.put(baseUrl, provider);
    }

    public NetProvider getCommonProvider() {
        return sProvider;
    }

    public void clearCache() {
        getInstance().retrofitMap.clear();
        getInstance().clientMap.clear();
    }

    public Retrofit getRetrofit(String baseUrl) {
        return getRetrofit(baseUrl, null);
    }

    public Retrofit getRetrofit(String baseUrl, NetProvider provider) {
        if (empty(baseUrl)) {
            throw new IllegalStateException("baseUrl can not be null");
        }
        if (retrofitMap.get(baseUrl) != null) {
            return retrofitMap.get(baseUrl);
        }

        if (provider == null) {
            provider = providerMap.get(baseUrl);
            if (provider == null) {
                provider = sProvider;
            }
        }
        checkProvider(provider);

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getClient(baseUrl, provider))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson));

        Retrofit retrofit = builder.build();
        retrofitMap.put(baseUrl, retrofit);
        providerMap.put(baseUrl, provider);

        return retrofit;
    }

    private boolean empty(String baseUrl) {
        return baseUrl == null || baseUrl.isEmpty();
    }

    private OkHttpClient getClient(String baseUrl, NetProvider provider) {
        if (empty(baseUrl)) {
            throw new IllegalStateException("baseUrl can not be null");
        }
        if (clientMap.get(baseUrl) != null) {
            return clientMap.get(baseUrl);
        }

        checkProvider(provider);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(provider.configConnectTimeoutSecs() != 0
                ? provider.configConnectTimeoutSecs()
                : connectTimeoutMills, TimeUnit.SECONDS);
        builder.readTimeout(provider.configReadTimeoutSecs() != 0
                ? provider.configReadTimeoutSecs() : readTimeoutMills, TimeUnit.SECONDS);

        builder.writeTimeout(provider.configWriteTimeoutSecs() != 0
                ? provider.configReadTimeoutSecs() : readTimeoutMills, TimeUnit.SECONDS);
        CookieJar cookieJar = provider.configCookie();
        if (cookieJar != null) {
            builder.cookieJar(cookieJar);
        }
        provider.configHttps(builder);

        RequestHandler handler = provider.configHandler();
        if (handler != null) {
            builder.addInterceptor(new NetInterceptor(handler));
        }

        Interceptor[] interceptors = provider.configInterceptors();
        if (!empty(interceptors)) {
            for (Interceptor interceptor : interceptors) {
                builder.addInterceptor(interceptor);
            }
        }

        if (provider.configLogEnable()) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        OkHttpClient client = builder.build();
        clientMap.put(baseUrl, client);
        providerMap.put(baseUrl, provider);

        return client;
    }

    private boolean empty(Interceptor[] interceptors) {
        return interceptors == null || interceptors.length == 0;
    }

    private void checkProvider(NetProvider provider) {
        if (provider == null) {
            throw new IllegalStateException("must register provider first");
        }
    }

    public Map<String, Retrofit> getRetrofitMap() {
        return retrofitMap;
    }

    public Map<String, OkHttpClient> getClientMap() {
        return clientMap;
    }

}
```

* **2、实现NetProvider接口并注入**

NetMgr就是一个Retrofit的实现类，然后NetProvider是一个接口，需要在外部去实现，然后注入。再看一下NetProvider的实现类BaseNetProvider

```java
public class BaseNetProvider implements NetProvider {

    private static final long CONNECT_TIME_OUT = 30;
    private static final long READ_TIME_OUT = 180;
    private static final long WRITE_TIME_OUT = 30;


    @Override
    public Interceptor[] configInterceptors() {
        return null;
    }

    @Override
    public void configHttps(OkHttpClient.Builder builder) {

    }

    @Override
    public CookieJar configCookie() {
        return null;
    }

    @Override
    public RequestHandler configHandler() {

        return new HeaderHandler();
    }

    @Override
    public long configConnectTimeoutSecs() {
        return CONNECT_TIME_OUT;
    }

    @Override
    public long configReadTimeoutSecs() {
        return READ_TIME_OUT;
    }

    @Override
    public long configWriteTimeoutSecs() {
        return WRITE_TIME_OUT;
    }

    @Override
    public boolean configLogEnable() {
        return BuildConfig.DEBUG;
    }


    private class HeaderHandler implements RequestHandler {

        @Override
        public Request onBeforeRequest(Request request, Interceptor.Chain chain) {
            return chain.request().newBuilder()
                    .addHeader("X-Auth-Token", Constant.accessToken)
                    .addHeader("Authorization", "")
                    .build();
        }

        @Override
        public Response onAfterRequest(Response response, Interceptor.Chain chain)
                throws IOException {
            ApiException e = null;
            if (401 == response.code()) {
                throw new ApiException("登录已过期,请重新登录!");
            } else if (403 == response.code()) {
                throw new ApiException("禁止访问!");
            } else if (404 == response.code()) {
                throw new ApiException("链接错误");
            } else if (503 == response.code()) {
                throw new ApiException("服务器升级中!");
            } else if (500 == response.code()) {
                throw new ApiException("服务器内部错误!");
            }
            return response;
        }
    }

```

在BaseNetProvider中实现了连接、读、写超时的时间处理，与请求和返回数据的请求头部处理。然后需要在Application中去注入BaseNetProvider

```
NetMgr.getInstance().registerProvider(new BaseNetProvider());
```

* **3、Observable实现**

首先实现一个UseCase的基类，处理公共的使用方法。通过调用NetMgr.getInstance().getRetrofit(BuildConfig.BaseUrl).create(getType())来获取ApiService的实例，然后提供了指定线程的基类方法。至于PagingReq是一个分页模型，方便分页接口的使用。

```java
public abstract class UseCase<T> {
    //用于分页请求 
    protected PagingReq pagingReq = new PagingReq();

    protected T ApiClient() {
        return NetMgr.getInstance().getRetrofit(BuildConfig.BaseUrl).create(getType());
    }
  
    //指定观察者与被观察者线程
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
```

此处实现一个简单的获取城市信息的接口。 首先定义接口ApiService，然后实现获取Observable的方法

```java
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
```

* 使用时调用

```java
 new GetCitiesCase().getCities()
                .compose(this.<List<City>>bindToLifecycle())
                .subscribe(new BaseSubscriber<List<City>>() {
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(List<City> o) {
                        getCitiesTv.setText("");
                        if (o != null && o.size() != 0) {
                            for (City city : o) {
                                getCitiesTv.setText(getCitiesTv.getText().toString() + city.id.intValue() + "  " + city.name + "\n");
                            }
                        }
                    }
                });
```

调用就很简单了，只需绑定生命周期（防止内存泄漏），然后订阅Subscriber，处理成功或失败后的返回。



附上[github](https://github.com/iceuncle/HttpFrame)链接，多多Star噢(～￣▽￣)～
