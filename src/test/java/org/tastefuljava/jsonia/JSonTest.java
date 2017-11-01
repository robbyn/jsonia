package org.tastefuljava.jsonia;

import org.tastefuljava.jsonia.JSonHandler;
import org.tastefuljava.jsonia.JSon;
import org.tastefuljava.jsonia.handler.JSonFormatter;
import org.tastefuljava.jsonia.producer.JSonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tastefuljava.jsonia.util.InvocationLogger;

public class JSonTest {
    private static final Logger LOG
            = Logger.getLogger(JSonTest.class.getName());

    public JSonTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testUnformatted() throws Exception {
        TestObject obj1 = new TestObject(
                    BigDecimal.valueOf(123, 2), new Date(), "Hello world!!!",
                    new int[] {1,2,3});
        String json1 = JSon.stringify(obj1, false);
        LOG.log(Level.INFO, "JSon: {0}", json1);
        TestObject obj2 = JSon.read(json1, TestObject.class);
        assertEquals(obj1, obj2);
        String json2 = JSon.stringify(obj2, false);
        assertEquals(json1, json2);
    }

    @Test
    public void testFormatted() {
        try {
            TestObject obj1 = new TestObject(
                    BigDecimal.valueOf(123, 2), new Date(), "Hello world!!!",
                    new int[] {1,2,3});
            String s = JSon.stringify(obj1, false);
            LOG.log(Level.INFO, "Formatted: {0}", reformat(s, true));
            String json1 = JSon.stringify(obj1, true);
            LOG.log(Level.INFO, "JSon: {0}", json1);
            TestObject obj2 = JSon.read(json1, TestObject.class);
            assertEquals(obj1, obj2);
            String json2 = JSon.stringify(obj2, true);
            assertEquals(json1, json2);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void testGeneric() {
        try {
            TestObject obj1 = new TestObject(
                    BigDecimal.valueOf(123, 2), new Date(), "Hello world!!!",
                    new int[] {1,2,3});
            String s = JSon.stringify(obj1, true);
            LOG.log(Level.INFO, "JSon: {0}", s);
            Object obj = JSon.read(s);
            assertTrue(obj instanceof Map);
            String json1 = JSon.stringify(obj, true);
            LOG.log(Level.INFO, "JSon: {0}", json1);
            TestObject obj2 = JSon.read(json1, TestObject.class);
            assertEquals(obj1, obj2);
            String json2 = JSon.stringify(obj2, true);
            assertEquals(s, json2);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void testNumbers() {
        try {
            String s = "-12345678901";
            Number n = JSon.read(s, Number.class);
            assertTrue(n instanceof Long);
            assertEquals(Long.parseLong(s), n.longValue());
            s = "-1234567890";
            n = JSon.read(s, Number.class);
            assertTrue(n instanceof Integer);
            assertEquals(Integer.parseInt(s), n.intValue());
            n = JSon.read("-1234.5678", Number.class);
            assertTrue(n instanceof BigDecimal);
            assertEquals(new BigDecimal("-1234.5678"),n);
            n = JSon.read("-1.2345678e+7", Number.class);
            assertTrue(n instanceof Double);
            assertEquals(-12345678L, n.longValue());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }
    }

    @Test
    public void testLogger() {
        LOG.info("begin testLogger");
        TestObject obj1 = new TestObject(
                BigDecimal.valueOf(123, 2), new Date(), "Hello world!!!",
                new int[] {1,2,3});
        JSon.visit(obj1,
                InvocationLogger.create(Level.INFO, JSonHandler.class));
        LOG.info("end testLogger");
    }

    private static String reformat(String json, boolean indent) {
        try {
            StringWriter sw = new StringWriter();
            JSonParser.parse(json, new JSonFormatter(sw, indent));
            return sw.toString();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
            return null;
        }
    }

    public static class TestObject {
        private final BigDecimal number;
        private final Date date;
        private final String string;
        private final int[] array;
        private final List<Integer> list;

        public TestObject() {
            this(null, null, null, null);
        }

        public TestObject(BigDecimal number, Date date, String string,
                int[] array) {
            this.number = number;
            this.date = date;
            this.string = string;
            this.array = array;
            if (array == null) {
                this.list = null;
            } else {
                this.list = new ArrayList<>();
                for (int val: array) {
                    list.add(val);
                }
            }
        }

        public String getMessage() {
            return "Test message";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + Objects.hashCode(this.number);
            hash = 67 * hash + Objects.hashCode(this.date);
            hash = 67 * hash + Objects.hashCode(this.string);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestObject other = (TestObject) obj;
            if (!Objects.equals(this.string, other.string)) {
                return false;
            }
            if (!Objects.equals(this.number, other.number)) {
                return false;
            }
            if (!Objects.equals(this.date, other.date)) {
                return false;
            }
            return true;
        }
    }
}
