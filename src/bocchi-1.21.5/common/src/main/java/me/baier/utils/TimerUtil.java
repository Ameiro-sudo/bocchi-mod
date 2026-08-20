package me.baier.utils;

public class TimerUtil {

  public long lastMS = System.currentTimeMillis();

  public TimerUtil() {
    reset();
  }

  public void reset() {
    lastMS = System.currentTimeMillis();
  }

  public void resetMS() {
    // 统一使用 currentTimeMillis, 与 passedS/passedMs/getPassedTimeMs 保持一致
    this.lastMS = System.currentTimeMillis();
  }

  public boolean hasTimeElapsed(long time, boolean reset) {
    if (System.currentTimeMillis() - lastMS > time) {
      if (reset) reset();
      return true;
    }

    return false;
  }

  public boolean delay(long time) {
    return System.currentTimeMillis() - lastMS >= time;
  }

  public boolean passedS(double s) {
    return System.currentTimeMillis() - lastMS >= (long) (s * 1000.0);
  }

  public boolean passedMs(long ms) {
    return System.currentTimeMillis() - lastMS >= ms;
  }

  public boolean hasTimeElapsed(long time) {
    return System.currentTimeMillis() - lastMS > time;
  }

  public boolean hasTimeElapsed(double time) {
    return hasTimeElapsed((long) time);
  }

  public long getTime() {
    return System.currentTimeMillis() - lastMS;
  }

  public void setTime(long time) {
    lastMS = time;
  }

  public long getPassedTimeMs() {
    return System.currentTimeMillis() - lastMS;
  }
}
