package cc.simp.modules;

import cc.simp.Simp;
import cc.simp.api.config.Serializable;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.api.properties.impl.MultiModeProperty;
import cc.simp.api.properties.impl.NumberProperty;
import cc.simp.utils.misc.Manager;
import cc.simp.utils.misc.StringUtils;
import cc.simp.utils.render.Translate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

public class Module extends Manager<Property<?>> implements Toggleable, Serializable {

    private final String label = getClass().getAnnotation(ModuleInfo.class).label();
    private final String description = getClass().getAnnotation(ModuleInfo.class).description();
    private final ModuleCategory category = getClass().getAnnotation(ModuleInfo.class).category();
    private int key = getClass().getAnnotation(ModuleInfo.class).key();
    private boolean enabled;
    private boolean hidden;
    private String suffix;
    private final Translate translate = new Translate(0.0, 0.0);

    public void resetPropertyValues() {
        for (Property<?> property : getElements())
            property.callFirstTime();
    }

    public String getSuffix() {
        return suffix;
    }

    public Translate getTranslate() {
        return translate;
    }

    public void setSuffix(String suffix) {
        suffix = suffix;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public void reflectProperties() {
        for (final Field field : getClass().getDeclaredFields()) {
            final Class<?> type = field.getType();
            if (type.isAssignableFrom(Property.class) ||
                    type.isAssignableFrom(NumberProperty.class) ||
                    type.isAssignableFrom(ModeProperty.class) ||
                    type.isAssignableFrom(MultiModeProperty.class)) {
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

    @Override
    public JsonObject save() {
        JsonObject object = new JsonObject();
        object.addProperty("toggled", isEnabled());
        object.addProperty("key", getKey());
        object.addProperty("hidden", isHidden());
        List<Property<?>> properties = getElements();
        if (!properties.isEmpty()) {
            JsonObject propertiesObject = new JsonObject();

            for (Property<?> property : properties) {
                if (property instanceof NumberProperty) {
                    propertiesObject.addProperty(property.getLabel(), ((NumberProperty) property).getValue());
                } else if (property instanceof ModeProperty) {
                    ModeProperty<?> ModeProperty = (ModeProperty<?>) property;
                    propertiesObject.add(property.getLabel(), new JsonPrimitive(ModeProperty.getValue().name()));
                } else if (property instanceof MultiModeProperty) {
                    MultiModeProperty<?> multiSelect = (MultiModeProperty<?>) property;
                    final JsonArray array = new JsonArray();
                    for (Enum<?> e : multiSelect.getValues()) {
                        array.add(new JsonPrimitive(e.name()));
                    }
                    propertiesObject.add(property.getLabel(), array);
                } else if (property.getType() == Boolean.class) {
                    propertiesObject.addProperty(property.getLabel(), (Boolean) property.getValue());
                } else if (property.getType() == Integer.class) {
                    propertiesObject.addProperty(property.getLabel(), Integer.toHexString((Integer) property.getValue()));
                } else if (property.getType() == String.class) {
                    propertiesObject.addProperty(property.getLabel(), (String) property.getValue());
                }
            }

            object.add("Properties", propertiesObject);
        }
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(JsonObject object) {
        if (object.has("toggled"))
            setEnabled(object.get("toggled").getAsBoolean());

        if (object.has("key"))
            setKey(object.get("key").getAsInt());

        if (object.has("hidden"))
            setHidden(object.get("hidden").getAsBoolean());

        if (object.has("Properties") && !getElements().isEmpty()) {
            JsonObject propertiesObject = object.getAsJsonObject("Properties");
            for (Property<?> property : getElements()) {
                if (propertiesObject.has(property.getLabel())) {
                    if (property instanceof NumberProperty) {
                        ((NumberProperty) property).setValue(propertiesObject.get(property.getLabel()).getAsDouble());
                    } else if (property instanceof ModeProperty) {
                        findEnumValue(property, propertiesObject);
                    } else if (property instanceof MultiModeProperty) {

                    } else if (property.getValue() instanceof Boolean) {
                        ((Property<Boolean>) property).setValue(propertiesObject.get(property.getLabel()).getAsBoolean());
                    } else if (property.getValue() instanceof Integer) {
                        ((Property<Integer>) property).setValue((int) Long.parseLong(propertiesObject.get(property.getLabel()).getAsString(), 16));
                    } else if (property.getValue() instanceof String) {
                        ((Property<String>) property).setValue(propertiesObject.get(property.getLabel()).getAsString());
                    }
                }
            }
        }
    }

    private static <T extends Enum<T>> void findEnumValue(Property<?> property, JsonObject propertiesObject) {
        ModeProperty<T> ModeProperty = (ModeProperty<T>) property;
        String value = propertiesObject.getAsJsonPrimitive(property.getLabel()).getAsString();
        for (T possibleValue : ModeProperty.getValues()) {
            if (possibleValue.name().equalsIgnoreCase(value)) {
                ModeProperty.setValue(possibleValue);
                break;
            }
        }
    }
}
