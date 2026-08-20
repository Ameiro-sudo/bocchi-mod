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
        Path cfgDir = Bocchi.INSTANCE.getBase().resolve("cfgs");
        try {
            Files.createDirectories(cfgDir);
        } catch (IOException e) {
            Bocchi.INSTANCE.getLogger().error("Failed to create config directory: {}", e.getMessage());
            return;
        }
        // 默认配置写入 cfgs/ 目录, 与其他用户配置同目录, 启动时一起被加载
        final var defaultCfgFile = cfgDir.resolve("default.json").toFile();
        final var defaultCfg = new Cfg("default");
        if (!defaultCfgFile.exists()) {
            try {
                FileUtils.writeStringToFile(
                        defaultCfgFile,
                        gson.toJson(defaultCfg.save(new JsonObject())),
                        "UTF-8"
                );
            } catch (IOException e) {
                Bocchi.INSTANCE.getLogger().error("Failed to write default config", e);
            }
        }
        try (final var list = Files.list(cfgDir)) {
            list.filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .forEach(this::loadConfigFile);
        } catch (IOException e) {
            Bocchi.INSTANCE.getLogger().error("Failed to scan config directory: {}", e.getMessage());
        }
    }

    private void loadConfigFile(Path filePath) {
        try (var reader = Files.newBufferedReader(filePath)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            final String name = json.has("name")
                    ? json.get("name").getAsString()
                    : filePath.getFileName().toString();
            Cfg cfg = new Cfg(name);
            cfg.load(json);

            Bocchi.INSTANCE.getLogger().info("Loaded config: {}", name);
        } catch (Exception e) {
            Bocchi.INSTANCE.getLogger().error("Failed to load config file {}: {}",
                    filePath, e.getMessage());
        }
    }
}
