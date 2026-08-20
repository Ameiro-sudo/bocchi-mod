package me.baier.skui.layout;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractLayout implements SkLayout {
    protected float padding = 0;
    protected float spacing = 0;

    public AbstractLayout() {}

    public AbstractLayout(float padding) {
        this.padding = padding;
    }

    public AbstractLayout(float padding, float spacing) {
        this.padding = padding;
        this.spacing = spacing;
    }
}