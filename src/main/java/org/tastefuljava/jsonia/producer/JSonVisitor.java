package org.tastefuljava.jsonia.producer;

import org.tastefuljava.jsonia.util.Dates;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.Map;
import org.tastefuljava.jsonia.JSonHandler;
import org.tastefuljava.jsonia.props.ClassDef;
import org.tastefuljava.jsonia.props.PropertyDef;

public class JSonVisitor {
    private final JSonHandler handler;

    public JSonVisitor(JSonHandler handler) {
        this.handler = handler;
    }

    public void visit(Object object) {
        if (object == null) {
            handler.handleNull();
        } else if (object instanceof Boolean) {
            handler.handleBoolean((Boolean)object);
        } else if (object instanceof Number) {
            handler.handleNumber(((Number)object).doubleValue());
        } else if (object instanceof String) {
            handler.handleString((String)object);
        } else if (object instanceof Date) {
            handler.handleString(Dates.format((Date)object));
        } else {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                visitArray(object);
            } else if (Iterable.class.isAssignableFrom(clazz)) {
                visitCollection((Iterable<?>)object);
            } else if (Map.class.isAssignableFrom(clazz)) {
                visitMap((Map<?,?>)object);
            } else {
                ClassDef cdef = ClassDef.forClass(clazz);
                visitObject(cdef, object);
            }
        }
    }

    private void visitMap(Map<?, ?> map) {
        handler.startObject();
        for (Map.Entry<?,?> e: map.entrySet()) {
            String name = e.getKey().toString();
            handler.startField(name);
            visit(e.getValue());
            handler.endField(name);
        }
        handler.endObject();
    }

    private void visitObject(ClassDef cdef, Object obj) {
        handler.startObject();
        for (PropertyDef prop: cdef.getProperties()) {
            if (prop.canGet()) {
                Object value = prop.get(obj);
                if (value != null) {
                    handler.startField(prop.getName());
                    visit(value);
                    handler.endField(prop.getName());
                }
            }
        }
        handler.endObject();
    }

    private void visitCollection(Iterable<?> col) {
        handler.startArray();
        for (Object elm: col) {
            handler.startElement();
            visit(elm);
            handler.endElement();
        }
        handler.endArray();
    }

    private void visitArray(Object array) {
        handler.startArray();
        int length = Array.getLength(array);
        for (int i = 0; i < length; ++i) {
            handler.startElement();
            visit(Array.get(array, i));
            handler.endElement();
        }
        handler.endArray();
    }
}
