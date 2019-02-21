/*
 * Copyright (C) 2012-2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lib_eventbus.eventbus;



import com.example.lib_eventbus.eventbus.meta.SubscriberInfo;
import com.example.lib_eventbus.eventbus.meta.SubscriberInfoIndex;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SubscriberMethodFinder {
    /*
     * In newer class files, compilers may add methods. Those are called bridge or synthetic methods.
     * EventBus must ignore both. There modifiers are not public but defined in the Java class file format:
     * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.6-200-A.1
     */
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;

    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;
    //订阅方法的缓存，key为订阅者对象，value订阅者对象的所有订阅方法的集合
    private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    private List<SubscriberInfoIndex> subscriberInfoIndexes;
    private final boolean strictMethodVerification;
    private final boolean ignoreGeneratedIndex;

    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];

    SubscriberMethodFinder(List<SubscriberInfoIndex> subscriberInfoIndexes, boolean strictMethodVerification,
                           boolean ignoreGeneratedIndex) {
        this.subscriberInfoIndexes = subscriberInfoIndexes;
        this.strictMethodVerification = strictMethodVerification;
        this.ignoreGeneratedIndex = ignoreGeneratedIndex;
    }

    /**
     * 通过订阅者对象 找到订阅者方法集合
     * @param subscriberClass
     * @return 订阅者所有的订阅方法的集合
     * 1：通过订阅者对象在 METHOD_CACHE缓存中 中查找该订阅者是否已经存在有订阅者方法集合
     * 2：如果存在则直接返回
     * 3：未查询  继续
     * 4:通过ignoreGeneratedIndex字段判断是否采用反射机制获取订阅者方法集合
     * 4.1：反射机制获取
     * 4.2：index方式获取
     * 5:如果订阅者对象中未查询到订阅者方法-抛出异常，也就是我们平常在使用EventBus的时候，注册了EventBus，但是类中未有订阅方法时候跑出去的异常
     * 6:订阅者对象为key，该订阅者对象所有的订阅方法为value放入缓冲池中
     */
    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);//注解1
        if (subscriberMethods != null) {
            return subscriberMethods;
        }

        if (ignoreGeneratedIndex) {
            //反射机制获取订阅者方法
            subscriberMethods = findUsingReflection(subscriberClass);//注解2
        } else {
            //index形式获取订阅者方法
            subscriberMethods = findUsingInfo(subscriberClass);//注解3
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);//注解4
            return subscriberMethods;
        }
    }

    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        FindState findState = prepareFindState();//准备FindState
        findState.initForSubscriber(subscriberClass);//关联订阅者对象
        while (findState.clazz != null) {
            //通过index获取订阅者信息
            findState.subscriberInfo = getSubscriberInfo(findState);//获取订阅者信息
            if (findState.subscriberInfo != null) {
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
                findUsingReflectionInSingleClass(findState);
            }
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }

    /**
     * 其实该方法主要的目的是为了返回1个FindState对象
     * 采用对象池的方法复用对象，避免了对象的重复创建
     * @return
     */
    private FindState prepareFindState() {
        //FIND_STATE_POOL 是1个FindState类型的数组，默认有4个对象
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState state = FIND_STATE_POOL[i];
                //找到不为null的对象，清空标志返回对象，
                if (state != null) {
                    FIND_STATE_POOL[i] = null;
                    return state;
                }
            }
        }
        return new FindState();
    }

    private SubscriberInfo getSubscriberInfo(FindState findState) {
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superclassInfo.getSubscriberClass()) {
                return superclassInfo;
            }
        }
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        //初始化FindState对象
        FindState findState = prepareFindState();
        //关联订阅者对象
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findUsingReflectionInSingleClass(findState);
            //
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
          //通过反射获取订阅者对象的全部方法
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        //循环遍历方法
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            //忽略不是public 和static 的方法
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
                //获取订阅方法的所有参数
                Class<?>[] parameterTypes = method.getParameterTypes();
                //严格规定了订阅方法只能有1个参数，超过了1个参数的忽略
                if (parameterTypes.length == 1) {
                    //获取订阅方法的Subscibe注解
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                        //获取第一个参数
                        Class<?> eventType = parameterTypes[0];
                        //检查eventType
                        if (findState.checkAdd(method, eventType)) {
                            //确定可以订阅后，获取订阅方法的线程模式
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            //添加订阅的方法
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    //抛异常
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                //抛异常
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }

    static void clearCaches() {
        METHOD_CACHE.clear();
    }

    /**
     * SubscriberMethodFinder的1个静态内部类
     * 封装了订阅者和所有订阅方法的集合
     * 主要功能就两个checkAdd与moveToSuperclsss
     */
    static class FindState {
        //保存了全部的订阅方法
        final List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        //事件的类型作为key,订阅方法为value
        final Map<Class, Object> anyMethodByEventType = new HashMap<>();
        //订阅方法为key，订阅对象为value
        final Map<String, Class> subscriberClassByMethodKey = new HashMap<>();

        final StringBuilder methodKeyBuilder = new StringBuilder(128);

        Class<?> subscriberClass;
        Class<?> clazz;
        boolean skipSuperClasses;
        SubscriberInfo subscriberInfo;

        //订阅者对象和findState关联，默认查找父类方法
        void initForSubscriber(Class<?> subscriberClass) {
            this.subscriberClass = clazz = subscriberClass;
            skipSuperClasses = false;
            subscriberInfo = null;
        }

        void recycle() {
            subscriberMethods.clear();
            anyMethodByEventType.clear();
            subscriberClassByMethodKey.clear();
            methodKeyBuilder.setLength(0);
            subscriberClass = null;
            clazz = null;
            skipSuperClasses = false;
            subscriberInfo = null;
        }


        /**
         * EventBus默认 会获取父类的订阅方法
         * @param method 订阅方法
         * @param eventType 事件类型，也就是订阅方法的参数
         * @return 是否可以订阅
         *
         * 该方法是传入订阅方法和订阅方法的参数，检查是否可以完成订阅；
         */
        boolean checkAdd(Method method, Class<?> eventType) {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a subscriber doesn't have methods listening to the same event type.
            //以订阅方法的参数为key，订阅方法为value放入anyMethodByEventType中，并且返回value（事件方法）
            Object existing = anyMethodByEventType.put(eventType, method);
            //existing为null，就很显然之前的集合中不存在该订阅方法，当然可以订阅，直接返回T
            if (existing == null) {
                return true;
            } else {
                // //existing不为null,有可能是父类中存在相同的订阅方法，也可能是订阅着对象中重复订阅了方法，重名检查
                //因此，需要根据方法签名来判断
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        // Paranoia check
                        throw new IllegalStateException();
                    }
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType.put(eventType, this);
                }
                return checkAddWithMethodSignature(method, eventType);
            }
        }

        private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());

            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();
            Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
            //如果methodClassOld为空，或者methodClassOld是methodClass父类的话，返回T
            //否则表明在同个订阅方法中找到了重名方法，
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                // Only add if not already found in a sub class
                return true;
            } else {
                // Revert the put, old class is further down the class hierarchy
                subscriberClassByMethodKey.put(methodKey, methodClassOld);
                return false;
            }
        }

        void moveToSuperclass() {
            if (skipSuperClasses) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String clazzName = clazz.getName();
                /** Skip system classes, this just degrades performance. */
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
    }

}
