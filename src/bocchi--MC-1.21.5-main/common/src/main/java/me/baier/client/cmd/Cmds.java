package me.baier.client.cmd;

import com.mojang.brigadier.CommandDispatcher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Cmds {
    INSTANCE;

    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();

    public void initialize() {

    }

    private void registerCommands(final Class<? extends Cmd<?>> klass) {
        try {
            Cmd<?> cmd = klass.getConstructor(CommandDispatcher.class).newInstance(dispatcher);
        } catch (Exception e) {
            log.error("Failed to register command {}", klass.getName(), e);
        }

    }
}
