package cc.simp.api.config;

import cc.simp.Simp;
import cc.simp.modules.Module;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;

public final class BindsConfig implements Serializable {

    private static final File BINDS_FILE = new File(Simp.NAME, "binds.json");

    public BindsConfig() {
        if (!BINDS_FILE.exists()) {
            try {
                BINDS_FILE.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean saveToFile() {
        JsonObject object = save();
        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        try {
            FileWriter writer = new FileWriter(BINDS_FILE);
            writer.write(contentPrettyPrint);
            writer.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean loadFromFile() {
        if (!BINDS_FILE.exists()) return false;
        try {
            FileReader reader = new FileReader(BINDS_FILE);
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(reader);

            // Check if the parsed element is a JsonObject
            if (element == null || !element.isJsonObject()) {
                reader.close();
                return false;
            }

            JsonObject object = element.getAsJsonObject();
            load(object);
            reader.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("key", module.getKey());
            modulesObject.add(module.getLabel(), moduleObject);
        }

        jsonObject.add("Modules", modulesObject);
        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        if (object.has("Modules")) {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : Simp.INSTANCE.getModuleManager().getModules()) {
                if (modulesObject.has(module.getLabel())) {
                    JsonObject moduleObject = modulesObject.getAsJsonObject(module.getLabel());
                    if (moduleObject.has("key")) {
                        module.setKey(moduleObject.get("key").getAsInt());
                    }
                }
            }
        }
    }
}