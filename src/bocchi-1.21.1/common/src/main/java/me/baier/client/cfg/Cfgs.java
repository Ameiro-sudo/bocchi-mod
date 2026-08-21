package me.baier.client.cfg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import lombok.Getter;
import me.baier.client.Bocchi;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public enum Cfgs {
    INSTANCE;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /** 最近一次成功加载(或保存)的配置名; save() 无参调用时写回该文件, 保证"最后加载者获胜"语义闭环 */
    @Getter
    private String activeName = "default";

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

    /**
     * 把当前 Mods 状态回写到 activeName 对应的配置文件 (与 {@link #loadConfigFile} 对称:
     * 同一 gson 实例、同一 "{name, <mod>: {setting: {value}}} 结构、UTF-8 落盘")。
     */
    public void save() {
        save(activeName);
    }

    public void save(String name) {
        Path cfgDir = Bocchi.INSTANCE.getBase().resolve("cfgs");
        try {
            Files.createDirectories(cfgDir);
            JsonObject json = new Cfg(name).save(new JsonObject());
            // 先写临时文件再原子替换, 避免进程中断留下半截 JSON (损坏文件在加载侧只会静默丢弃)
            final var tmp = cfgDir.resolve(name + ".json.tmp");
            FileUtils.writeStringToFile(tmp.toFile(), gson.toJson(json), "UTF-8");
            final var target = cfgDir.resolve(name + ".json");
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            this.activeName = name;
            Bocchi.INSTANCE.getLogger().info("Saved config: {}", name);
        } catch (IOException e) {
            Bocchi.INSTANCE.getLogger().error("Failed to save config {}: {}", name, e.getMessage());
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

            this.activeName = name;
            Bocchi.INSTANCE.getLogger().info("Loaded config: {}", name);
        } catch (Exception e) {
            Bocchi.INSTANCE.getLogger().error("Failed to load config file {}: {}",
                    filePath, e.getMessage());
        }
    }
}