package me.baier.skui;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SkReference<T> {

  private T value;

  public SkReference(T value) {
    this.value = value;
  }
}
