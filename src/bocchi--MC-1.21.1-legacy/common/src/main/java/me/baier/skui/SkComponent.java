package me.baier.skui;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Color;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import me.baier.graphics.SkiaEnvironment;
import me.baier.skui.layout.SkLayout;

public class SkComponent {

  public enum LifecycleState {
    INITIALIZED,
    ATTACHED,
    RENDERED,
    DETACHED,
    DISPOSED
  }

  /* Position and dimensions */
  @Getter @Setter private String name = UUID.randomUUID().toString();
  @Setter @Getter private float x;
  @Setter @Getter private float y;
  @Getter private float width;
  @Getter private float height;

  @Getter private int mouseX;
  @Getter private int mouseY;

  /* Hierarchy */
  @Getter @Setter private SkComponent parent;
  @Getter private final List<SkComponent> children = new ArrayList<>();
  @Getter private int zIndex = 0;

  /* State */
  @Getter @Setter private boolean visible = true;

  @Setter @Getter private boolean enabled = true;

  @Setter @Getter private boolean pressed = false;

  @Getter @Setter private boolean hovered = false;

  @Getter @Setter private boolean childHovered = false;

  @Getter @Setter private boolean clickThrough = false;

  @Getter private boolean focused = false;

  @Getter private LifecycleState lifecycleState = LifecycleState.INITIALIZED;

  private final ConcurrentLinkedQueue<Consumer<SkComponent>> updateListeners =
      new ConcurrentLinkedQueue<>();

  /* Layout */
  @Getter private SkLayout layout;

  @Getter private float marginLeft = 0;
  @Getter private float marginTop = 0;
  @Getter private float marginRight = 0;
  @Getter private float marginBottom = 0;

  /* Caching and optimizations */
  private boolean layoutDirty = true;

  /* Debug support */
  @Getter @Setter private boolean debugMode = false;

  /* Drag and drop support */
  @Getter @Setter private boolean draggable = false;
  @Getter private boolean isDragging = false;
  private float dragStartX, dragStartY;
  private float componentStartX, componentStartY;

  public SkComponent() {
    this(0, 0, 100, 100);
  }

