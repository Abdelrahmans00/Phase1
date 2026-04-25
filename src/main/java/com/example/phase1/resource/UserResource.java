package com.example.phase1.resource;

import com.example.phase1.dto.LoginRequest;
import com.example.phase1.dto.RegisterRequest;
import com.example.phase1.dto.UpdateRequest;
import com.example.phase1.entity.User;
import com.example.phase1.service.UserService;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public class UserResource {
    @EJB
    private UserService userService;

    @POST
    @Path("/register")
    public Response register(RegisterRequest request){
        if(request.email ==null || request.fullName ==null || request.password ==null ||request.role==null){
            return Response.status(400).entity("{\"message\":\"Missing fields\"}").build();
        }

        request.role = request.role.toLowerCase();
        if (!"donor".equals(request.role) && !"organization".equals(request.role)) {
            return Response.status(400)
                    .entity("{\"message\":\"Role must be donor or organization\"}")
                    .build();
        }

        if(userService.emailExists(request.email)){
            return Response.status(400).entity("{\"message\":\"Email already exists\"}").build();
        }



        Date birthdayDate = null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            birthdayDate = sdf.parse(request.birthday);
        } catch (ParseException e) {
            return Response.status(400)
                    .entity("{\"message\":\"Invalid birthday format. Use yyyy-MM-dd\"}")
                    .build();
        }

        User user = new User();
        user.setEmail(request.email);
        user.setPassword(request.password);
        user.setName(request.fullName);
        user.setBio(request.bio);
        user.setRole(request.role);
        user.setBirthday(birthdayDate);

        userService.register(user);

        return Response.status(201).entity("{\"message\":\"User registered successfully.\"}").build();


    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request){
        User user= userService.login(request.email, request.password);
        if(user ==null){
            return Response.status(401).entity("{\"message\":\"Invalid credentials\"}").build();
        }
        return Response.ok("{\"message\":\"Login successful.\"}").build();
    }

    @PUT
    @Path("/profile")
    public Response updateProfile(UpdateRequest request){
        if(request.email ==null|| request.fullName==null){
            return Response.status(400).entity("{\"message\":\"Email and name are required\"}").build();
        }
        User updatedUser = userService.updateProfile(
                request.email,
                request.fullName,
                request.bio
        );

        if (updatedUser == null) {
            return Response.status(404)
                    .entity("{\"message\":\"User not found\"}")
                    .build();
        }

        return Response.ok("{\"message\":\"Profile updated successfully.\"}").build();

    }

}
