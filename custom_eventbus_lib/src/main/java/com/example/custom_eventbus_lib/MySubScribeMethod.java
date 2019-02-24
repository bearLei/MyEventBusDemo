package com.example.custom_eventbus_lib;


import java.lang.reflect.Method;

/**
 * 自定义的封装订阅方法类
 */
public class MySubScribeMethod {

    private MyThreadMode threadMode;//线程模式
    private Class<?> parames;//参数列表
    private Method method;//执行方法

    public MySubScribeMethod(MyThreadMode threadMode, Class<?> parames, Method method) {
        this.threadMode = threadMode;
        this.parames = parames;
        this.method = method;
    }

    public MyThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(MyThreadMode threadMode) {
        this.threadMode = threadMode;
    }

    public Class<?> getParames() {
        return parames;
    }

    public void setParames(Class<?> parames) {
        this.parames = parames;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
