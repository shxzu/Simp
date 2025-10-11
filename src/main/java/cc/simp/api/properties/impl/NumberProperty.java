package cc.simp.api.properties.impl;

import cc.simp.api.properties.Property;

import java.util.function.Supplier;

public class NumberProperty extends Property<Double> {

    private final double min;
    private final double max;
    private final double increment;
    private final Representation representation;

    public NumberProperty(String label, double value, Supplier<Boolean> dependency, double min, double max, double increment, Representation representation) {
        super(label, value, dependency);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.representation = representation;
    }

    public NumberProperty(String label, double value, Supplier<Boolean> dependency, double min, double max, double increment) {
        this(label, value, dependency, min, max, increment, Representation.DOUBLE);
    }

    public NumberProperty(String label, double value, double min, double max, double increment, Representation representation) {
        this(label, value, () -> true, min, max, increment, representation);
    }

    public NumberProperty(String label, double value, double min, double max, double increment) {
        this(label, value, () -> true, min, max, increment, Representation.DOUBLE);
    }

    public Representation getRepresentation() {
        return representation;
    }

    @Override
    public void setValue(Double value) {
        if (this.value != null && this.value.doubleValue() != value.doubleValue()) {
            if (value < min)
                value = min;
            else if (value > max)
                value = max;
        }

        super.setValue(value);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }

    public Number getRandomBetween() {
        long min = (long) this.getMin();
        long max = (long) this.getMax();

        if (min == max) {
            return min;
        } else if (min > max) {
            final long d = min;
            min = max;
            max = d;
        }

        long random = (long) (min + (max - min) * Math.random() * Math.random());
        return new Number() {
            @Override
            public int intValue() {
                return Math.round(random);
            }

            @Override
            public long longValue() {
                return random;
            }

            @Override
            public float floatValue() {
                return (float) random;
            }

            @Override
            public double doubleValue() {
                return (double) random;
            }
        };
    }

}
