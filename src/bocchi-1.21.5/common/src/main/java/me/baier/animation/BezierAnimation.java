package me.baier.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;

public class BezierAnimation<T> {
  private T startValue;
  @Getter private T targetValue;
  @Getter private T currentValue;
  @Getter @Setter private String tag = "";
  private final long duration;
  private final BezierControlPoints controlPoints;
  private final Interpolator<T> interpolate;
  @Getter private long startTime;
  @Getter private boolean isAnimating;
  @Getter private boolean isPaused;
  private long pauseStartTime;
  private long totalPausedTime;

  private final float cx, bx, ax;
  private final float cy, by, ay;

  private static final int SAMPLE_SIZE = 11;
  private static final float NEWTON_MIN_SLOPE = 0.001f;
  private static final float NEWTON_ITERATIONS = 8f;
  private static final float SUBDIVISION_PRECISION = 0.0000001f;
  private static final int SUBDIVISION_MAX_ITERATIONS = 10;
  private static final Map<BezierControlPoints, float[]> CACHED_SAMPLE_TABLES =
      new ConcurrentHashMap<>();
  private final float[] sampleTable;

  @Getter private Consumer<T> onUpdate;
  @Getter private Runnable onComplete;

  @Getter private float progress;

  public void setTargetValue(T targetValue) {
    this.targetValue = targetValue;
    if (!this.isAnimating || this.isComplete()) {
      this.currentValue = targetValue;
    }
  }

  private static class ChainedAnimationEntry<T> {
    BezierAnimation<T> animation;
    float triggerProgress;
    boolean triggered;

    ChainedAnimationEntry(BezierAnimation<T> animation, float triggerProgress) {
      this.animation = animation;
      this.triggerProgress = triggerProgress;
      this.triggered = false;
    }
  }

  private List<ChainedAnimationEntry<T>> chainedAnimations = new ArrayList<>();

  public static BezierAnimation<Float> createFloat(
      float initialValue, long duration, BezierControlPoints controlPoints) {
    return createFloat(initialValue, initialValue, duration, controlPoints);
  }

  public static BezierAnimation<Float> createFloat(
      float initialValue, float targetValue, long duration, BezierControlPoints controlPoints) {
    return new BezierAnimation<>(
        initialValue,
        targetValue,
        duration,
        controlPoints,
        (start, end, progressValue) -> start + (end - start) * progressValue);
  }

  public static BezierAnimation<Float> createFloat(
      float initialValue, float targetValue, long duration) {
    return createFloat(initialValue, targetValue, duration, BezierControlPoints.LINEAR);
  }

  public static BezierAnimation<Integer> createInt(
      int initialValue, long duration, BezierControlPoints controlPoints) {
    return createInt(initialValue, initialValue, duration, controlPoints);
  }

  public static BezierAnimation<Integer> createInt(
      int initialValue, int targetValue, long duration, BezierControlPoints controlPoints) {
    return new BezierAnimation<>(
        initialValue,
        targetValue,
        duration,
        controlPoints,
        (start, end, progressValue) -> (int) (start + (end - start) * progressValue));
  }

  public static BezierAnimation<Integer> createInt(int initialValue, long duration) {
    return createInt(initialValue, duration, BezierControlPoints.LINEAR);
  }

  public static BezierAnimation<Integer> createInt(int initialValue) {
    return createInt(initialValue, 300);
  }

  public static BezierAnimation<Float> createFEmpty() {
    return createFloat(0, 0, 0, BezierControlPoints.LINEAR);
  }

  public static BezierAnimation<Integer> createIEmpty() {
    return createInt(0, 0, 0, BezierControlPoints.LINEAR);
  }

  public BezierAnimation(
      T initialValue,
      T targetValue,
      long duration,
      BezierControlPoints controlPoints,
      Interpolator<T> interpolate) {
    this.startValue = initialValue;
    this.targetValue = targetValue;
    this.currentValue = initialValue;
    this.duration = duration <= 0 ? 1 : duration;
    this.controlPoints = controlPoints;
    this.interpolate = interpolate;

    // x(t) = ax*t^3 + bx*t^2 + cx*t
    // y(t) = ay*t^3 + by*t^2 + cy*t
    float p1x = controlPoints.getP1x();
    float p1y = controlPoints.getP1y();
    float p2x = controlPoints.getP2x();
    float p2y = controlPoints.getP2y();

    this.cx = 3.0f * p1x;
    this.bx = 3.0f * (p2x - p1x) - this.cx;
    this.ax = 1.0f - this.cx - this.bx;

    this.cy = 3.0f * p1y;
    this.by = 3.0f * (p2y - p1y) - this.cy;
    this.ay = 1.0f - this.cy - this.by;

    this.startTime = 0;
    this.isAnimating = false;
    this.isPaused = false;
    this.totalPausedTime = 0;
    this.progress = 0.0f;

    this.sampleTable = getSampleTableForControlPoints(controlPoints);
  }

