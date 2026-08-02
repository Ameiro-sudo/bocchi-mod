package me.baier.graphics.media;

import io.github.humbleui.skija.*;
import io.github.humbleui.skija.ColorAlphaType; // Correct import for AlphaType
import io.github.humbleui.skija.Image;
import me.baier.utils.TimerUtil;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class SKVideoDecoder implements AutoCloseable {

  static {
    avutil.av_log_set_level(avutil.AV_LOG_ERROR);
  }

  private FFmpegFrameGrabber grabber;
  private Image currentFrame; // Needs closing
  private Bitmap bitmap; // Reused across frames to avoid per-frame native allocations
  private final TimerUtil timer = new TimerUtil();

  private void internalReset() throws FrameGrabber.Exception {
    if (grabber != null) {
      grabber.stop();
      grabber.close();
      grabber = null;
    }
    if (currentFrame != null) {
      currentFrame.close();
      currentFrame = null;
    }
    if (bitmap != null) {
      bitmap.close();
      bitmap = null;
    }
  }

  public void loadUrl(String url) throws FrameGrabber.Exception {
    if (grabber != null) {
      this.internalReset();
    }

    grabber = new FFmpegFrameGrabber(url);
    grabber.setImageWidth(960);
    grabber.setImageHeight(540);
    grabber.setPixelFormat(avutil.AV_PIX_FMT_RGBA);
    grabber.start();
    timer.reset();
  }

  public Image computeFrame() throws IOException {

    if (currentFrame != null && !timer.hasTimeElapsed((long) (1000 / grabber.getFrameRate()))) {
      return currentFrame;
    }
    if (grabber.getFrameNumber() + 1 >= grabber.getLengthInFrames()) {
      grabber.setFrameNumber(0);
    }
    timer.reset();

    Frame frame = grabber.grabImage();

    if (frame == null) {

      if (currentFrame != null) {
        currentFrame.close();
        currentFrame = null;
      }
      return null;
    }

    int width = frame.imageWidth;
    int height = frame.imageHeight;

    if (bitmap == null
        || bitmap.getWidth() != width
        || bitmap.getHeight() != height
        || bitmap.getRowBytes() != (long) width * 4) {
      if (bitmap != null) {
        bitmap.close();
      }
      bitmap = new Bitmap();
      ImageInfo imageInfo =
          new ImageInfo(
              width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE, ColorSpace.getSRGB());

      if (!bitmap.allocPixels(imageInfo)) {
        bitmap.close();
        bitmap = null;
        throw new IOException("Failed to allocate pixels for Skija Bitmap.");
      }
    }

    ByteBuffer src = (ByteBuffer) frame.image[0].position(0);
    ByteBuffer dst = Objects.requireNonNull(bitmap.peekPixels());
    int srcStride = frame.imageStride;
    int rowBytes = width * 4;
    dst.clear();

    for (int y = 0; y < height; y++) {
      int rowOffset = y * srcStride;
      src.position(rowOffset);
      src.limit(rowOffset + rowBytes);
      dst.put(src);
    }

    Image newSkijaImage = Image.makeRasterFromBitmap(bitmap);

    if (currentFrame != null) {
      currentFrame.close();
    }
    currentFrame = newSkijaImage;
    return currentFrame;
  }

  @Override
  public void close() throws FrameGrabber.Exception {
    internalReset();
  }
}
