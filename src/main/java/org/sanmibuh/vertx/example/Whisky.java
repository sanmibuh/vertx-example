package org.sanmibuh.vertx.example;

import io.vertx.core.json.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Whisky {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  private final int id;

  private String name;

  private String origin;

  public Whisky() {
    this.id = COUNTER.getAndIncrement();
  }

  public Whisky(final String name, final String origin) {
    this();
    this.name = name;
    this.origin = origin;
  }

  public Whisky(final JsonObject json) {
    this(json.getInteger("ID"), json.getString("NAME"), json.getString("ORIGIN"));
  }

}
