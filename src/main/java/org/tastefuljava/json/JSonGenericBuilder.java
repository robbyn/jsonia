package org.tastefuljava.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSonGenericBuilder extends AbstractJSonBuilder {
    private final List<Object> stack = new ArrayList<>();

    @Override
    public void startObject() {
        stack.add(0, new HashMap<>());
    }

    @Override
    public void endObject() {
        top = stack.remove(0);
    }

    @Override
    public void startField(String name) {
    }

    @Override
    public void endField(String name) {
        Map<String,Object> object = (Map<String,Object>)stack.get(0);
        object.put(name, top);
    }

    @Override
    public void startArray() {
        stack.add(0, new ArrayList<>());
    }

    @Override
    public void endArray() {
        top = stack.remove(0);
    }

    @Override
    public void startElement() {
    }

    @Override
    public void endElement() {
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
}
