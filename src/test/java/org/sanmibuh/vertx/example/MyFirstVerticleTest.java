package org.sanmibuh.vertx.example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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

  private int port;

  private Vertx vertx;

  @Before
  public void setUp(final TestContext context) throws IOException {
    vertx = Vertx.vertx();

    final ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    final DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put(Config.HTTP_PORT_KEY, port));

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
}

