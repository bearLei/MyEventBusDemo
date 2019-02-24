package com.example.administrator.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.administrator.myapplication.event.Event01;
import com.example.custom_eventbus_lib.MyEventBus;
import com.example.custom_eventbus_lib.MySubScribe;
import com.example.custom_eventbus_lib.MyThreadMode;

//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;

public class MainActivity extends Activity implements View.OnClickListener {


    Button testRegist;
    Button testPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testRegist = findViewById(R.id.test_regist);
        testPost = findViewById(R.id.test_post);

        testPost.setOnClickListener(this);
        testRegist.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyEventBus.getDefault().reigst(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyEventBus.getDefault().unRegist(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.test_regist:
                startActivity(new Intent(this,EventBusRegistActivity.class));
                break;
            case R.id.test_post:
//                new TestPostEvent().post();
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        Log.d("lei","发送事件所在线程--->"+Thread.currentThread().getName());
                        MyEventBus.getDefault().post(new Event01());
                    }
                }.start();
                break;
        }
    }

    @MySubScribe (threadMode =  MyThreadMode.Main)
    public void getEvent01(){
        Log.d("lei","接收事件所在线程--->"+Thread.currentThread().getName());
    }



//    @OnClick({R.id.test_regist, R.id.test_post})
//    public void onViewClicked(View view) {
//        switch (view.getId()) {
//            case R.id.test_regist:
//                startActivity(new Intent(this,EventBusRegistActivity.class));
//                break;
//            case R.id.test_post:
//                break;
//        }
//    }
}
