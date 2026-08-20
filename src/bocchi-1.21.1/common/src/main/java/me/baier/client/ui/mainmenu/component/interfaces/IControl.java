package me.baier.client.ui.mainmenu.component.interfaces;

public interface IControl {

  default void mouseClicked(int mouseX, int mouseY, int mouseButton) {}

  default void mouseReleased(int mouseX, int mouseY, int state) {}

  default void mouseClickMove(
      int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {}

  default void keyTyped(char typedChar, int keyCode) {}

  default void keyPressed(int keyCode, int scanCode, int modifiers) {}

  default void mouseScrolled(int mouseX, int mouseY, int scroll) {}

  default void mouseMove(int mouseX, int mouseY, int scroll) {}
}
