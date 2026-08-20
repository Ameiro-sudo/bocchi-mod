package me.baier.skui.layout.impl;

import me.baier.skui.SkComponent;
import me.baier.skui.layout.AbstractLayout;

import java.util.List;

public class FlowVerticalLayout extends AbstractLayout {
  @Override
  public void applyLayout(SkComponent parent) {
    List<SkComponent> children = parent.getChildren();

    float currentY = padding;
    for (SkComponent child : children) {
      child.setX(padding + child.getMarginLeft());
      child.setY(currentY + child.getMarginTop());
      currentY += child.getHeight() + child.getMarginTop() + child.getMarginBottom() + spacing;
    }
  }
}
