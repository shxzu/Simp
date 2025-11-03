package cc.simp.api.config;

import cc.simp.Simp;
import cc.simp.processes.DraggingProcess;
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
            modulesObject.add(module.getLabel(), module.save());

        for (String key : DraggingProcess.components.keySet()) {
            draggingObject.add(key, DraggingProcess.components.get(key).save());
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
                if (!DraggingProcess.components.containsKey(key)) {
                    DraggingProcess.components.put(key, new DraggingProcess.DraggableComponent(0, 0));
                }
                DraggingProcess.components.get(key).load(componentData);
            }
        }
    }
}
