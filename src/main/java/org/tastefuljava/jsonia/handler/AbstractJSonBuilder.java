package org.tastefuljava.jsonia.handler;

import org.tastefuljava.jsonia.JSonHandler;

public abstract class AbstractJSonBuilder implements JSonHandler {
    protected Object top;

    public Object getTop() {
        return top;
    }
}
