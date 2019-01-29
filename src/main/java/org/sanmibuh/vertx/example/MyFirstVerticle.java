package org.sanmibuh.vertx.example;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.List;
import java.util.stream.Collectors;

public class MyFirstVerticle extends AbstractVerticle {

  private JDBCClient jdbc;

  @Override
  public void start(final Future<Void> fut) {
    final JsonObject config = config();
    jdbc = JDBCClient.createShared(vertx, config, config.getString(Config.DS_NAME));

    startBackend(
        connection -> createSomeData(connection,
            nothing -> startWebApp(http -> completeStartup(http, fut)), fut),
        fut);
  }

  private void startBackend(final Handler<AsyncResult<SQLConnection>> next, final Future<Void> fut) {
    jdbc.getConnection(ar -> {
      if (ar.failed()) {
        fut.fail(ar.cause());
      } else {
        next.handle(Future.succeededFuture(ar.result()));
      }
    });
  }

  private void createSomeData(final AsyncResult<SQLConnection> result, final Handler<AsyncResult<Void>> next,
      final Future<Void> fut) {

    if (result.failed()) {
      fut.fail(result.cause());
    } else {
      final SQLConnection connection = result.result();
      connection.execute(
          "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), origin varchar(100))",
          ar -> {
            if (ar.failed()) {
              fut.fail(ar.cause());
              connection.close();
              return;
            }
            connection.query("SELECT * FROM Whisky", select -> {
              if (select.failed()) {
                fut.fail(ar.cause());
                connection.close();
                return;
              }
              if (select.result().getNumRows() == 0) {
                insert(
                    new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"),
                    connection,
                    v -> insert(new Whisky("Talisker 57° North", "Scotland, Island"),
                        connection,
                        r -> {
                          next.handle(Future.succeededFuture());
                          connection.close();
                        }));
              } else {
                next.handle(Future.succeededFuture());
                connection.close();
              }
            });
          });
    }
  }

  private void startWebApp(final Handler<AsyncResult<HttpServer>> next) {

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
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(getPort(), next);
  }

  private void completeStartup(final AsyncResult<HttpServer> http, final Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
    } else {
      fut.fail(http.cause());
    }
  }

  private int getPort() {
    return config().getInteger(Config.HTTP_PORT_KEY, Config.DEFAULT_HTTP_PORT);
  }

  @Override
  public void stop() {
    jdbc.close();
  }

  private void insert(final Whisky whisky, final SQLConnection connection, final Handler<AsyncResult<Whisky>> next) {
    final String sql = "INSERT INTO Whisky (name, origin) VALUES ?, ?";
    connection.updateWithParams(sql,
        new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
        ar -> {
          if (ar.failed()) {
            next.handle(Future.failedFuture(ar.cause()));
            return;
          }
          final UpdateResult result = ar.result();
          final Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
          next.handle(Future.succeededFuture(w));
        });
  }

  private void select(final String id, final SQLConnection connection, final Handler<AsyncResult<Whisky>> resultHandler) {
    connection.queryWithParams("SELECT * FROM Whisky WHERE id=?", new JsonArray().add(id), ar -> {
      if (ar.failed()) {
        resultHandler.handle(Future.failedFuture("Whisky not found"));
      } else {
        if (ar.result().getNumRows() >= 1) {
          resultHandler.handle(Future.succeededFuture(new Whisky(ar.result().getRows().get(0))));
        } else {
          resultHandler.handle(Future.failedFuture("Whisky not found"));
        }
      }
    });
  }

  private void update(final String id, final JsonObject content, final SQLConnection connection,
      final Handler<AsyncResult<Whisky>> resultHandler) {
    final String sql = "UPDATE Whisky SET name=?, origin=? WHERE id=?";
    connection.updateWithParams(sql,
        new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id),
        update -> {
          if (update.failed()) {
            resultHandler.handle(Future.failedFuture("Cannot update the whisky"));
            return;
          }
          if (update.result().getUpdated() == 0) {
            resultHandler.handle(Future.failedFuture("Whisky not found"));
            return;
          }
          resultHandler.handle(
              Future.succeededFuture(new Whisky(Integer.valueOf(id),
                  content.getString("name"), content.getString("origin"))));
        });
  }

  private void delete(final String id, final SQLConnection connection, final Handler<AsyncResult<Whisky>> resultHandler) {
    connection.queryWithParams("DELETE FROM Whisky WHERE id=?", new JsonArray().add(id),
        ar -> resultHandler.handle(Future.succeededFuture()));
  }

  private void getAll(final RoutingContext routingContext) {
    jdbc.getConnection(ar -> {
      final SQLConnection connection = ar.result();
      connection.query("SELECT * FROM Whisky", result -> {
        final List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
            .end(Json.encodePrettily(whiskies));
        connection.close(); // Close the connection
      });
    });
  }

  private void addOne(final RoutingContext routingContext) {
    jdbc.getConnection(ar -> {
      final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
      final SQLConnection connection = ar.result();
      insert(whisky, connection, r ->
          routingContext.response()
              .setStatusCode(HttpResponseStatus.CREATED.code())
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(r.result())));
      connection.close();
    });
  }

  private void deleteOne(final RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
    } else {
      jdbc.getConnection(ar -> {
        final SQLConnection connection = ar.result();
        delete(id, connection, result -> {
          routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
          connection.close();
        });
      });
    }
  }

  private void getOne(final RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
    } else {
      jdbc.getConnection(ar -> {
        // Read the request's content and create an instance of Whisky.
        final SQLConnection connection = ar.result();
        select(id, connection, result -> {
          if (result.succeeded()) {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.OK.code())
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .end(Json.encodePrettily(result.result()));
          } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
          }
          connection.close();
        });
      });
    }

  }

  private void updateOne(final RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    final JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
    } else {
      jdbc.getConnection(ar ->
          update(id, json, ar.result(), whisky -> {
            if (whisky.failed()) {
              routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            } else {
              routingContext.response()
                  .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                  .end(Json.encodePrettily(whisky.result()));
            }
            ar.result().close();
          })
      );

    }
  }
}
