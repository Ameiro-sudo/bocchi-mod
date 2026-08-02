package me.baier.graphics.font;

/**
 * @author ByteBreaker create 31/03/2024
 */
public interface IFontRenderer {
  default void drawCenteredStringWithFade(String text, float x, float y, boolean shadow) {
    drawStringWithFade(text, x - this.getStringWidth(text) / 2f, y, 1, shadow);
  }

  default void drawStringWithFade(String text, float x, float y, boolean shadow) {
    drawStringWithFade(text, x, y, 1, shadow);
  }

  default void drawStringWithFade(String text, float x, float y, float alpha, boolean shadow) {
    if (shadow) drawStringWithFade(text, ((double) x + .5f), ((double) y + .5f), alpha, true);
    drawStringWithFade(text, ((double) x), ((double) y), alpha, false);
  }

  void drawStringWithFade(String text, double x, double y, float alpha, boolean shadow);

  default int drawCenteredString(String name, float x, float y, int color) {
    return drawString(name, x - (getStringWidth(name) / 2f), y, color);
  }

  default void drawCenteredStringWithShadow(String text, float x, float y, int color) {
    this.drawStringWithShadow(text, x - (this.getStringWidth(text) / 2f), y, color);
  }

  int drawStringWithShadow(String name, float x, float y, int color);

  int drawString(String text, float x, float y, int color);

  float getHeight();

  float getStringWidth(String text);

  default float getHalfHeight() {
    return getHeight() / 2f;
  }
}
