package com.corejsf;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Stateless
@Path("/greet")
@Produces(MediaType.APPLICATION_XML)
@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
public class HelloWorld{
	@GET
	@Produces(MediaType.TEXT_PLAIN)
    public String getGreet() {
        return "Greetings from the backend";
    }
}