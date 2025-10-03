package cc.simp.api.properties.impl;

import cc.simp.api.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class MultiModeProperty<T extends Enum<T>> extends Property<List<T>> {

    private final T[] values;

    @SafeVarargs
    public MultiModeProperty(String label, Supplier<Boolean> dependency, T... values) {
        super(label, Arrays.asList(values), dependency);

        if (values.length == 0)
            throw new RuntimeException("Must have at least one default value.");

        this.values = getEnumConstants();
    }

    @SafeVarargs
    public MultiModeProperty(String label, T... values) {
        this(label, () -> true, values);
    }

    @SuppressWarnings("unchecked")
    private T[] getEnumConstants() {
        return (T[]) value.get(0).getClass().getEnumConstants();
    }

    public T[] getValues() {
        return values;
    }

    public boolean isSelected(T variant) {
        return getValue().contains(variant);
    }

    public void setValue(int index) {
        final List<T> values = new ArrayList<>(this.value);
        final T referencedVariant = this.values[index];
        if (values.contains(referencedVariant)) {
            values.remove(referencedVariant);
        } else {
            values.add(referencedVariant);
        }
        setValue(values);
    }
}
