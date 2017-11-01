package org.tastefuljava.jsonia.handler;

import org.tastefuljava.jsonia.util.Dates;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jsonia.props.ClassDef;
import org.tastefuljava.jsonia.props.PropertyDef;

public class JSonBuilder extends AbstractJSonBuilder {
    private static final Logger LOG
            = Logger.getLogger(JSonBuilder.class.getName());

    private final List<Object> stack = new ArrayList<>();
    private Class<?> type;
    private final List<Class<?>> typeStack = new ArrayList<>();

    public JSonBuilder(Class<?> type) {
        this.type = type;
    }

    @Override
    public void startObject() {
        try {
            stack.add(0, type.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, "Error instanciating object", ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public void endObject() {
        top = stack.remove(0);
    }

    @Override
    public void startField(String name) {
        typeStack.add(0, type);
        Object object = stack.get(0);
        ClassDef cdef = ClassDef.forClass(object.getClass());
        PropertyDef prop = cdef.getProperty(name);
        type = prop == null ? Object.class : prop.getType();
    }

    @Override
    public void endField(String name) {
        type = typeStack.remove(0);
        Object object = stack.get(0);
        ClassDef cdef = ClassDef.forClass(object.getClass());
        PropertyDef prop = cdef.getProperty(name);
        if (prop != null && prop.canSet()) {
            prop.set(object, convert(top, prop.getType()));
        }
    }

    @Override
    public void startArray() {
        stack.add(0, new ArrayList<>());
    }

    @Override
    public void endArray() {
        top = stack.remove(0);
        List<?> list = (List<?>) top;
        if (type.isArray()) {
            int length = list.size();
            Class<?> elmType = type.getComponentType();
            top = Array.newInstance(elmType, length);
            for (int i = 0; i < length; ++i) {
                Object elm = convert(list.get(i), elmType);
                Array.set(top, i, elm);
            }
        } else if (type.isAssignableFrom(List.class)) {
            // nothing to do
        } else if (type.isAssignableFrom(TreeSet.class)) {
            top = new TreeSet<>(list);
        } else if (type.isAssignableFrom(HashSet.class)) {
            top = new HashSet<>(list);
        }
    }

    @Override
    public void startElement() {
        typeStack.add(0, type);
        if (type.isArray()) {
            type = type.getComponentType();
        } else if (Collection.class.isAssignableFrom(type)) {
            type = Object.class;
        }
    }

    @Override
    public void endElement() {
        type = typeStack.remove(0);
        @SuppressWarnings(value = "unchecked")
        List<Object> array = (List<Object>) stack.get(0);
        array.add(top);
    }

    @Override
    public void handleNull() {
        top = null;
    }

    @Override
    public void handleBoolean(boolean value) {
        top = value;
    }

    @Override
    public void handleNumber(Number value) {
        top = value;
    }

    @Override
    public void handleString(String value) {
        top = value;
    }

    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            return null;
        } else if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        } else if ((type == boolean.class || type == Boolean.class)
                && value.getClass() == Boolean.class) {
            return value;
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if (type == byte.class || type == Byte.class) {
                return number.byteValue();
            } else if (type == short.class || type == Short.class) {
                return number.shortValue();
            } else if (type == int.class || type == Integer.class) {
                return number.intValue();
            } else if (type == long.class || type == Long.class) {
                return number.longValue();
            } else if (type == float.class || type == Float.class) {
                return number.floatValue();
            } else if (type == double.class || type == Double.class) {
                return number.doubleValue();
            } else if (type == BigDecimal.class) {
                return BigDecimal.valueOf(number.doubleValue());
            } else {
                throw new RuntimeException(
                        "Cannot convert value of type " + type);
            }
        } else if (type == Date.class && value instanceof String) {
            return Dates.parse((String) value);
        } else if (Enum.class.isAssignableFrom(type)
                && value instanceof String) {
            @SuppressWarnings(value = "unchecked")
            Object result = Enum.valueOf(
                    (Class<? extends Enum>) type, (String) value);
            return result;
        } else {
            throw new RuntimeException("Cannot convert value of type " + type);
        }
    }
}
