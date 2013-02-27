package com.joelj.jenkins.eztemplates.utils;

import java.lang.reflect.Field;

/**
 * User: Joel Johnson
 * Date: 2/26/13
 * Time: 5:42 PM
 */
public class ReflectionUtils {
	public static <T> T getFieldValue(Class c, Object instance, String name) {
		try {
			Field declaredField = c.getDeclaredField(name);
			declaredField.setAccessible(true);
			Object result = declaredField.get(instance);
			//noinspection unchecked
			return (T)result;
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
}
