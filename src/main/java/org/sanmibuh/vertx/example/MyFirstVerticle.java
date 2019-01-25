package org.sanmibuh.vertx.example;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyFirstVerticle extends AbstractVerticle {

  private final Map<Integer, Whisky> products = new LinkedHashMap<>();

  @Override
  public void start(final Future<Void> fut) {
    createSomeData();

    final Router router = Router.router(vertx);

    router.route("/").handler(routingContext -> {
      final HttpServerResponse response = routingContext.response();
      response
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    router.route("/assets/*").handler(StaticHandler.create("assets"));

    router.get("/api/whiskies").handler(this::getAll);
    router.route("/api/whiskies*").handler(BodyHandler.create());
    router.post("/api/whiskies").handler(this::addOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

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

  private void createSomeData() {
    final Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
    products.put(bowmore.getId(), bowmore);
    final Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
    products.put(talisker.getId(), talisker);
  }

  private void getAll(final RoutingContext routingContext) {
    routingContext.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
        .end(Json.encodePrettily(products.values()));
  }

  private void addOne(final RoutingContext routingContext) {
    final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(),
        Whisky.class);
    products.put(whisky.getId(), whisky);
    routingContext.response()
        .setStatusCode(HttpResponseStatus.CREATED.code())
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
        .end(Json.encodePrettily(whisky));
  }

  private void deleteOne(final RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
    } else {
      final Integer idAsInteger = Integer.valueOf(id);
      products.remove(idAsInteger);
    }
    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
  }
}
