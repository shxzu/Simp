package cc.simp.api.properties.impl;

import cc.simp.api.properties.Property;

import java.util.function.Supplier;

public class ModeProperty<T extends Enum<T>> extends Property<T> {

    private final T[] values;

    public ModeProperty(String label, T value, Supplier<Boolean> dependency) {
        super(label, value, dependency);

        this.values = getEnumConstants();
    }

    public ModeProperty(String label, T value) {
        this(label, value, () -> true);
    }

    @SuppressWarnings("unchecked")
    private T[] getEnumConstants() {
        return (T[]) value.getClass().getEnumConstants();
    }

    public T[] getValues() {
        return values;
    }

    public void setValue(int index) {
        setValue(values[index]);
    }
}
