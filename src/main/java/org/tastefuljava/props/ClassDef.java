package org.tastefuljava.props;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassDef<T> {
    private static final Logger LOG
            = Logger.getLogger(FieldProperty.class.getName());
    private static final Map<Class<?>,ClassDef> CLASSES = new HashMap<>();

    private final Class<T> clazz;
    private final Map<String,PropertyDef> props = new LinkedHashMap<>();

    public static <T> ClassDef<T> forClass(Class<T> clazz) {
        ClassDef<T> def = CLASSES.get(clazz);
        if (def == null) {
            def = new ClassDef(clazz);
            CLASSES.put(clazz, def);
        }
        return def;
    }

    private ClassDef(Class<T> clazz) {
        this.clazz = clazz;
        extractProps();
    }

    public Class<T> getJavaClass() {
        return clazz;
    }

    public String getName() {
        return clazz.getName();
    }

    public PropertyDef getProperty(String name) {
        return props.get(name);
    }

    public PropertyDef[] getProperties() {
        return props.values().toArray(new PropertyDef[props.size()]);
    }

    private void extractFieldProps() {
        for (Class<?> cl = clazz; cl != Object.class;
                cl = cl.getSuperclass()) {
            for (Field field: cl.getDeclaredFields()) {
                String name = field.getName();
                int mods = field.getModifiers();
                if (!props.containsKey(name)
                        && !Modifier.isStatic(mods)
                        && !Modifier.isTransient(mods)
                        && !field.getName().startsWith("this$")
                        && !field.getName().startsWith("val$")) {
                    field.setAccessible(true);
                    props.put(name, new FieldProperty(field));
                }
            }
        }
    }

    private void extractMethodProps() {
        final Method[] methods = clazz.getMethods();
        Map<String,Method> getters = extractGetters(methods);
        Map<String,Method> setters = extractSetters(methods);
        for (Map.Entry<String,Method> e: getters.entrySet()) {
            String name = e.getKey();
            Method getter = e.getValue();
            Method setter = setters.get(name);
            props.put(name, new MethodProperty(
                    name, getter.getReturnType(), getter, setter));
        }
        for (Map.Entry<String,Method> e: setters.entrySet()) {
            String name = e.getKey();
            if (!getters.containsKey(name)) {
                Method setter = e.getValue();
                props.put(name, new MethodProperty(
                        name, setter.getParameterTypes()[0], null, setter));
            }
        }
    }

    private static Map<String, Method> extractGetters(Method[] methods) {
        Map<String, Method> getters = new LinkedHashMap<>();
        for (Method method: methods) {
            if (method.getDeclaringClass() == Object.class) {
                // skip
            } else if (method.getParameterTypes().length > 0) {
                // skip
            } else {
                String name = method.getName();
                String propName = null;
                if (name.startsWith("get") && name.length() > 3) {
                    propName = Character.toLowerCase(name.charAt(3))
                            + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2) {
                    Class<?> retType = method.getReturnType();
                    if (retType == boolean.class
                            || retType == Boolean.class) {
                        propName = Character.toLowerCase(name.charAt(3))
                                + name.substring(4);
                    }
                }
                if (propName != null && !getters.containsKey(propName)) {
                    getters.put(propName, method);
                }
            }
        }
        return getters;
    }

    private static Map<String, Method> extractSetters(Method[] methods) {
        Map<String, Method> setters = new LinkedHashMap<>();
        for (Method method: methods) {
            if (method.getDeclaringClass() == Object.class) {
                // skip
            } else if (method.getParameterTypes().length != 1) {
                // skip
            } else {
                String name = method.getName();
                String propName = null;
                if (name.startsWith("set") && name.length() > 3) {
                    propName = Character.toLowerCase(name.charAt(3))
                            + name.substring(4);
                }
                if (propName != null && !setters.containsKey(propName)) {
                    setters.put(propName, method);
                }
            }
        }
        return setters;
    }

    private void extractProps() {
        long tm = System.nanoTime();
        extractFieldProps();
        extractMethodProps();
        tm = System.nanoTime()-tm;
        LOG.log(Level.INFO, "Properties extracted in {0}ns", tm);
    }
}
