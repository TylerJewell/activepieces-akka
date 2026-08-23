package io.akka.activepieces.api;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.http.HttpResponses;

/**
 * Serves activepieces' own web interface, built from {@code gui/} into
 * {@code src/main/resources/static-resources/}.
 *
 * <p>RENDERING.md R3 — this is the original's interface, not a smaller one standing in for it.
 * What the port changed is in {@code gui/src/lib/akka-feed.ts} and the two views that read it;
 * {@code activepieces-port/specs/RENDER-001-activepieces.md} lists the whole diff.
 */
@HttpEndpoint
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class UiEndpoint {

  @Get("/")
  public HttpResponse index() {
    return HttpResponses.staticResource("index.html");
  }

  /**
   * Routing is the application's, not this endpoint's: a path with no file behind it is one of its
   * own routes and gets the shell, which is what lets {@code /projects/x/runs/y} be openable
   * directly rather than only reachable by clicking.
   */
  @Get("/**")
  public HttpResponse asset(HttpRequest request) {
    String path = request.getUri().path();
    // /akka/ is the runtime's own namespace, not the application's. A catch-all that answers
    // there tells the runtime's health check that a path it expects to be absent exists, and
    // the service is then reported as never having started.
    if (path.startsWith("/akka/")) {
      return HttpResponses.notFound();
    }
    if (looksLikeAFile(path)) {
      return HttpResponses.staticResource(request, "/");
    }
    return HttpResponses.staticResource("index.html");
  }

  private static boolean looksLikeAFile(String path) {
    int lastSlash = path.lastIndexOf('/');
    return path.indexOf('.', lastSlash) > -1;
  }
}
