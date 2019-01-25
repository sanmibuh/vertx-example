package org.sanmibuh.vertx.example;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import com.jayway.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MyRestIT {

  @BeforeClass
  public static void configureRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = Integer.getInteger(Config.HTTP_PORT_KEY, Config.DEFAULT_HTTP_PORT);
  }

  @AfterClass
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }

  @Test
  public void checkThatWeCanRetrieveIndividualProduct() {
    // Get the list of bottles, ensure it's a success and extract the first id.
    final int id = get("/api/whiskies").then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .extract()
        .jsonPath().getInt("find { it.name=='Bowmore 15 Years Laimrig' }.id");
    // Now get the individual resource and check the content
    get("/api/whiskies/" + id).then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .body("name", equalTo("Bowmore 15 Years Laimrig"))
        .body("origin", equalTo("Scotland, Islay"))
        .body("id", equalTo(id));
  }

  @Test
  public void checkWeCanAddAndDeleteAProduct() {
    // Create a new bottle and retrieve the result (as a Whisky instance).
    final Whisky whisky = given()
        .body("{\"name\":\"Jameson\", \"origin\":\"Ireland\"}").request().post("/api/whiskies").thenReturn()
        .as(Whisky.class);
    assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
    assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
    assertThat(whisky.getId()).isNotZero();
    // Check that it has created an individual resource, and check the content.
    get("/api/whiskies/" + whisky.getId()).then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .body("name", equalTo("Jameson"))
        .body("origin", equalTo("Ireland"))
        .body("id", equalTo(whisky.getId()));
    // Delete the bottle
    delete("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(HttpStatus.SC_NO_CONTENT);
    // Check that the resource is not available anymore
    get("/api/whiskies/" + whisky.getId()).then()
        .assertThat()
        .statusCode(HttpStatus.SC_NOT_FOUND);
  }
}
