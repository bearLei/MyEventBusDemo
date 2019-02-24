package com.example.custom_eventbus_lib;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyEventBus {

    private Map<Class<?>,List<MySubScribeMethod>> cacheMethod ;

    private static MyEventBus eventBus;
    private Handler handler;
    private ExecutorService executorService;

    public MyEventBus() {
        cacheMethod = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    //生成EventBus实例
    public static MyEventBus getDefault(){
        if (eventBus == null){
            synchronized (MyEventBus.class){
                if (eventBus == null){
                    eventBus = new MyEventBus();
                }
            }
        }
        return eventBus;
    }


    //注册的目的是将订阅者对象的订阅方法找出并且和订阅者对象形成映射关系缓存在内存中
    public void reigst(Object obj){

        if (obj == null) return;

        Class<?> clazz = obj.getClass();

        //从缓存中查找，命中的话就不往下走

        List<MySubScribeMethod> mySubScribeMethods = cacheMethod.get(clazz);

        if (mySubScribeMethods != null){
            return;
        }
        //通过反射查找到订阅者的全部订阅方法缓存起来
        //考虑条件：1: 涉及继承关系，自身查询完需要考虑父类的订阅方法
        //        : 2:反射效率问题

        while (clazz != null){
            findSubScibeMethod(clazz);
            //继续查找父类，剔除工程类
            clazz = clazz.getSuperclass();
            String clazzName = clazz.getName();
            if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                clazz = null;
            }
        }

    }

    /**
     * 通过反射机制将订阅者对象和订阅者方法完成映射缓存起来
     * @param clazz
     */
    private void findSubScibeMethod(Class clazz){
        if (clazz == null) return;
        Method[] methods = clazz.getDeclaredMethods();
        ArrayList<MySubScribeMethod> list = new ArrayList<>();
        if (methods == null || methods.length == 0){
            return;
        }
        //遍历方法
        for (Method mehod : methods) {
            //这里要考虑的是如何封装一个MySubScibeMethod方法
            // 1:忽略非pubilic 和static 方法
            //2：获取参数
            Class<?>[] parameterTypes = mehod.getParameterTypes();
            if (parameterTypes.length == 1){
                //2：获取注解
                MySubScribe annotation = mehod.getAnnotation(MySubScribe.class);

                if (annotation != null){
                    //获取线程模型
                    MyThreadMode myThreadMode = annotation.threadMode();
                    //封装方法
                    MySubScribeMethod subScribeMethod = new MySubScribeMethod(myThreadMode, parameterTypes[0], mehod);
                    list.add(subScribeMethod);
                    cacheMethod.put(clazz,list);
                }
            }
        }
    }


    /**
     * 解绑
     * @param obj
     */
    public void  unRegist(Object obj){
        if (obj == null){
            return;
        }
        List<MySubScribeMethod> mySubScribeMethods = cacheMethod.get(obj.getClass());
        //将内存中的数据移除
        if (mySubScribeMethods != null){
            cacheMethod.remove(obj.getClass());
        }
    }

    /**
     * post流程
     * @param obj
     */
    public void  post(Object obj){
        if (obj == null){
            return;
        }
        //遍历缓存列表
        Set<Map.Entry<Class<?>, List<MySubScribeMethod>>> entries = cacheMethod.entrySet();
        Iterator<Map.Entry<Class<?>, List<MySubScribeMethod>>> iterator = entries.iterator();
        while (iterator.hasNext()){
            Map.Entry<Class<?>, List<MySubScribeMethod>> next = iterator.next();
            List<MySubScribeMethod> scribeMethods = next.getValue();
            for (MySubScribeMethod subScribeMethod : scribeMethods) {
                Class<?> parames = subScribeMethod.getParames();
                //类信息相同，说明命中，反射调用方法
                if (parames.isAssignableFrom(obj.getClass())){
                    invokeMethod(next.getKey(),subScribeMethod);
                    continue;
                }
            }
        }
    }

    /**
     *
     * @param clazz 命中的反射类
     */
    private void invokeMethod(final Class clazz, final MySubScribeMethod subScribeMethod) {
        switch (subScribeMethod.getThreadMode()){
            //需要在主线程订阅
            case Main:
                //如果post的事件是在主线程
                if (Looper.myLooper() == Looper.getMainLooper()){
                    //直接反射
                    invoke(subScribeMethod.getMethod(),clazz,subScribeMethod.getParames());
                }else {
                    //发布者在子线程 通过handler调用
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            invoke(subScribeMethod.getMethod(),clazz,subScribeMethod.getParames());
                        }
                    });
                }
                break;
                //需要在子线程订阅
            case Background:
                //发布者在主线程
                if (Looper.myLooper() == Looper.getMainLooper()){
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                           invoke(subScribeMethod.getMethod(),clazz,subScribeMethod.getParames());
                        }
                    });
                }else {
                    //发布者在子线程
                    invoke(subScribeMethod.getMethod(),clazz,subScribeMethod.getParames());
                }
                break;
        }
    }


    private void invoke(Method method,Class clazz,Class type){
        try {
            method.invoke(clazz,type);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
}
