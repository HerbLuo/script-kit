package cn.cloudself.script.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BeanHelper {
    private BeanHelper() {}

    private final Set<String> shouldIgnoreFields = new HashSet<String>() {{
        add("serialVersionUID");
    }};

    public static BeanHelper createDefault() {
        return new BeanHelper();
    }

    public Map<String, Object> toMapDeep(Object bean) {
        if (bean == null) {
            return null;
        }

        final Map<String, Object> map = new HashMap<>();

        final Class<?> clazz = bean.getClass();
        Class<?> classOrSuperClass = clazz;
        while (classOrSuperClass != null) {
            final Field[] declaredFields = classOrSuperClass.getDeclaredFields();
            for (Field field : declaredFields) {
                final String key = field.getName();
                if (shouldIgnoreFields.contains(key)) {
                    continue;
                }
                final Object value;
                if (canAccess(field, bean)) {
                    try {
                        value = field.get(bean);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    final String getterMethodName = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
                    Method getter;
                    try {
                        getter = clazz.getDeclaredMethod(getterMethodName);
                    } catch (NoSuchMethodException e) {
                        try {
                            getter = clazz.getMethod(getterMethodName);
                        } catch (NoSuchMethodException e2) {
                            throw new RuntimeException("无法访问私有且无getter的属性 " + key);
                        }
                    }
                    try {
                        value = getter.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                map.put(key, trans(value));
            }
            classOrSuperClass = classOrSuperClass.getSuperclass();
        }
        return map;
    }

    private Object trans(Object value) {
        if (
                value == null ||
                value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double ||
                value instanceof BigDecimal || value instanceof BigInteger ||
                value instanceof Boolean ||
                value instanceof String || // warn
                value instanceof Date || value instanceof LocalDate || value instanceof LocalTime || value instanceof LocalDateTime ||
                value instanceof Duration
        ) {
            return value;
        } else if (value instanceof Iterable) {
            return StreamSupport.stream(((Iterable<?>) value).spliterator(), false)
                    .map(this::trans)
                    .collect(Collectors.toList());
        } else if (value instanceof Object[]) {
            return Arrays.stream((Object[]) value)
                    .map(this::trans)
                    .collect(Collectors.toList());
        } else if (value instanceof Map) {
            final Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                final Object k = entry.getKey();
                final Object v = entry.getValue();
                map.put(k + "", trans(v));
            }
            return map;
        } else {
            return toMapDeep(value);
        }
    }

    private static Boolean canAccess(Field field, Object obj) {
        try {
            //noinspection ResultOfMethodCallIgnored
            field.get(obj);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
