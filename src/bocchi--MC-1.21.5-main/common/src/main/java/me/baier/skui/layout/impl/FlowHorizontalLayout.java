package me.baier.skui.layout.impl;

import me.baier.skui.SkComponent;
import me.baier.skui.layout.AbstractLayout;

import java.util.List;

public class FlowHorizontalLayout extends AbstractLayout {
  @Override
  public void applyLayout(SkComponent parent) {
    List<SkComponent> children = parent.getChildren();

    float currentX = padding;
    for (SkComponent child : children) {
      child.setX(currentX + child.getMarginLeft());
      child.setY(padding + child.getMarginTop());
      currentX += child.getWidth() + child.getMarginLeft() + child.getMarginRight() + spacing;
    }
  }
}
