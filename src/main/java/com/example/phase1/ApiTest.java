package com.example.phase1;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/api-test")
public class ApiTest {
    @GET
    @Produces("text/plain")
    public String hello() {
        return "Hello, api!";
    }
}
