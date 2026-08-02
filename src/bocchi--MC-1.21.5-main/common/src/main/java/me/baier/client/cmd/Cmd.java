package me.baier.client.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import lombok.Getter;
import me.baier.client.Labelable;

@Getter
public abstract class Cmd<S> implements Labelable {
    private String label;
    protected final CommandDispatcher<S> dispatcher;

    public Cmd(CommandDispatcher<S> dispatcher) {
        this.dispatcher = dispatcher;
    }

    protected LiteralArgumentBuilder<S> name(final String name) {
        this.label = name;
        return LiteralArgumentBuilder.literal(name);
    }

    protected LiteralArgumentBuilder<S> then(final CommandNode<S> argument) {
        return this.name(this.getLabel()).then(argument);
    }
}