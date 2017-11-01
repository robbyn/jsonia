package org.tastefuljava.jsonia.props;

public abstract class PropertyDef {
    private final String name;
    private final Class<?> type;

    protected PropertyDef(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public abstract boolean canGet();
    public abstract boolean canSet();
    public abstract Object get(Object object);
    public abstract void set(Object object, Object value);
}
