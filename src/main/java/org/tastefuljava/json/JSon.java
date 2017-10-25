package org.tastefuljava.json;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import org.tastefuljava.props.ClassDef;
import org.tastefuljava.props.PropertyDef;
import org.tastefuljava.util.InvocationLogger;

public class JSon {
    public static <T> T read(String json, Class<T> clazz) throws IOException {
        return read(new StringReader(json), clazz);
    }

    public static <T> T read(Reader in, Class<T> clazz)
            throws IOException {
        return clazz.cast(readObject(in, new JSonBuilder(clazz)));
    }

    public static Object read(String json) throws IOException {
        return read(new StringReader(json));
    }

    public static Object read(Reader in) throws IOException {
        return readObject(in, new JSonGenericBuilder());
    }

    private static Object readObject(Reader in, AbstractJSonBuilder handler)
            throws IOException {
        JSonHandler jsonHandler = InvocationLogger.wrap(
                Level.FINE, handler, JSonHandler.class);
        JSonParser.parse(in, jsonHandler);
        return handler.getTop();
    }

    public static void write(Object object, PrintWriter out, boolean format) {
        try (JSonFormatter fmt = new JSonFormatter(out, format)) {
            visit(object, fmt);
        }
    }

    public static void write(Object object, Writer writer, boolean format) {
        if (writer instanceof PrintWriter) {
            write(object, (PrintWriter)writer, format);
        } else {
            try (PrintWriter out = new PrintWriter(writer)) {
                write(object, out, format);
            }
        }
    }

    public static String stringify(Object object, boolean format) {
        StringWriter writer = new StringWriter();
        write(object, writer, format);
        return writer.toString();
    }

    public static void visit(Object object, JSonHandler handler) {
        if (object == null) {
            handler.handleNull();
        } else if (object instanceof Boolean) {
            handler.handleBoolean((Boolean)object);
        } else if (object instanceof Number) {
            handler.handleNumber(((Number)object).doubleValue());
        } else if (object instanceof String) {
            handler.handleString((String)object);
        } else if (object instanceof Date) {
            handler.handleString(JSonDates.format((Date)object));
        } else {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                visitArray(object, handler);
            } else if (Iterable.class.isAssignableFrom(clazz)) {
                visitCollection((Iterable<?>)object, handler);
            } else if (Map.class.isAssignableFrom(clazz)) {
                visitMap((Map<?,?>)object, handler);
            } else {
                ClassDef cdef = ClassDef.forClass(clazz);
                visitObject(cdef, object, handler);
            }
        }
    }

    private static void visitMap(Map<?, ?> map, JSonHandler handler) {
        handler.startObject();
        for (Map.Entry<?,?> e: map.entrySet()) {
            String name = e.getKey().toString();
            handler.startField(name);
            visit(e.getValue(), handler);
            handler.endField(name);
        }
        handler.endObject();
    }

    private static void visitObject(ClassDef cdef, Object obj,
            JSonHandler handler) {
        handler.startObject();
        for (PropertyDef prop: cdef.getProperties()) {
            Object value = prop.get(obj);
            if (value != null) {
                handler.startField(prop.getName());
                visit(value, handler);
                handler.endField(prop.getName());
            }
        }
        handler.endObject();
    }

    private static void visitCollection(Iterable<?> col, JSonHandler handler) {
        handler.startArray();
        for (Object elm: col) {
            handler.startElement();
            visit(elm, handler);
            handler.endElement();
        }
        handler.endArray();
    }

    private static void visitArray(Object array, JSonHandler handler) {
        handler.startArray();
        int length = Array.getLength(array);
        for (int i = 0; i < length; ++i) {
            handler.startElement();
            visit(Array.get(array, i), handler);
            handler.endElement();
        }
        handler.endArray();
    }
}
