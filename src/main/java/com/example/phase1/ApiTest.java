package com.example.phase1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/api-test")
public class ApiTest {
    @GET
    @Produces("text/plain")
    public String hello() {
        return "Hello, api!";
    }
}