  public SkComponent(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /* Lifecycle methods */
  public void initialize() {
    if (lifecycleState == LifecycleState.INITIALIZED) {
      return;
    }

    lifecycleState = LifecycleState.INITIALIZED;
    onInitialize();
  }

  public void addUpdateListener(Consumer<SkComponent> listener) {
    updateListeners.add(listener);
  }

  public void attach() {
    if (lifecycleState != LifecycleState.INITIALIZED && lifecycleState != LifecycleState.DETACHED) {
      return;
    }

    lifecycleState = LifecycleState.ATTACHED;
    onAttach();

    /* Attach all children */
    for (SkComponent child : children) {
      child.attach();
    }
  }

  public void render(SkiaEnvironment env, int mouseX, int mouseY) {
    if (lifecycleState != LifecycleState.ATTACHED && lifecycleState != LifecycleState.RENDERED) {
      return;
    }

    this.mouseX = mouseX;
    this.mouseY = mouseY;

    /* Ensure layout is up-to-date */
    if (layoutDirty) {
      updateLayout();
    }

    if (!updateListeners.isEmpty()) {
      while (!updateListeners.isEmpty()) {
        Consumer<SkComponent> listener = updateListeners.poll();
        listener.accept(this);
      }
    }

    lifecycleState = LifecycleState.RENDERED;
    onRender(env, mouseX, mouseY);

    /* Render all children */
    onRenderChildren(env, mouseX, mouseY);

    onRenderEnd(env, mouseX, mouseY);
  }

  public void detach() {
    if (lifecycleState != LifecycleState.ATTACHED && lifecycleState != LifecycleState.RENDERED) {
      return;
    }

    /* Detach all children first */
    for (SkComponent child : children) {
      child.detach();
    }

    lifecycleState = LifecycleState.DETACHED;
    onDetach();
  }

  public void dispose() {
    /* Dispose all children first */
    for (SkComponent child : children) {
      child.dispose();
    }

    lifecycleState = LifecycleState.DISPOSED;
    onDispose();

    /* Clear children */
    children.clear();
  }

  /* Lifecycle event handlers that can be overridden by subclasses */
  protected void onInitialize() {}

  protected void onAttach() {}

  protected void onRenderChildren(SkiaEnvironment env, int mouseX, int mouseY) {
    for (SkComponent child : children) {
      if (!child.isVisibleInHierarchy()) {
        continue;
      }
      child.render(env, mouseX, mouseY);
    }
  }

  protected void onRender(SkiaEnvironment env, int mouseX, int mouseY) {}

  protected void onRenderEnd(SkiaEnvironment env, int mouseX, int mouseY) {
    if (debugMode) {
      final Canvas canvas = env.getCanvas();
      final Paint paint = env.borrowPaint();
      paint.setColor(Color.makeARGB(10, 255, 0, 0));
      canvas.drawRect(
          Rect.makeXYWH(getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight()), paint);
      env.recyclePaint(paint);
    }
  }

  protected void onDetach() {}

  protected void onDispose() {}

  public void addChild(SkComponent... children) {
    for (SkComponent child : children) {
      if (child.parent != null) {
        child.parent.removeChild(child);
      }
      this.children.add(child);
      child.parent = this;

      /* Update the child's lifecycle state to match the parent's */
      if (lifecycleState == LifecycleState.ATTACHED || lifecycleState == LifecycleState.RENDERED) {
        child.attach();
      }
    }

    sortChildrenByZIndex();
    invalidateLayout();
  }

  public void removeChild(SkComponent child) {
    if (children.remove(child)) {
      /* Detach the child if it's currently attached */
      if (child.lifecycleState == LifecycleState.ATTACHED
          || child.lifecycleState == LifecycleState.RENDERED) {
        child.detach();
      }

      child.parent = null;
      invalidateLayout();
    }
  }

  public void removeAllChildren(Collection<SkComponent> childrenToKeep) {
    if (children.isEmpty()) {
      return;
    }

    List<SkComponent> copy = new ArrayList<>(children);

    // Clear the children list first
    children.clear();

    for (SkComponent child : copy) {
      if (childrenToKeep != null && childrenToKeep.contains(child)) {
        // Re-add the children we want to keep
        children.add(child);
      } else {
        // Detach the children we want to remove
        if (child.lifecycleState == LifecycleState.ATTACHED
            || child.lifecycleState == LifecycleState.RENDERED) {
          child.detach();
        }
        child.parent = null;
      }
    }

    invalidateLayout();
  }

  public void removeAllChildren() {
    removeAllChildren(null);
  }

  public void setZIndex(int zIndex) {
    setZIndex(zIndex, false);
  }

  public void setZIndex(int zIndex, boolean updateNow) {
    this.zIndex = zIndex;
    if (parent != null) {
      /* Sort parent's children by zIndex */
      if (updateNow) {
        parent.sortChildrenByZIndex();
      } else {
        parent.addUpdateListener(SkComponent::sortChildrenByZIndex);
      }
    }
  }

  private void sortChildrenByZIndex() {
    children.sort(Comparator.comparingInt(SkComponent::getZIndex));
  }

  /* Visibility methods */
  public boolean isVisibleInHierarchy() {
    if (!visible) return false;
    return parent == null || parent.isVisibleInHierarchy();
  }

  /* Enable/Disable methods */
  public boolean isEnabledInHierarchy() {
    if (!enabled) return false;
    return parent == null || parent.isEnabledInHierarchy();
  }

  public void setLayout(SkLayout layout) {
    this.layout = layout;
    invalidateLayout();
  }

  public void setMargin(float margin) {
    setMargin(margin, margin, margin, margin);
  }

  public void setMargin(float left, float top, float right, float bottom) {
    this.marginLeft = left;
    this.marginTop = top;
    this.marginRight = right;
    this.marginBottom = bottom;
    invalidateLayout();
  }

  public void invalidateLayout() {
    if (layoutDirty) {
      return;
    }
    layoutDirty = true;
    SkComponent current = parent;
    while (current != null && !current.layoutDirty) {
      current.layoutDirty = true;
      current = current.parent;
    }
  }

  private void updateLayout() {
    if (!layoutDirty) {
      return;
    }

    if (layout != null) {
      layout.applyLayout(this);
    }

    layoutDirty = false;
  }

  public float getAbsoluteX() {
    return parent == null ? x : parent.getAbsoluteX() + x;
  }

  public float getAbsoluteY() {
    return parent == null ? y : parent.getAbsoluteY() + y;
  }

  public void setWidth(float width) {
    if (this.width != width) {
      this.width = width;
      invalidateLayout();
    }
  }

  public void setFocused(boolean focused) {
    boolean wasFocused = this.focused;
    this.focused = focused;

    if (!wasFocused && focused) {
      onFocusGained();
    } else if (wasFocused && !focused) {
      onFocusLost();
    }
  }

  public void setHeight(float height) {
    /* Apply constraints */
    if (this.height != height) {
      onHeightChange(height);
      this.height = height;
      invalidateLayout();
    }
  }

  public boolean containsPoint(int pointX, int pointY) {
    float absX = getAbsoluteX();
    float absY = getAbsoluteY();
    return containsPoint(pointX, pointY, absX, absY, width, height);
  }

  public static boolean containsPoint(
      int pointX, int pointY, float absX, float absY, float width, float height) {
    return pointX >= absX && pointX <= absX + width && pointY >= absY && pointY <= absY + height;
  }

  public void handleResizeWindow() {
    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return;
    }

    invalidateLayout();

    this.onResizeWindow();
    for (int i = children.size() - 1; i >= 0; i--) {
      SkComponent child = children.get(i);
      child.handleResizeWindow();
    }
  }

