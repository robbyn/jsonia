package org.tastefuljava.jsonia.producer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.tastefuljava.jsonia.JSonHandler;

public class JSonParser {
    private static enum Symbol {
        EOF, NULL, FALSE, TRUE, LBRACE, RBRACE, LBRACKET, RBRACKET,
        COMMA, COLON, NUMBER, STRING, ID
    };

    @SuppressWarnings("serial")
    private static final Map<String,Symbol> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("null", Symbol.NULL);
        KEYWORDS.put("undefined", Symbol.NULL);
        KEYWORDS.put("false", Symbol.FALSE);
        KEYWORDS.put("true", Symbol.TRUE);
    }

    private static final BigInteger MIN_LONG
            = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG
            = BigInteger.valueOf(Long.MAX_VALUE);

    private final Reader in;
    private final JSonHandler handler;
    private int c = ' ';
    private Symbol sy;
    private String string;
    private Number number;

    private JSonParser(Reader in, JSonHandler handler) {
        this.in = in;
        this.handler = handler;
    }

    public static void parse(InputStream in, String encoding,
            JSonHandler handler) throws IOException {
        try (Reader reader = new InputStreamReader(in, encoding)) {
            parse(reader, handler);
        }
    }

    public static void parse(Reader in, JSonHandler handler)
            throws IOException {
        new JSonParser(in, handler).parse();
    }

    public static void parse(String s, JSonHandler handler) throws IOException {
        parse(new StringReader(s), handler);
    }

    public void parse() throws IOException {
        nextsy();
        value();
    }

    private void value() throws IOException {
        switch(sy) {
            case EOF:
                throw new IOException("Unexpected end of file");
            case NULL:
                handler.handleNull();
                break;
            case FALSE:
                handler.handleBoolean(false);
                break;
            case TRUE:
                handler.handleBoolean(true);
                break;
            case STRING:
                handler.handleString(string);
                break;
            case NUMBER:
                handler.handleNumber(number);
                break;
            case LBRACKET:
                array();
                break;
            case LBRACE:
                object();
                break;
        }
        nextsy();
    }

    private int nextc() throws IOException {
        return c = in.read();
    }

    private Symbol nextsy() throws IOException {
        while (Character.isWhitespace(c)) {
            nextc();
        }
        switch (c) {
            case -1:
                sy = Symbol.EOF;
                break;
            case '{':
                nextc();
                sy = Symbol.LBRACE;
                break;
            case '}':
                nextc();
                sy = Symbol.RBRACE;
                break;
            case '[':
                nextc();
                sy = Symbol.LBRACKET;
                break;
            case ']':
                nextc();
                sy = Symbol.RBRACKET;
                break;
            case ',':
                nextc();
                sy = Symbol.COMMA;
                break;
            case ':':
                nextc();
                sy = Symbol.COLON;
                break;
            case '"':
                string();
                break;
            case '-':
                nextc();
                if (!Character.isDigit(c)) {
                    throw new IOException("Invalid number");
                }
                number(true);
                break;
            default:
                if (Character.isDigit(c)) {
                    number(false);
                } else if (Character.isJavaIdentifierStart(c)) {
                    keyword();
                } else {
                    throw new IOException("Invalid character " + (char)c);
                }
                break;
        }
        return sy;
    }

    private void string() throws IOException {
        StringBuilder buf = new StringBuilder();
        nextc();
        while (c != '"') {
            if (c < 0) {
                throw new IOException("End of file in string");
            }
            if (c == '\\') {
                nextc();
                switch (c) {
                    case -1:
                        throw new IOException("End of file in string");
                    case '\\':
                    case '/':
                    case '"':
                        buf.append((char)c);
                        nextc();
                        break;
                    case 'b':
                        buf.append('\b');
                        nextc();
                        break;
                    case 'f':
                        buf.append('\f');
                        nextc();
                        break;
                    case 'n':
                        buf.append('\n');
                        nextc();
                        break;
                    case 'r':
                        buf.append('\r');
                        nextc();
                        break;
                    case 't':
                        buf.append('\t');
                        nextc();
                        break;
                    case 'u':
                        nextc();
                        int val = hexDigit();
                        val = 16*val + hexDigit();
                        val = 16*val + hexDigit();
                        val = 16*val + hexDigit();
                        buf.append((char)val);
                        break;
                    default:
                        throw new IOException("Invalid escape sequence");
                }
            } else if (c >= 32) {
                buf.append((char)c);
                nextc();
            } else {
                throw new IOException("Invalid character in string");
            }
        }
        nextc();
        sy = Symbol.STRING;
        string = buf.toString();
    }

    private int hexDigit() throws IOException {
        int result;
        if (c >= '0' && c <= '9') {
            result = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            result = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            result = c - 'A' + 10;
        } else {
            throw new IOException("Hex digit expected");
        }
        nextc();
        return result;
    }

    private void number(boolean neg) throws IOException {
        StringBuilder buf = new StringBuilder();
        if (neg) {
            buf.append('-');
        }
        if (c == '0') {
            buf.append('0');
            nextc();
        } else {
            do {
                buf.append((char)c);
            } while (Character.isDigit(nextc()));
        }
        boolean hasFrac = c == '.';
        if (hasFrac) {
            buf.append('.');
            nextc();
            while (Character.isDigit(c)) {
                buf.append((char)c);
                nextc();
            }
        }
        boolean hasExp = c == 'E' || c == 'e';
        if (hasExp) {
            buf.append('E');
            nextc();
            if (c == '+') {
                nextc();
            } else if (c == '-') {
                buf.append('-');
                nextc();
            }
            while (Character.isDigit(c)) {
                buf.append((char)c);
                nextc();
            }
        }
        sy = Symbol.NUMBER;
        String s = buf.toString();
        if (hasExp) {
            number = Double.valueOf(s);
        } else if (hasFrac) {
            number = new BigDecimal(s);
        } else {
            BigInteger bi = new BigInteger(s);
            if (bi.compareTo(MIN_LONG) < 0 || bi.compareTo(MAX_LONG) > 0) {
                number = bi;
            } else {
                long l = bi.longValue();
                if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                    number = l;
                } else {
                    number = (int) l;
                }
            }
        }
    }

    private void keyword() throws IOException {
        StringBuilder buf = new StringBuilder();
        do {
            buf.append((char)c);
        } while (Character.isJavaIdentifierPart(nextc()));
        String id = buf.toString();
        sy = KEYWORDS.get(id);
        if (sy == null) {
            throw new IOException("Invalid keyword " + id);
        }
    }

    private void array() throws IOException {
        handler.startArray();
        nextsy();
        if (sy != Symbol.RBRACKET) {
            while (true) {
                handler.startElement();
                value();
                handler.endElement();
                if (sy != Symbol.COMMA) {
                    break;
                }
                nextsy();
            }
            if (sy != Symbol.RBRACKET) {
                throw new IOException("']' expected");
            }
        }
        handler.endArray();
    }

    private void object() throws IOException {
        handler.startObject();
        nextsy();
        if (sy != Symbol.RBRACE) {
            while (true) {
                if (sy != Symbol.STRING) {
                    throw new IOException("String expected");
                }
                String field = string;
                nextsy();
                if (sy != Symbol.COLON) {
                    throw new IOException("':' expected");
                }
                nextsy();
                handler.startField(field);
                value();
                handler.endField(field);
                if (sy != Symbol.COMMA) {
                    break;
                }
                nextsy();
            }
            if (sy != Symbol.RBRACE) {
                throw new IOException("'}' expected");
            }
        }
        handler.endObject();
    }
}
