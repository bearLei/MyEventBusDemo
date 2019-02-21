package com.example.administrator.myapplication;

import com.example.administrator.myapplication.event.Event01;
import com.example.lib_eventbus.eventbus.EventBus;

public class TestPostEvent {


    public TestPostEvent() {
        EventBus.getDefault().register(this);
    }


    public void post(){
        EventBus.getDefault().post(new Event01());
    }

}
