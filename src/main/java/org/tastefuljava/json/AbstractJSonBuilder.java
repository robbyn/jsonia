package org.tastefuljava.json;

public abstract class AbstractJSonBuilder implements JSonHandler {
    protected Object top;

    public Object getTop() {
        return top;
    }
}
