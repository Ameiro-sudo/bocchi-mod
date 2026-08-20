package me.baier.client;

import com.google.gson.JsonObject;

public interface Saveable {
    JsonObject save(final JsonObject json);
    void load(final JsonObject json);
}
