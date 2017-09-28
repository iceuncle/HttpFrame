package com.hd123.httpframe.views;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hd123.httpframe.R;
import com.hd123.httpframe.net.cases.GetCitiesCase;
import com.hd123.httpframe.net.extension.BaseSubscriber;
import com.hd123.httpframe.net.models.City;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.List;

public class MainActivity extends RxAppCompatActivity {
    private TextView getCitiesTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getCitiesTv = (TextView) findViewById(R.id.cities_tv);
    }

    public void getCities(View view) {
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

    }
}