  public void handleMouseMove(int mouseX, int mouseY) {
    boolean wasHovered = hovered;

    /* Check if the mouse is over the current component */
    boolean shouldBeHovered =
        isVisibleInHierarchy()
            && isEnabledInHierarchy()
            && containsPoint(mouseX, mouseY)
            && !clickThrough;

    /* Handle dragging if applicable */
    if (isDragging) {
      float deltaX = mouseX - dragStartX;
      float deltaY = mouseY - dragStartY;
      setX(componentStartX + deltaX);
      setY(componentStartY + deltaY);
      // invalidateLayout();
      onDragMove(deltaX, deltaY);
    }

    if (clickThrough) {
      setHovered(false);
      setChildHovered(false);
      return;
    }

    /* Update the hover state of all child components */
    childHovered = false;
    for (int i = children.size() - 1; i >= 0; i--) {
      SkComponent child = children.get(i);

      child.setClickThrough(!shouldForwardHover(child, mouseX, mouseY) && shouldBeHovered);
      if (childHovered) {
        handleAllDescendantsMove(child, true);
      }

      child.handleMouseMove(mouseX, mouseY);
      if (child.isHovered() || child.isChildHovered()) {
        childHovered = true;
      }
    }

    /* If a child component is hovered, the current component should not be hovered */
    if (childHovered) {
      shouldBeHovered = false;
    }

    /* Set hover state */
    setHovered(shouldBeHovered);

    /* If the hovering state changes, an event can be triggered */
    if (wasHovered != hovered) {
      if (hovered) {
        onMouseEnter();
      } else {
        onMouseExit();
      }
    }
  }

  public boolean isValidMousePos() {
    return mouseX != 0 || mouseY != 0;
  }

  private void handleAllDescendantsMove(SkComponent component, boolean clickThrough) {
    component.setClickThrough(clickThrough);
    for (SkComponent child : component.getChildren()) {
      child.setHovered(false);
      child.setChildHovered(false);
      child.setClickThrough(clickThrough);
      handleAllDescendantsMove(child, clickThrough);
    }
  }

  public boolean handleMouseClick(int mouseX, int mouseY, int button) {
    /* Ensure layout is up-to-date */
    if (layoutDirty) {
      updateLayout();
    }

    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return false;
    }

    if (onPreMouseClick(mouseX, mouseY, button)) {
      return true;
    }

    /* First, check the child components (from back to front, in order to correctly handle the z-index) */
    for (int i = children.size() - 1; i >= 0; i--) {
      if (i >= children.size()) {
        break;
      }

      SkComponent child = children.get(i);
      if (child.handleMouseClick(mouseX, mouseY, button)) {
        return true;
      }
    }

    if (isHovered() && !clickThrough) {
      SkComponent root = getRoot();
      root.clearFocusRecursive(this);
      setFocused(true);

      if (draggable && button == 0) {
        isDragging = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        componentStartX = x;
        componentStartY = y;
        onDragStart(mouseX, mouseY);
      }

      setPressed(true);
      onMouseClick(mouseX, mouseY, button);
      return true;
    }

