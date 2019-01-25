package org.sanmibuh.vertx.example;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

@Data
public class Whisky {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  private final int id;

  private String name;

  private String origin;

  public Whisky(final String name, final String origin) {
    this();
    this.name = name;
    this.origin = origin;
  }

  public Whisky() {
    this.id = COUNTER.getAndIncrement();
  }

}
