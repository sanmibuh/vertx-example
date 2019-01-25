package org.sanmibuh.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class MyFirstVerticle extends AbstractVerticle {

  @Override
  public void start(final Future<Void> fut) {
    final Router router = Router.router(vertx);

    router.route("/").handler(routingContext -> {
      final HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(
            getPort(),
            result -> {
              if (result.succeeded()) {
                fut.complete();
              } else {
                fut.fail(result.cause());
              }
            }
        );
  }

  private int getPort() {
    return config().getInteger(Config.HTTP_PORT_KEY, Config.DEFAULT_HTTP_PORT);
  }
}