  private static synchronized float[] getSampleTableForControlPoints(BezierControlPoints cp) {
    if (cp.equals(BezierControlPoints.LINEAR)) {
      return null;
    }
    return CACHED_SAMPLE_TABLES.computeIfAbsent(
        cp,
        controlPoints -> {
          float[] table = new float[SAMPLE_SIZE];
          float p1x = controlPoints.getP1x();
          float p2x = controlPoints.getP2x();
          float currentCx = 3.0f * p1x;
          float currentBx = 3.0f * (p2x - p1x) - currentCx;
          float currentAx = 1.0f - currentCx - currentBx;

          for (int i = 0; i < SAMPLE_SIZE; ++i) {
            table[i] = calcBezier(i * (1.0f / (SAMPLE_SIZE - 1)), currentAx, currentBx, currentCx);
          }
          return table;
        });
  }

  private static float calcBezier(float t, float a, float b, float c) {
    return ((a * t + b) * t + c) * t;
  }

  private static float getSlope(float t, float a, float b, float c) {
    return 3.0f * a * t * t + 2.0f * b * t + c;
  }

  private float getTForX(float x) {
    if (x <= 0.0f) return 0.0f;
    if (x >= 1.0f) return 1.0f;
    if (this.controlPoints.equals(BezierControlPoints.LINEAR) || sampleTable == null) {
      return x;
    }

    // 浣跨敤閲囨牱琛ㄨ繘琛屽垵姝ヤ及璁?
    float intervalStart = 0.0f;
    int currentSample = 1;
    int lastSample = SAMPLE_SIZE - 1;

    while (currentSample != lastSample && sampleTable[currentSample] <= x) {
      intervalStart += (1.0f / (SAMPLE_SIZE - 1));
      ++currentSample;
    }
    --currentSample;

    float dist =
        (x - sampleTable[currentSample])
            / (sampleTable[currentSample + 1] - sampleTable[currentSample]);
    float guessForT = intervalStart + dist * (1.0f / (SAMPLE_SIZE - 1));
    float initialSlope = getSlope(guessForT, ax, bx, cx);

    if (initialSlope >= NEWTON_MIN_SLOPE) {
      for (int i = 0; i < NEWTON_ITERATIONS; ++i) {
        float currentX = calcBezier(guessForT, ax, bx, cx) - x;
        if (Math.abs(currentX) < SUBDIVISION_PRECISION) return guessForT;
        float currentSlope = getSlope(guessForT, ax, bx, cx);
        if (Math.abs(currentSlope) < NEWTON_MIN_SLOPE) break;
        guessForT -= currentX / currentSlope;
      }
      return guessForT;
    } else if (initialSlope == 0.0f) {
      return guessForT;
    } else { // 浜屽垎娉?
      float m_intervalStart = intervalStart; // a
      float m_intervalEnd = intervalStart + (1.0f / (SAMPLE_SIZE - 1)); // b
      int i = 0;
      while (true) {
        guessForT = m_intervalStart + (m_intervalEnd - m_intervalStart) / 2.0f;
        float currentX = calcBezier(guessForT, ax, bx, cx) - x;
        if (Math.abs(currentX) < SUBDIVISION_PRECISION) return guessForT;
        if (currentX > 0.0f) m_intervalEnd = guessForT;
        else m_intervalStart = guessForT; // 鍑芥暟鍊艰繃灏忔椂鏍瑰湪鍙冲崐鍖洪棿, 鏀剁缉宸︾鐐?
        ++i;
        if (i == SUBDIVISION_MAX_ITERATIONS) return guessForT;
      }
    }
  }

  public BezierAnimation(T initialValue, Interpolator<T> interpolate) {
    this(initialValue, initialValue, 300, BezierControlPoints.EASE_IN_OUT, interpolate);
  }

  public BezierAnimation<T> onUpdate(Consumer<T> callback) {
    this.onUpdate = callback;
    return this;
  }

  public BezierAnimation<T> onComplete(Runnable callback) {
    this.onComplete = callback;
    return this;
  }

  public BezierAnimation<T> start() {
    this.currentValue = this.startValue;
    this.startTime = System.currentTimeMillis();
    this.totalPausedTime = 0;
    this.isAnimating = true;
    this.isPaused = false;
    for (ChainedAnimationEntry<T> entry : chainedAnimations) {
      entry.triggered = false;
    }
    return this;
  }

  public void reset(T initialValue, T value) {
    startValue = initialValue;
    targetValue = value;
    currentValue = initialValue;
    this.progress = 0.0f;
    this.startTime = System.currentTimeMillis();
    this.totalPausedTime = 0;
    this.isAnimating = true;
    this.isPaused = false;
    for (ChainedAnimationEntry<T> entry : chainedAnimations) {
      entry.triggered = false;
    }
  }

