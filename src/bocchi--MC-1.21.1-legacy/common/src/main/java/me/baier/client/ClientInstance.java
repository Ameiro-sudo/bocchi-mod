package me.baier.client;

import net.minecraft.client.Minecraft;

public interface ClientInstance {
    Minecraft mc = Minecraft.getInstance();
}