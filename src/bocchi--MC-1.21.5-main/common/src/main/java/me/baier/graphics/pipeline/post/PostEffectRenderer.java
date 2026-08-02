package me.baier.graphics.pipeline.post;

import me.baier.client.ClientInstance;

public class PostEffectRenderer implements ClientInstance {
    public static PostEffect current;

    public static void beginDraw() {
        if (current != null) {
            current.pre();
        }
    }

    public static void postDraw() {
        if (current != null) {
            current.post();
        }
    }

    public static void render() {
        if (current != null) {
            current.render();
        }
    }

    public static void set(PostEffect effect) {
        if (effect == current) return;

        PostEffectRenderer.current = effect;
        if (effect != null) {
            effect.onResized(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
        }
    }

    public static void clear() {
        set(null);
    }

    public static void onResized(int width, int height) {
        if (current != null) {
            current.onResized(width, height);
        }
    }
}
