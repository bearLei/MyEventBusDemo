package com.example.administrator.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

//import com.example.lib_eventbus.eventbus.EventBus;
//import com.example.lib_eventbus.eventbus.Subscribe;
//import com.example.lib_eventbus.eventbus.ThreadMode;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;

public class EventBusRegistActivity extends Activity implements View.OnClickListener {

    TextView showProcee;
    Button getDefault;
    Button regist;
    TextView showRegistProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_bus_regist);

        showProcee = findViewById(R.id.show_procee);
        getDefault = findViewById(R.id.get_default);
        regist = findViewById(R.id.regist);
        showRegistProcess = findViewById(R.id.show_regist_process);

        getDefault.setOnClickListener(this);
        regist.setOnClickListener(this);
    }


    private String getDefaultString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EventBust通过调用getDefault方法来生成1个EventBus对象，其中getDefault采用双重锁机制生成1个单例对象" +
                "该单例的创建和普通的有些区别的地方是  构造器竟然是public 不是private" +
                "之后通过建造者模式来生成1个EventBus对象，并为EventBus设置一系列的默认值");
        return builder.toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.get_default:
                showProcee.setText(getDefaultString());
                break;
            case R.id.regist:
                new TestEvent();
                break;
        }
    }

//    @OnClick({R.id.get_default, R.id.regist})
//    public void onViewClicked(View view) {
//        switch (view.getId()) {
//            case R.id.get_default:
//                showProcee.setText(getDefault());
//                break;
//            case R.id.regist:
//                new TestEvent();
//                break;
//        }
//    }

//    @Subscribe (threadMode = ThreadMode.POSTING,sticky = false,priority = 4)
//    public void testEvent(){
//
//    }

}
