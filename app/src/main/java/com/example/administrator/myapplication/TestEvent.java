package com.example.administrator.myapplication;

import com.example.administrator.myapplication.event.Event01;
import com.example.lib_eventbus.eventbus.EventBus;
import com.example.lib_eventbus.eventbus.EventBusBuilder;
import com.example.lib_eventbus.eventbus.Subscribe;
import com.example.lib_eventbus.eventbus.ThreadMode;

public class TestEvent extends TestEventParent{





    public void testEventMessage(){
    }

    @Subscribe(threadMode = ThreadMode.MAIN ,sticky =  false)
    public void testEventMessage(Event01 event01){

    }
}
