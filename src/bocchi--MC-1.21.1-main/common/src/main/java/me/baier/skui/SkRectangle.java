package me.baier.skui;

import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import lombok.Getter;
import lombok.Setter;

/**
 * A rectangle representation for the SkUI framework. Provides functionality for rectangle
 * operations such as contains, intersects, union, and more.
 */
@Setter
@Getter
public class SkRectangle {
  private float x;

  private float y;

  private float width;

  private float height;

  /** Creates an empty rectangle (0,0,0,0) */
  public SkRectangle() {
    this(0, 0, 0, 0);
  }

  /**
   * Creates a rectangle with the specified position and size
   *
   * @param x The x coordinate of the top-left corner
   * @param y The y coordinate of the top-left corner
   * @param width The width of the rectangle
   * @param height The height of the rectangle
   */
  public SkRectangle(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Creates a copy of another rectangle
   *
   * @param other The rectangle to copy
   */
  public SkRectangle(SkRectangle other) {
    this(other.x, other.y, other.width, other.height);
  }

  /**
   * Gets the left edge of the rectangle
   *
   * @return The x coordinate of the left edge
   */
  public float getLeft() {
    return x;
  }

  /**
   * Gets the top edge of the rectangle
   *
   * @return The y coordinate of the top edge
   */
  public float getTop() {
    return y;
  }

  /**
   * Gets the right edge of the rectangle
   *
   * @return The x coordinate of the right edge
   */
  public float getRight() {
    return x + width;
  }

  /**
   * Gets the bottom edge of the rectangle
   *
   * @return The y coordinate of the bottom edge
   */
  public float getBottom() {
    return y + height;
  }

  /**
   * Sets the position of this rectangle
   *
   * @param x The new x coordinate
   * @param y The new y coordinate
   */
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Sets the size of this rectangle
   *
   * @param width The new width
   * @param height The new height
   */
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
  }

  /**
   * Sets the bounds of this rectangle
   *
   * @param x The new x coordinate
   * @param y The new y coordinate
   * @param width The new width
   * @param height The new height
   */
  public void setBounds(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Sets this rectangle to match another rectangle
   *
   * @param rect The rectangle to copy bounds from
   */
  public void setBounds(SkRectangle rect) {
    setBounds(rect.x, rect.y, rect.width, rect.height);
  }

  /**
   * Checks if this rectangle is empty (has no area)
   *
   * @return true if the rectangle has zero width or height
   */
  public boolean isEmpty() {
    return width <= 0 || height <= 0;
  }

  /**
   * Checks if a point is contained within this rectangle
   *
   * @param pointX The x coordinate of the point
   * @param pointY The y coordinate of the point
   * @return true if the point is inside this rectangle
   */
  public boolean contains(float pointX, float pointY) {
    return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
  }

  /**
   * Checks if a rectangle is completely contained within this rectangle
   *
   * @param rect The rectangle to check
   * @return true if the specified rectangle is inside this rectangle
   */
  public boolean contains(SkRectangle rect) {
    return rect.x >= x
        && rect.x + rect.width <= x + width
        && rect.y >= y
        && rect.y + rect.height <= y + height;
  }

  /**
   * Checks if this rectangle intersects with another rectangle
   *
   * @param rect The rectangle to check for intersection
   * @return true if the rectangles intersect
   */
  public boolean intersects(SkRectangle rect) {
    return rect.x + rect.width > x
        && rect.x < x + width
        && rect.y + rect.height > y
        && rect.y < y + height;
  }

  /**
   * Computes the intersection of this rectangle with another rectangle
   *
   * @param rect The rectangle to intersect with
   * @return A new rectangle representing the intersection
   */
  public SkRectangle intersection(SkRectangle rect) {
    float ix = Math.max(this.x, rect.x);
    float iy = Math.max(this.y, rect.y);
    float iw = Math.min(this.x + this.width, rect.x + rect.width) - ix;
    float ih = Math.min(this.y + this.height, rect.y + rect.height) - iy;

    if (iw <= 0 || ih <= 0) {
      return new SkRectangle(0, 0, 0, 0); // No intersection
    }

    return new SkRectangle(ix, iy, iw, ih);
  }

  /**
   * Computes the union of this rectangle with another rectangle
   *
   * @param rect The rectangle to union with
   * @return A new rectangle representing the union
   */
  public SkRectangle union(SkRectangle rect) {
    if (rect.isEmpty()) {
      return new SkRectangle(this);
    }

    if (this.isEmpty()) {
      return new SkRectangle(rect);
    }

    float ux = Math.min(this.x, rect.x);
    float uy = Math.min(this.y, rect.y);
    float uw = Math.max(this.x + this.width, rect.x + rect.width) - ux;
    float uh = Math.max(this.y + this.height, rect.y + rect.height) - uy;

    return new SkRectangle(ux, uy, uw, uh);
  }

  /**
   * Grows this rectangle by the specified amount in all directions
   *
   * @param amount The amount to grow by
   * @return A new expanded rectangle
   */
  public SkRectangle grow(float amount) {
    return new SkRectangle(x - amount, y - amount, width + (amount * 2), height + (amount * 2));
  }

  /**
   * Shrinks this rectangle by the specified amount in all directions
   *
   * @param amount The amount to shrink by
   * @return A new shrunk rectangle
   */
  public SkRectangle shrink(float amount) {
    return grow(-amount);
  }

  /**
   * Translates this rectangle by the specified amounts
   *
   * @param dx The x translation amount
   * @param dy The y translation amount
   * @return A new translated rectangle
   */
  public SkRectangle translate(float dx, float dy) {
    return new SkRectangle(x + dx, y + dy, width, height);
  }

  /**
   * Creates a new rectangle with the same dimensions but centered at the origin
   *
   * @return A new centered rectangle
   */
  public SkRectangle centered() {
    return new SkRectangle(-width / 2, -height / 2, width, height);
  }

  public void inflate(float dx, float dy) {
    x -= dx;
    y -= dy;
    width += dx * 2;
    height += dy * 2;
  }

  public Rect toSkiaRect() {
    return Rect.makeXYWH(x, y, width, height);
  }

  public RRect toSkiaRRect(float radius) {
    return RRect.makeXYWH(x, y, width, height, radius);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    SkRectangle other = (SkRectangle) obj;
    return Float.compare(other.x, x) == 0
        && Float.compare(other.y, y) == 0
        && Float.compare(other.width, width) == 0
        && Float.compare(other.height, height) == 0;
  }

  @Override
  public int hashCode() {
    int result = Float.floatToIntBits(x);
    result = 31 * result + Float.floatToIntBits(y);
    result = 31 * result + Float.floatToIntBits(width);
    result = 31 * result + Float.floatToIntBits(height);
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "SkRectangle[x=%.1f, y=%.1f, width=%.1f, height=%.1f]", x, y, width, height);
  }
}
