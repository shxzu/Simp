package cc.simp.modules;

import cc.simp.Simp;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.property.impl.MultiSelectEnumProperty;
import cc.simp.utils.client.Manager;
import cc.simp.utils.client.misc.StringUtils;
import cc.simp.utils.client.render.Translate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

public class Module extends Manager<Property<?>> implements Toggleable, Serializable {

    private final String label = getClass().getAnnotation(ModuleInfo.class).label();
    private final String description = getClass().getAnnotation(ModuleInfo.class).description();
    private final ModuleCategory category = getClass().getAnnotation(ModuleInfo.class).category();
    private final Translate translate = new Translate(0.0, 0.0);
    private int key = getClass().getAnnotation(ModuleInfo.class).key();
    private boolean enabled;
    private boolean hidden;
    private Supplier<String> suffix;
    private String updatedSuffix;

    public String getUpdatedSuffix() {
        return updatedSuffix;
    }

    public void setUpdatedSuffix(String updatedSuffix) {
        this.updatedSuffix = updatedSuffix;
    }

    private void updateSuffix(EnumProperty<?> mode) {
        setUpdatedSuffix(StringUtils.upperSnakeCaseToPascal(mode.getValue().name()));
    }

    public void setSuffixListener(EnumProperty<?> mode) {
        updateSuffix(mode);
        mode.addValueChangeListener((oldValue, value) -> updateSuffix(mode));
    }

    public void resetPropertyValues() {
        for (Property<?> property : getElements())
            property.callFirstTime();
    }

    public Translate getTranslate() {
        return translate;
    }

    public Supplier<String> getSuffix() {
        return suffix;
    }

    public void setSuffix(Supplier<String> suffix) {
        this.suffix = suffix;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public void reflectProperties() {
        for (final Field field : getClass().getDeclaredFields()) {
            final Class<?> type = field.getType();
            if (type.isAssignableFrom(Property.class) ||
                    type.isAssignableFrom(DoubleProperty.class) ||
                    type.isAssignableFrom(EnumProperty.class) ||
                    type.isAssignableFrom(MultiSelectEnumProperty.class)) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                try {
                    elements.add((Property<?>) field.get(this));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;

            if (enabled) {
                onEnable();
                Simp.INSTANCE.getEventBus().subscribe(this);
            } else {
                Simp.INSTANCE.getEventBus().unsubscribe(this);
                onDisable();
            }
        }
    }

    public boolean isVisible() {
        return enabled && !hidden;
    }

    @Override
    public void toggle() {
        setEnabled(!enabled);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    private static <T extends Enum<T>> void findEnumValue(Property<?> property, JsonObject propertiesObject) {
        EnumProperty<T> enumProperty = (EnumProperty<T>) property;
        String value = propertiesObject.getAsJsonPrimitive(property.getLabel()).getAsString();
        for (T possibleValue : enumProperty.getValues()) {
            if (possibleValue.name().equalsIgnoreCase(value)) {
                enumProperty.setValue(possibleValue);
                break;
            }
        }
    }
}