    return false;
  }

  public boolean handleMouseRelease(int mouseX, int mouseY, int button) {
    /* End drag operation if applicable */
    if (isDragging) {
      isDragging = false;
      onDragEnd(mouseX, mouseY);
    }

    /* If the component is invisible or disabled, do not handle the click event */
    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return false;
    }

    boolean handled = false;
    for (int i = children.size() - 1; i >= 0; i--) {
      if (i >= children.size()) {
        break;
      }

      SkComponent child = children.get(i);
      /*
          For release events, we usually check all child components
          If any child component handles the event, we mark it as handled, but continue propagating
      */
      if (child.handleMouseRelease(mouseX, mouseY, button)) {
        handled = true;
      }
    }

    if (isPressed()) {
      setPressed(false);
      return onMouseRelease(mouseX, mouseY, button);
    }

    /* Return whether any child component handled the event */
    return handled;
  }

  public boolean handleMouseScroll(int mouseX, int mouseY, int scroll) {
    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return false;
    }

    for (int i = children.size() - 1; i >= 0; i--) {
      if (i >= children.size()) {
        break;
      }

      SkComponent child = children.get(i);
      if (child.handleMouseScroll(mouseX, mouseY, scroll)) {
        return true;
      }
    }

    if (isHovered() || isChildHovered()) {
      return onMouseScroll(mouseX, mouseY, scroll);
    }

    return false;
  }

  public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return false;
    }

    for (int i = children.size() - 1; i >= 0; i--) {
      if (i >= children.size()) {
        break;
      }

      SkComponent child = children.get(i);
      if (child.handleKeyPressed(keyCode, scanCode, modifiers)) {
        return true;
      }
    }

    return onKeyPressed(keyCode, scanCode, modifiers);
  }

  public boolean handleCharTyped(char chr, int modifiers) {
    if (!isVisibleInHierarchy() || !isEnabledInHierarchy()) {
      return false;
    }

    for (int i = children.size() - 1; i >= 0; i--) {
      if (i >= children.size()) {
        break;
      }

      SkComponent child = children.get(i);
      if (child.handleCharTyped(chr, modifiers)) {
        return true;
      }
    }

    return onCharTyped(chr, modifiers);
  }

  /* Focus management */
  public void clearFocusRecursive(SkComponent skip) {
    for (SkComponent child : children) {
      child.clearFocusRecursive(skip);
    }

    if (this != skip) {
      setFocused(false);
    }
  }

  public SkComponent getFocusedComponent() {
    if (focused) {
      return this;
    }

    for (SkComponent child : children) {
      SkComponent focusedChild = child.getFocusedComponent();
      if (focusedChild != null) {
        return focusedChild;
      }
    }

    return null;
  }

  public SkComponent getRoot() {
    if (parent == null) {
      return this;
    }
    return parent.getRoot();
  }

  public SkComponent find(Function<SkComponent, Boolean> expression) {
    if (parent == null) {
      return null;
    }

    if (expression.apply(parent)) {
      return parent;
    }

    SkComponent current = parent;
    while (current != null) {
      if (expression.apply(current)) {
        return current;
      }
      current = current.parent;
    }
    return null;
  }

  /* Debug support */
  public void debug(Consumer<String> logger) {
    String name = this.name.isEmpty() ? this.getClass().getSimpleName() : this.name;
    String info =
        String.format(
            "Component: %s, Position: (%.1f, %.1f), Size: %.1fx%.1f, Visible: %b, Enabled: %b",
            name, x, y, width, height, visible, enabled);
    logger.accept(info);

    for (SkComponent child : children) {
      logger.accept("  └─ ");
      child.debug(msg -> logger.accept("    " + msg));
    }
  }

  /* Drag and drop events */
  protected void onDragStart(float startX, float startY) {}

  protected void onDragMove(float deltaX, float deltaY) {}

  protected void onDragEnd(float endX, float endY) {}

  /* Mouse events */
  protected void onMouseEnter() {}

  protected void onMouseExit() {}

  protected boolean onPreMouseClick(int mouseX, int mouseY, int button) {
    return false;
  }

  protected void onMouseClick(int mouseX, int mouseY, int button) {}

  protected boolean onMouseRelease(int mouseX, int mouseY, int button) {
    return false;
  }

  protected boolean onMouseScroll(int mouseX, int mouseY, int scroll) {
    return false;
  }

  /* Keyboard events */
  protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }

  protected boolean onCharTyped(char chr, int modifiers) {
    return false;
  }

  /* Focus events */
  protected void onFocusGained() {}

  protected void onFocusLost() {}

  protected void onResizeWindow() {}

  /* Other */
  protected boolean shouldForwardHover(SkComponent child, int mouseX, int mouseY) {
    return true;
  }

  protected void onHeightChange(float height) {}
}
