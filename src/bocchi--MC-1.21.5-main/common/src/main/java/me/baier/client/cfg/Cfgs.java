package me.baier.client.cfg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.baier.client.Bocchi;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public enum Cfgs {
    INSTANCE;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public void initialize() {
        final var defaultCfgFile = Bocchi.INSTANCE.getBase().resolve("default.json").toFile();
        final var defaultCfg = new Cfg("default");
        if (!defaultCfgFile.exists()) {
            try {
                FileUtils.writeStringToFile(
                        defaultCfgFile,
                        gson.toJson(defaultCfg.save(new JsonObject())),
                        "UTF-8"
                );
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path cfgDir = Bocchi.INSTANCE.getBase().resolve("cfgs");
        if (!cfgDir.toFile().exists()) cfgDir.toFile().mkdirs();
        try (final var list = Files.list(cfgDir)) {
            list.filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadConfigFile);
        } catch (IOException e) {
            Bocchi.INSTANCE.getLogger().error("Failed to scan config directory: {}", e.getMessage());
        }
    }

    private void loadConfigFile(Path filePath) {
        try {
            JsonObject json = gson.fromJson(
                    Files.newBufferedReader(filePath),
                    JsonObject.class
            );
            final String name = json.get("name").getAsString();
            Cfg cfg = new Cfg(name);
            cfg.load(json);

            Bocchi.INSTANCE.getLogger().info("Loaded config: {}", name);
        } catch (Exception e) {
            Bocchi.INSTANCE.getLogger().error("Failed to load config file {}: {}",
                    filePath, e.getMessage());
        }
    }
}
