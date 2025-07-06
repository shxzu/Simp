package cc.simp.property.impl;

import cc.simp.property.Property;

import java.util.function.Supplier;

public class BooleanProperty extends Property<Boolean> {
	private String dependency; 
	
	public BooleanProperty(String name, boolean defaultValue, Supplier<Boolean> dependency) {
        super(name, defaultValue, dependency);
    }
    public BooleanProperty(String name, boolean defaultValue) {
        super(name, defaultValue, () -> true);
    }

    public void toggle() {
        this.setValue(!this.getValue());
    }
}
