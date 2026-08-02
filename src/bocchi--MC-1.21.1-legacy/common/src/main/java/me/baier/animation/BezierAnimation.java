package me.baier.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import me.baier.graphics.font.IFontRenderer;

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

    // 使用采样表进行初步估计
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
    } else { // 二分法
      float m_intervalStart = intervalStart; // a
      float m_intervalEnd = intervalStart + (1.0f / (SAMPLE_SIZE - 1)); // b
      int i = 0;
      while (true) {
        guessForT = m_intervalStart + (m_intervalEnd - m_intervalStart) / 2.0f;
        float currentX = calcBezier(guessForT, ax, bx, cx) - x;
        if (Math.abs(currentX) < SUBDIVISION_PRECISION) return guessForT;
        if (currentX > 0.0f) m_intervalEnd = guessForT;
        else m_intervalEnd = guessForT;
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

  /** 检查动画是否已完成（基于时间进度）。 */
  public boolean isComplete() {
    return progress >= 1.0f;
  }

  public boolean isCompleteWithValue() {
    return this.currentValue != null && this.currentValue.equals(targetValue) && isComplete();
  }

  /** 更新动画状态。由AnimationManager在每一帧调用。 */
  public T update() {
    if (!isAnimating || isPaused) {
      return currentValue;
    }

    long currentTime = System.currentTimeMillis();
    long elapsed = currentTime - startTime - totalPausedTime;
    this.progress = Math.max(0.0f, Math.min(1.0f, (float) elapsed / this.duration));

    float t = getTForX(this.progress); // 根据时间进度x获取曲线参数t
    float bezierProgressY = calcBezier(t, ay, by, cy); // 使用t计算缓动后的进度y
    currentValue = interpolate.interpolate(startValue, targetValue, bezierProgressY);

    if (onUpdate != null) {
      onUpdate.accept(currentValue);
    }

    // 检查并触发链式动画
    checkAndTriggerChainedAnimations();

    if (this.progress >= 1.0f) {
      isAnimating = false; // 标记动画结束
      // currentValue 应该已经通过插值到达 targetValue
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

  public static void main(String[] args) {
    Locale.setDefault(Locale.US);

    System.out.println("--- BezierAnimation Standalone Test ---");

    final List<BezierAnimation<?>> allAnimations = new ArrayList<>();

    BezierAnimation<Float> anim1 =
        BezierAnimation.createFloat(0f, 100f, 2000, BezierControlPoints.EASE_IN_OUT);
    anim1.setTag("Anim1");

    final BezierAnimation<Float> finalAnim1 = anim1;
    anim1
        .onUpdate(
            value ->
                System.out.printf(
                    "%s Update: %.2f (Progress: %.2f)\n",
                    finalAnim1.getTag(), value, finalAnim1.getProgress()))
        .onComplete(
            () ->
                System.out.printf(
                    "==> %s Complete! (Progress: %.2f) <==\n",
                    finalAnim1.getTag(), finalAnim1.getProgress()));
    allAnimations.add(anim1);

    // 动画2: 链接到 anim1, 当 anim1 进度达到 50% 时开始 (1000ms)
    BezierAnimation<Float> anim2 =
        BezierAnimation.createFloat(0f, 10f, 1000, BezierControlPoints.LINEAR);
    anim2.setTag("Anim2_ChainedTo_Anim1@0.5");
    final BezierAnimation<Float> finalAnim2 = anim2;
    anim2
        .onUpdate(
            value ->
                System.out.printf(
                    "  %s Update: %.2f (Progress: %.2f)\n",
                    finalAnim2.getTag(), value, finalAnim2.getProgress()))
        .onComplete(
            () ->
                System.out.printf(
                    "  ==> %s Complete! (Progress: %.2f) <==\n",
                    finalAnim2.getTag(), finalAnim2.getProgress()));
    allAnimations.add(anim2);

    // 动画3: 链接到 anim1, 当 anim1 完成时 (100% 进度) 开始 (1200ms)
    BezierAnimation<Float> anim3 =
        BezierAnimation.createFloat(100.f, 0.f, 1200, BezierControlPoints.EASE_OUT);
    anim3.setTag("Anim3_ChainedTo_Anim1@1.0");
    final BezierAnimation<Float> finalAnim3 = anim3;
    anim3
        .onUpdate(
            value ->
                System.out.printf(
                    "  %s Update: %.2f (Progress: %.2f)\n",
                    finalAnim3.getTag(), value, finalAnim3.getProgress()))
        .onComplete(
            () ->
                System.out.printf(
                    "  ==> %s Complete! (Progress: %.2f) <==\n",
                    finalAnim3.getTag(), finalAnim3.getProgress()));
    allAnimations.add(anim3);

    // 动画4: 链接到 anim2, 当 anim2 进度达到 60% 时开始 (800ms)
    BezierAnimation<Float> anim4 =
        BezierAnimation.createFloat(500f, 1000f, 800, BezierControlPoints.EASE_IN);
    anim4.setTag("Anim4_ChainedTo_Anim2@0.6");
    final BezierAnimation<Float> finalAnim4 = anim4;
    anim4
        .onUpdate(
            value ->
                System.out.printf(
                    "    %s Update: %.2f (Progress: %.2f)\n",
                    finalAnim4.getTag(), value, finalAnim4.getProgress()))
        .onComplete(
            () ->
                System.out.printf(
                    "    ==> %s Complete! (Progress: %.2f) <==\n",
                    finalAnim4.getTag(), finalAnim4.getProgress()));
    allAnimations.add(anim4);

    // --- 设置动画链 ---
    System.out.println("\n--- Setting up Animation Chains ---");
    anim1.then(anim2, 0.5f); // anim2 在 anim1 50% 时启动
    anim1.then(anim3, 1.0f); // anim3 在 anim1 100% 时启动
    anim2.then(anim4, 0.6f); // anim4 在 anim2 60% 时启动
    System.out.println("Chaining setup complete.");

    // --- 启动主动画 ---
    anim1.start(); // 这会将 anim1 的 isAnimating 设为 true

    // --- 手动模拟动画循环 ---
    System.out.println("\n--- Manual Update Loop (Simulating Time Progression) ---");
    long simulationLoopStartTime = System.currentTimeMillis();
    long totalSimulationDurationMs = 4000;
    long timeStepMs = 50;

    boolean anyAnimationIsStillRunningInLoop;

    do {
      long currentLoopTime = System.currentTimeMillis();
      anyAnimationIsStillRunningInLoop = false;

      System.out.printf(
          "\n--- Loop Tick @ t = %dms ---\n", (currentLoopTime - simulationLoopStartTime));

      for (BezierAnimation<?> anim : allAnimations) {
        if (anim.isAnimating() && !anim.isPaused()) {
          anim.update();
          if (anim.isAnimating()) {
            anyAnimationIsStillRunningInLoop = true;
          }
        }
      }

      // 如果所有已知的动画都已完成，但模拟时间未结束，则继续等待（以防有延迟的日志或检查点）
      if (!anyAnimationIsStillRunningInLoop
          && (currentLoopTime - simulationLoopStartTime) < totalSimulationDurationMs) {
        boolean allReallyComplete = true;
        for (BezierAnimation<?> anim : allAnimations) {
          if (anim.isAnimating() || (anim.getStartTime() > 0 && anim.getProgress() < 1.0f)) {
            allReallyComplete = false;
            anyAnimationIsStillRunningInLoop = true; // 重新标记为有动画在运行
            break;
          }
        }
        if (allReallyComplete) {
          System.out.println(
              "All known animations have completed. Loop will finish if simulation time is also up.");
        }
      }

      try {
        Thread.sleep(timeStepMs); // 暂停以模拟时间的流逝
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    } while (anyAnimationIsStillRunningInLoop
        && (System.currentTimeMillis() - simulationLoopStartTime) < totalSimulationDurationMs);

    System.out.println("\n--- Simulation Loop Ended ---");

    // 打印所有动画的最终状态
    System.out.println("\nFinal states of all animations:");
    for (BezierAnimation<?> anim : allAnimations) {
      System.out.printf(
          "Animation '%s': Progress = %.2f, IsAnimating = %b, IsPaused = %b, CurrentValue = %s\n",
          anim.getTag(),
          anim.getProgress(),
          anim.isAnimating(),
          anim.isPaused(),
          anim.getCurrentValue() != null ? anim.getCurrentValue().toString() : "null");
    }

    System.out.println(
        "\nNote: In a real scenario with AnimationManager, chained animations started via 'then'");
    System.out.println(
        "would be automatically managed and updated by the AnimationManager's loop.");
    System.out.println(
        "This test bypassed that manager's loop by manually calling update() on animations.");
  }
}
