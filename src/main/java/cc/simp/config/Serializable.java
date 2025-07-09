package cc.simp.config;

import com.google.gson.JsonObject;

public interface Serializable {

    JsonObject save();

    void load(JsonObject object);

}
