package me.baier.client.cmd.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import me.baier.client.cmd.Cmd;

import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class Toggle extends Cmd {

    public Toggle(CommandDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    protected LiteralArgumentBuilder name(String name) {
        return super.name("toggle");
    }

    @Override
    protected LiteralArgumentBuilder then(CommandNode argument) {
        return super.then(argument("mod", StringArgumentType.string()).executes(ctx -> {

            return 1;
        }).build());
    }
}
