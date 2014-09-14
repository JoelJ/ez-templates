package com.joelj.jenkins.eztemplates.utils;


import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Singleton
public class ReflectionUtils {
    public static <T> T getFieldValue(Class c, Object instance, String name) {
        try {
            Field declaredField = c.getDeclaredField(name);
            declaredField.setAccessible(true);
            Object result = declaredField.get(instance);
            //noinspection unchecked
            return (T) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFieldValue(Class c, Object instance, String name, Object value) {
        try {
            Field declaredField = c.getDeclaredField(name);
            declaredField.setAccessible(true);
            declaredField.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, R> R invokeMethod(Class<? extends T> type, T object, String methodName, MethodParameter<?>... parameters) {
        Object[] parameterValues = new Object[parameters == null ? 0 : parameters.length];
        Class[] parameterTypes = new Class[parameterValues.length];
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                MethodParameter parameter = parameters[i];
                parameterValues[i] = parameter.getValue();
                parameterTypes[i] = parameter.getType();
            }
        }

        try {
            Method method = type.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            Object invoke = method.invoke(object, parameterValues);

            //noinspection unchecked
            return (R) invoke;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static class MethodParameter<T> {
        private final Class<T> type;
        private final T value;

        public static <T> MethodParameter<T> get(Class<T> type, T value) {
            if (type == null) {
                throw new NullPointerException("type cannot be null");
            }
            return new MethodParameter<T>(type, value);
        }

        private MethodParameter(Class<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        public Class<T> getType() {
            return type;
        }

        public T getValue() {
            return value;
        }
    }
}
