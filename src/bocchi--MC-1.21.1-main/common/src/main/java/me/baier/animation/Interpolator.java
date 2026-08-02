package me.baier.animation;

@FunctionalInterface
public interface Interpolator<T> {
  T interpolate(T start, T end, float progress);
}
