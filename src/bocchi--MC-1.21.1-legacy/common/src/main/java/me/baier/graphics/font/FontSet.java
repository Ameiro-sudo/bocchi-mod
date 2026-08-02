package me.baier.graphics.font;

import java.util.ArrayList;
import java.util.List;

/**
 * @author AquaVase Created on 7/10/2024
 */
public interface FontSet {
  List<String> FONTS = new ArrayList<>();
  SkiaFont KRANKY = new SkiaFont("Kranky-Regular");
  SkiaFont RADIKAL_BLACK = new SkiaFont("Radikal-Black");
  SkiaFont RADIKAL_REGULAR = new SkiaFont("Radikal-Regular");
  SkiaFont RADIKAL_THIN = new SkiaFont("radikal-thin");
  SkiaFont MEIRYO_BOLD = new SkiaFont("meiryo-bold");
  SkiaFont SH_LIGHT = new SkiaFont("SourceHanSansSC-Light");
  SkiaFont SH_REGULAR = new SkiaFont("SourceHanSansSC-Regular");
  SkiaFont SH_HEAVY = new SkiaFont("SourceHanSansSC-Heavy");
  SkiaFont SH_NORMAL = new SkiaFont("SourceHanSansSC-Normal");
  SkiaFont SH_BOLD = new SkiaFont("SourceHanSansSC-Bold");
  /*SkiaFont GENERAL = new SkiaFont("misans");
  SkiaFont CLICKGUI_ICON = new SkiaFont("clickgui-icon");*/
}
