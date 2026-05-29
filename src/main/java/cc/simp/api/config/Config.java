package cc.simp.api.config;

import cc.simp.Simp;
import cc.simp.utils.render.DragUtils;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;

public final class Config implements Serializable {

    private final String name;
    private final File file;

    public Config(String name) {
        this.name = name;
        this.file = new File(ConfigManager.CONFIGS_DIR, name + ConfigManager.EXTENSION);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();
        JsonObject draggingObject = new JsonObject();

        for (cc.simp.modules.Module module : Simp.INSTANCE.getModuleManager().getModules())
            modulesObject.add(module.getLabel(), module.save(false));

        for (String key : DragUtils.components.keySet()) {
            draggingObject.add(key, DragUtils.components.get(key).save());
        }

        jsonObject.add("Modules", modulesObject);
        jsonObject.add("Dragging", draggingObject);
        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        if (object.has("Modules")) {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (cc.simp.modules.Module module : Simp.INSTANCE.getModuleManager().getModules()) {
                if (modulesObject.has(module.getLabel()))
                    module.load(modulesObject.getAsJsonObject(module.getLabel()));
            }
        }
        if (object.has("Dragging")) {
            JsonObject draggingObject = object.getAsJsonObject("Dragging");
            for (String key : draggingObject.keySet()) {
                JsonObject componentData = draggingObject.getAsJsonObject(key);
                if (!DragUtils.components.containsKey(key)) {
                    DragUtils.components.put(key, new DragUtils.DraggableComponent(0, 0));
                }
                DragUtils.components.get(key).load(componentData);
            }
        }
    }
}
