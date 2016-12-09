package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Deletes any value that may exist for the given key. */
public final class DeleteServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    RequestReader reader = RequestReader.create(request);
    ResponseWriter writer = ResponseWriter.create(response);
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    String key = reader.readKey();
    if (key == null) {
      writer.write("No key specified.");
      return;
    }
    writer.write(Boolean.toString(memcache.delete(key)));
  }
}
