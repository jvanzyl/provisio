package io.takari.server;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import java.io.InputStream;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

@Path("/status")
public class Status {

  @GET
  @Produces(TEXT_PLAIN)
  public Response status() {
    return Response.ok("happy").build();
  }
}