  public void set(T value) {
    if (isAnimating && !isPaused) {
      update();
    }
    this.startValue = this.currentValue;
    this.targetValue = value;

    this.progress = 0.0f;
    this.startTime = System.currentTimeMillis();
    this.isAnimating = true;
    this.isPaused = false;
    this.totalPausedTime = 0;
    for (ChainedAnimationEntry<T> entry : chainedAnimations) {
      entry.triggered = false;
    }
  }

  public void pause() {
    if (isAnimating && !isPaused) {
      isPaused = true;
      pauseStartTime = System.currentTimeMillis();
    }
  }

  public void resume() {
    if (isAnimating && isPaused) {
      isPaused = false;
      totalPausedTime += System.currentTimeMillis() - pauseStartTime;
    }
  }

  public void stop() {
    isAnimating = false;
    isPaused = false;
    progress = 1.0f;

    float t = getTForX(progress);
    float bezierProgressY = calcBezier(t, ay, by, cy);
    currentValue = interpolate.interpolate(startValue, targetValue, bezierProgressY);

    if (onUpdate != null) {
      onUpdate.accept(currentValue);
    }

    checkAndTriggerChainedAnimations();
    if (onComplete != null) {
      onComplete.run();
    }
  }

  public T interpolate(T start, T end, float t) {
    return interpolate.interpolate(start, end, t);
  }

  /** 妫€鏌ュ姩鐢绘槸鍚﹀凡瀹屾垚锛堝熀浜庢椂闂磋繘搴︼級銆?*/
  public boolean isComplete() {
    return progress >= 1.0f;
  }

  public boolean isCompleteWithValue() {
    return this.currentValue != null && this.currentValue.equals(targetValue) && isComplete();
  }

  /** 鏇存柊鍔ㄧ敾鐘舵€併€傜敱AnimationManager鍦ㄦ瘡涓€甯ц皟鐢ㄣ€?*/
  public T update() {
    if (!isAnimating || isPaused) {
      return currentValue;
    }

    long currentTime = System.currentTimeMillis();
    long elapsed = currentTime - startTime - totalPausedTime;
    this.progress = Math.max(0.0f, Math.min(1.0f, (float) elapsed / this.duration));

    float t = getTForX(this.progress); // 鏍规嵁鏃堕棿杩涘害x鑾峰彇鏇茬嚎鍙傛暟t
    float bezierProgressY = calcBezier(t, ay, by, cy); // 浣跨敤t璁＄畻缂撳姩鍚庣殑杩涘害y
    currentValue = interpolate.interpolate(startValue, targetValue, bezierProgressY);

    if (onUpdate != null) {
      onUpdate.accept(currentValue);
    }

    // 妫€鏌ュ苟瑙﹀彂閾惧紡鍔ㄧ敾
    checkAndTriggerChainedAnimations();

    if (this.progress >= 1.0f) {
      isAnimating = false; // 鏍囪鍔ㄧ敾缁撴潫
      // currentValue 搴旇宸茬粡閫氳繃鎻掑€煎埌杈?targetValue
      if (onComplete != null) {
        onComplete.run();
      }
    }
    return currentValue;
  }

  private void checkAndTriggerChainedAnimations() {
    for (ChainedAnimationEntry<T> entry : chainedAnimations) {
      if (!entry.triggered && this.progress >= entry.triggerProgress) {
        entry.animation.start();
        entry.triggered = true;
      }
    }
  }

  public BezierAnimation<T> then(BezierAnimation<T> next) {
    return then(next, 1.0f);
  }

  public BezierAnimation<T> then(BezierAnimation<T> next, float triggerProgress) {
    if (next == null) {
      throw new IllegalArgumentException("Next animation cannot be null.");
    }
    float clampedProgress = Math.max(0.0f, Math.min(1.0f, triggerProgress));
    this.chainedAnimations.add(new ChainedAnimationEntry<>(next, clampedProgress));
    return next;
  }

  public BezierAnimation<T> then(
      T initialValue,
      T targetValue,
      long duration,
      BezierControlPoints controlPoints,
      float triggerProgress) {
    BezierAnimation<T> next =
        new BezierAnimation<>(initialValue, targetValue, duration, controlPoints, this.interpolate);
    return this.then(next, triggerProgress);
  }

  public BezierAnimation<T> then(
      T initialValue, T targetValue, long duration, BezierControlPoints controlPoints) {
    return then(initialValue, targetValue, duration, controlPoints, 1.0f);
  }

  public BezierAnimation<T> then(
      T initialValue, T targetValue, long duration, float triggerProgress) {
    return then(
        initialValue, targetValue, duration, BezierControlPoints.EASE_IN_OUT, triggerProgress);
  }

  public BezierAnimation<T> then(T initialValue, T targetValue, long duration) {
    return then(initialValue, targetValue, duration, BezierControlPoints.EASE_IN_OUT, 1.0f);
  }
}
