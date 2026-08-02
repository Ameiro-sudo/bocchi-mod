package me.baier.utils;

import io.github.humbleui.types.Point;

public class ScreenUtils {
  public static Point calculateCenterPosition(Point p1, Point p2) {
    return new Point((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
  }

  public static Point calculateStartPosition(
      float finalX,
      float finalY,
      float elWidth,
      float elHeight,
      float screenW,
      float screenH,
      float offset) {
    float startX = finalX;
    float startY = finalY;
    float distToLeft = finalX;
    float distToRight = screenW - (finalX + elWidth);
    float distToTop = finalY;
    float distToBottom = screenH - (finalY + elHeight);
    float minDist = distToLeft;
    int nearestEdge = 0;
    if (distToRight < minDist) {
      minDist = distToRight;
      nearestEdge = 1;
    }
    if (distToTop < minDist) {
      minDist = distToTop;
      nearestEdge = 2;
    }
    if (distToBottom < minDist) {
      nearestEdge = 3;
    }
    switch (nearestEdge) {
      case 0:
        startX = -elWidth - offset;
        break;
      case 1:
        startX = screenW + offset;
        break;
      case 2:
        startY = -elHeight - offset;
        break;
      case 3:
        startY = screenH + offset;
        break;
    }
    return new Point(startX, startY);
  }
}
