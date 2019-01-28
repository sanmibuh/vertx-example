package org.sanmibuh.vertx.example;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {

  private static final String JDBC_URL = "jdbc:hsqldb:mem:test?shutdown=true";
  private static final String JDBC_DRIVER = "org.hsqldb.jdbcDriver";

  private int port;
  private Vertx vertx;

  @Before
  public void setUp(final TestContext context) throws IOException {
    vertx = Vertx.vertx();

    final ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    final DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject()
            .put(Config.HTTP_PORT_KEY, port)
            .put(Config.URL_KEY, JDBC_URL)
            .put(Config.DRIVER_CLASS_KEY, JDBC_DRIVER)
        );

    vertx.deployVerticle(MyFirstVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(final TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testMyApplication(final TestContext context) {
    final Async async = context.async();

    vertx.createHttpClient().getNow(port, "localhost", "/",
        response -> response.handler(body -> {
          context.assertTrue(body.toString().contains("Hello"));
          async.complete();
        }));
  }

  @Test
  public void checkThatTheIndexPageIsServed(final TestContext context) {
    final Async async = context.async();
    vertx.createHttpClient().getNow(port, "localhost", "/assets/index.html", response -> {
      context.assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
      context.assertTrue(response.headers().get(HttpHeaders.CONTENT_TYPE).contains("text/html"));
      response.bodyHandler(body -> {
        context.assertTrue(body.toString().contains("<title>My Whisky Collection</title>"));
        async.complete();
      });
    });
  }

  @Test
  public void checkThatWeCanAdd(final TestContext context) {
    final Async async = context.async();
    final String json = Json.encodePrettily(new Whisky("Jameson", "Ireland"));
    final String length = Integer.toString(json.length());
    vertx.createHttpClient().post(port, "localhost", "/api/whiskies")
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .putHeader(HttpHeaders.CONTENT_LENGTH, length)
        .handler(response -> {
          context.assertEquals(response.statusCode(), HttpResponseStatus.CREATED.code());
          context.assertTrue(response.headers().get(HttpHeaders.CONTENT_TYPE).contains("application/json"));
          response.bodyHandler(body -> {
            final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);
            context.assertEquals(whisky.getName(), "Jameson");
            context.assertEquals(whisky.getOrigin(), "Ireland");
            context.assertNotNull(whisky.getId());
            async.complete();
          });
        })
        .write(json)
        .end();
  }
}

