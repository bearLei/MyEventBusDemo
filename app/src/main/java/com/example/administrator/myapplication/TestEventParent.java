package com.example.administrator.myapplication;

import com.example.administrator.myapplication.event.Event01;
import com.example.lib_eventbus.eventbus.EventBus;
import com.example.lib_eventbus.eventbus.Subscribe;
import com.example.lib_eventbus.eventbus.ThreadMode;

public class TestEventParent {


    public TestEventParent() {
        EventBus eventBus = EventBus.builder().ignoreGeneratedIndex(true).build();
        eventBus.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN ,sticky =  false)
    public void testEventMessage(Event01 event01){

    }
}
