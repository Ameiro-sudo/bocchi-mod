package me.baier.client.ui.api;

public class BreathingUtil {

  private long startTimeMillis;
  private final long durationMillis;
  private final float minValue;
  private final float maxValue;

  public BreathingUtil(long durationMillis, float minValue, float maxValue) {
    if (durationMillis <= 0) {
      throw new IllegalArgumentException("Duration must be positive.");
    }
    if (minValue > maxValue) {
      float temp = minValue;
      minValue = maxValue;
      maxValue = temp;
      System.err.println("Warning: minValue was greater than maxValue. They have been swapped.");
    }

    this.durationMillis = durationMillis;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.startTimeMillis = System.currentTimeMillis();
  }

  public void reset() {
    this.startTimeMillis = System.currentTimeMillis();
  }

  public float getCurrentValue() {
    long currentTimeMillis = System.currentTimeMillis();
    long elapsedTime = currentTimeMillis - this.startTimeMillis;

    if (this.minValue == this.maxValue) {
      return this.minValue;
    }

    float cyclePosition = (float) (elapsedTime % this.durationMillis) / this.durationMillis;

    float angle = (float) (cyclePosition * 2.f * Math.PI);
    float normalizedValue = (float) ((-Math.cos(angle) + 1.f) / 2.f);

    float currentValue = this.minValue + normalizedValue * (this.maxValue - this.minValue);

    return currentValue;
  }
}
