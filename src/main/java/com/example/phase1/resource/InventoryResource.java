package com.example.phase1.resource;

import com.example.phase1.service.InventoryService;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/api/inventory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private InventoryService inventoryService;

    @POST
    @Path("/allocate")
    public Response allocate(Map<String, Object> body) {
        Long warehouseId = asLong(body.get("warehouse_id"));
        Long campaignId = asLong(body.get("campaign_id"));
        String itemName = asString(body.get("item_name"));
        Integer quantity = asInteger(body.get("quantity"));
        String organizationEmail = asString(body.get("organizationEmail"));

        if (warehouseId == null || campaignId == null || itemName == null || quantity == null || organizationEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"warehouse_id, campaign_id, item_name, quantity, and organizationEmail are required\"}")
                    .build();
        }

        if (quantity < 1) {
            return Response.status(400).entity("{\"message\":\"quantity must be at least 1\"}").build();
        }

        try {
            InventoryService.AllocationResult result = inventoryService.allocate(
                    warehouseId,
                    campaignId,
                    itemName,
                    quantity,
                    organizationEmail
            );
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Inventory allocated successfully.");
            response.put("warehouse_id", result.getWarehouseId());
            response.put("campaign_id", result.getCampaignId());
            response.put("item_name", result.getItemName());
            response.put("allocated_quantity", result.getAllocatedQuantity());
            response.put("remaining_warehouse_quantity", result.getRemainingWarehouseQuantity());
            response.put("campaign_received_quantity", result.getCampaignReceivedQuantity());
            return Response.ok(response).build();
        } catch (WebApplicationException ex) {
            return ex.getResponse();
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim().isEmpty() ? null : value.toString().trim();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
