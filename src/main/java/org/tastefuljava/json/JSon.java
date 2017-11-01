package org.tastefuljava.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import org.tastefuljava.util.InvocationLogger;

public class JSon {
    public static <T> T read(File file, String encoding, Class<T> clazz)
            throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return read(in, encoding, clazz);
        }
    }

    public static <T> T read(InputStream in, String encoding, Class<T> clazz)
            throws IOException {
        try (Reader reader = new InputStreamReader(in, encoding)) {
            return read(reader, clazz);
        }
    }

    public static <T> T read(Reader in, Class<T> clazz)
            throws IOException {
        return clazz.cast(readObject(in, new JSonBuilder(clazz)));
    }

    public static <T> T read(String json, Class<T> clazz) throws IOException {
        return read(new StringReader(json), clazz);
    }

    public static Object read(File file, String encoding)
            throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return read(in, encoding);
        }
    }

    public static Object read(InputStream in, String encoding)
            throws IOException {
        try (Reader reader = new InputStreamReader(in, encoding)) {
            return read(reader);
        }
    }

    public static Object read(Reader in) throws IOException {
        return readObject(in, new JSonGenericBuilder());
    }

    public static Object read(String json) throws IOException {
        return read(new StringReader(json));
    }

    private static Object readObject(Reader in, AbstractJSonBuilder handler)
            throws IOException {
        parse(in, handler);
        return handler.getTop();
    }

    public static void parse(Reader in, JSonHandler handler)
            throws IOException {
        JSonHandler jsonHandler = InvocationLogger.wrap(
                Level.FINE, handler, JSonHandler.class);
        JSonParser.parse(in, jsonHandler);
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
        new JSonVisitor(handler).visit(object);
    }
}
