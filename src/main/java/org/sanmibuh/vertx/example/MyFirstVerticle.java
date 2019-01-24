package org.sanmibuh.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MyFirstVerticle extends AbstractVerticle {

  @Override
  public void start(final Future<Void> fut) {
    final int port = getPort();
    vertx
        .createHttpServer()
        .requestHandler(r -> r.response().end("<h1>Hello from my first Vert.x 3 application</h1>"))
        .listen(port, result -> {
          if (result.succeeded()) {
            fut.complete();
          } else {
            fut.fail(result.cause());
          }
        });
  }

  private int getPort() {
    return config().getInteger(Config.HTTP_PORT_KEY, Config.DEFAULT_HTTP_PORT);
  }
}
