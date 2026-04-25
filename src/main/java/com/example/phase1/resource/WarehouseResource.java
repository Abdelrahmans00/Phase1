package com.example.phase1.resource;

import com.example.phase1.entity.User;
import com.example.phase1.entity.Warehouse;
import com.example.phase1.entity.WarehouseItem;
import com.example.phase1.service.UserService;
import com.example.phase1.service.WarehouseService;
import com.example.phase1.util.SecurityUtil;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/warehouse")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WarehouseResource {

    @EJB
    private WarehouseService warehouseService;

    @EJB
    private UserService userService;

    @POST
    @Path("/{warehouse_id}/add")
    public Response addInventory(@PathParam("warehouse_id") Long warehouseId, Map<String, Object> body) {
        String organizationEmail = asString(body.get("organizationEmail"));
        String itemName = asString(body.get("item_name"));
        String category = asString(body.get("category"));
        Integer quantity = asInteger(body.get("quantity"));

        if (organizationEmail == null || itemName == null || quantity == null) {
            return Response.status(400)
                    .entity("{\"message\":\"organizationEmail, item_name, and quantity are required\"}")
                    .build();
        }
        if (quantity < 1) {
            return Response.status(400)
                    .entity("{\"message\":\"quantity must be at least 1\"}")
                    .build();
        }

        User organization = userService.findByEmail(organizationEmail);
        if (organization == null) {
            return Response.status(404).entity("{\"message\":\"Organization user not found\"}").build();
        }
        if (!SecurityUtil.isOrganization(organization)) {
            return Response.status(403).entity("{\"message\":\"Only organization users can add inventory\"}").build();
        }

        Warehouse warehouse = (warehouseId == 0)
                ? warehouseService.findOrCreateDefaultWarehouse(organization)
                : warehouseService.findWarehouseById(warehouseId);
        if (warehouse == null) {
            return Response.status(404).entity("{\"message\":\"Warehouse not found\"}").build();
        }
        if (!warehouse.getOrganization().getId().equals(organization.getId())) {
            return Response.status(403).entity("{\"message\":\"You are not authorized to update this warehouse\"}").build();
        }

        WarehouseItem item = warehouseService.addInventory(warehouse, itemName, quantity, category);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Inventory item added successfully.");
        response.put("warehouse_id", warehouse.getId());
        response.put("item_name", item.getItemName());
        response.put("category", item.getCategory());
        response.put("quantity", item.getQuantity());
        return Response.ok(response).build();
    }

    @GET
    @Path("/dashboard")
    public Response getOrganizationInventory(@QueryParam("organizationEmail") String organizationEmail) {
        if (organizationEmail == null || organizationEmail.trim().isEmpty()) {
            return Response.status(400).entity("{\"message\":\"organizationEmail is required\"}").build();
        }

        User organization = userService.findByEmail(organizationEmail);
        if (organization == null) {
            return Response.status(404).entity("{\"message\":\"Organization user not found\"}").build();
        }
        if (!SecurityUtil.isOrganization(organization)) {
            return Response.status(403).entity("{\"message\":\"Only organization users can view inventory\"}").build();
        }

        List<WarehouseItem> items = warehouseService.getInventoryByOrganization(organization.getId());
        List<Map<String, Object>> response = new ArrayList<>();
        for (WarehouseItem item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("warehouse_id", item.getWarehouse().getId());
            row.put("warehouse_name", item.getWarehouse().getName());
            row.put("item_name", item.getItemName());
            row.put("category", item.getCategory());
            row.put("quantity", item.getQuantity());
            response.add(row);
        }
        return Response.ok(response).build();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim().isEmpty() ? null : value.toString().trim();
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
