package com.example.phase1.resource;

import com.example.phase1.entity.NotificationEvent;
import com.example.phase1.service.NotificationService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/api/notifications")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @EJB
    private NotificationService notificationService;

    @GET
    public Response getNotifications(@QueryParam("limit") @DefaultValue("50") int limit) {
        int resolvedLimit = Math.max(1, Math.min(limit, 200));
        List<NotificationEvent> events = notificationService.getRecentNotifications(resolvedLimit);

        List<Map<String, Object>> response = new ArrayList<>();
        for (NotificationEvent event : events) {
            Map<String, Object> row = new HashMap<>();
            row.put("event_type", event.getEventType());
            row.put("message", event.getMessage());
            row.put("timestamp", event.getTimestamp());
            response.add(row);
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/donation-received")
    public Response donationReceived(Map<String, Object> body) {
        String message = asString(body.get("message"));
        String timestampText = asString(body.get("timestamp"));

        if (message == null) {
            return Response.status(400).entity("{\"message\":\"message is required\"}").build();
        }

        Date timestamp = parseTimestamp(timestampText);
        if (timestampText != null && timestamp == null) {
            return Response.status(400)
                    .entity("{\"message\":\"timestamp must be ISO-8601, for example 2026-04-25T09:00:00.000Z\"}")
                    .build();
        }

        NotificationEvent event = notificationService.notifyDonationReceived(message, timestamp);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Donation notification published.");
        response.put("event_type", event.getEventType());
        response.put("timestamp", event.getTimestamp());
        return Response.status(201).entity(response).build();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String parsed = String.valueOf(value).trim();
        return parsed.isEmpty() ? null : parsed;
    }

    private Date parseTimestamp(String value) {
        if (value == null) {
            return null;
        }
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return formatter.parse(value);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }
}
