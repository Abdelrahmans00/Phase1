package com.example.phase1.resource;

import com.example.phase1.dto.CampaignItemDTO;
import com.example.phase1.dto.CampaignRequest;
import com.example.phase1.dto.UpdateCampaignItemsRequest;
import com.example.phase1.dto.UpdateCampaignStatusRequest;
import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.CampaignItem;
import com.example.phase1.entity.User;
import com.example.phase1.service.CampaignService;
import com.example.phase1.service.UserService;
import com.example.phase1.util.SecurityUtil;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/campaigns")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CampaignResource {

    @EJB
    private CampaignService campaignService;

    @EJB
    private UserService userService;

    // Create Campaign
    @RolesAllowed("Organization")
    @POST
    @Path("/create")
    public Response createCampaign(CampaignRequest request) {
        // Validate fields
        if (request.organizationEmail == null || request.title == null ||
                request.description == null || request.category == null) {
            return Response.status(400)
                    .entity("{\"message\":\"Missing required fields (organizationEmail, title, description, category)\"}")
                    .build();
        }

        if (request.needed_items == null || request.needed_items.isEmpty()) {
            return Response.status(400)
                    .entity("{\"message\":\"At least one item is required\"}")
                    .build();
        }

        User orgUser = userService.findByEmail(request.organizationEmail);
        if (orgUser == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Organization user not found\"}")
                    .build();
        }

        // Check user is organization
        if (!SecurityUtil.isOrganization(orgUser)) {
            return Response.status(403)
                    .entity("{\"message\":\"Only organization users can create campaigns\"}")
                    .build();
        }

        for (CampaignItemDTO itemDTO : request.needed_items) {
            if (itemDTO.item_name == null || itemDTO.item_name.trim().isEmpty()) {
                return Response.status(400)
                        .entity("{\"message\":\"Each item must have a name\"}")
                        .build();
            }
            if (itemDTO.target_quantity == null || itemDTO.target_quantity < 1) {
                return Response.status(400)
                        .entity("{\"message\":\"Each item must have a quantity of at least 1\"}")
                        .build();
            }
        }

        // Build Campaign
        Campaign campaign = new Campaign();
        campaign.setTitle(request.title);
        campaign.setDescription(request.description);
        campaign.setCategory(request.category);
        campaign.setOrganization(orgUser);

        List<CampaignItem> items = new ArrayList<>();
        for (CampaignItemDTO itemDTO : request.needed_items) {
            CampaignItem item = new CampaignItem();
            item.setName(itemDTO.item_name);
            item.setQuantity(itemDTO.target_quantity);
            items.add(item);
        }

        Campaign created = campaignService.createCampaign(campaign, items);

        //response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Campaign created successfully.");
        response.put("campaign_id", created.getId());
        return Response.status(201).entity(response).build();
    }

    // Update Status
    @RolesAllowed("Organization")
    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id, UpdateCampaignStatusRequest request) {
        if (request.status == null || request.organizationEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"organizationEmail and status are required\"}")
                    .build();
        }

        String status = request.status;
        if (!"Open".equals(status) && !"Paused".equals(status) && !"Completed".equals(status)) {
            return Response.status(400)
                    .entity("{\"message\":\"Status must be Open, Paused, or Completed\"}")
                    .build();
        }

        // Find campaign
        Campaign campaign = campaignService.findById(id);
        if (campaign == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Campaign not found\"}")
                    .build();
        }

        // Verify ownership
        User orgUser = userService.findByEmail(request.organizationEmail);
        if (orgUser == null || !orgUser.getId().equals(campaign.getOrganization().getId())) {
            return Response.status(403)
                    .entity("{\"message\":\"You are not authorized to update this campaign\"}")
                    .build();
        }

        campaignService.updateStatus(id, status);

        return Response.ok("{\"message\":\"Campaign status updated to " + status + ".\"}").build();
    }

    // Update Campaign Items
    @RolesAllowed("Organization")
    @PUT
    @Path("/{id}/items")
    public Response updateItems(@PathParam("id") Long id, UpdateCampaignItemsRequest request) {
        if (request.organizationEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"organizationEmail is required\"}")
                    .build();
        }

        if (request.needed_items == null || request.needed_items.isEmpty()) {
            return Response.status(400)
                    .entity("{\"message\":\"At least one item is required\"}")
                    .build();
        }

        Campaign campaign = campaignService.findById(id);
        if (campaign == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Campaign not found\"}")
                    .build();
        }

        User orgUser = userService.findByEmail(request.organizationEmail);
        if (orgUser == null || !orgUser.getId().equals(campaign.getOrganization().getId())) {
            return Response.status(403)
                    .entity("{\"message\":\"You are not authorized to update this campaign\"}")
                    .build();
        }

        for (CampaignItemDTO itemDTO : request.needed_items) {
            if (itemDTO.item_name == null || itemDTO.item_name.trim().isEmpty()) {
                return Response.status(400)
                        .entity("{\"message\":\"Each item must have a name\"}")
                        .build();
            }
            if (itemDTO.target_quantity == null || itemDTO.target_quantity < 1) {
                return Response.status(400)
                        .entity("{\"message\":\"Each item must have a quantity of at least 1\"}")
                        .build();
            }
        }

        List<CampaignItem> newItems = new ArrayList<>();
        for (CampaignItemDTO itemDTO : request.needed_items) {
            CampaignItem item = new CampaignItem();
            item.setName(itemDTO.item_name);
            item.setQuantity(itemDTO.target_quantity);
            newItems.add(item);
        }

        campaignService.updateItems(id, newItems);

        return Response.ok("{\"message\":\"Campaign items updated successfully.\"}").build();
    }

    //Discover Campaigns
    @RolesAllowed({"Donor", "Organization"})
    @GET
    public Response getActiveCampaigns(@QueryParam("category") String category) {
        List<Campaign> campaigns;

        if (category != null && !category.trim().isEmpty()) {
            campaigns = campaignService.getActiveCampaignsByCategory(category);
        } else {
            campaigns = campaignService.getActiveCampaigns();
        }

        // Build list
        List<Map<String, Object>> result = new ArrayList<>();
        for (Campaign c : campaigns) {
            Map<String, Object> campaignMap = new HashMap<>();
            campaignMap.put("id", c.getId());
            campaignMap.put("title", c.getTitle());
            campaignMap.put("description", c.getDescription());
            campaignMap.put("category", c.getCategory());
            campaignMap.put("status", c.getStatus());
            campaignMap.put("organizationName", c.getOrganization().getName());
            campaignMap.put("createdDate", c.getCreatedDate());

            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (CampaignItem item : c.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("item_name", item.getName());
                itemMap.put("target_quantity", item.getQuantity());
                itemMap.put("received_quantity", item.getReceivedQuantity());
                itemsList.add(itemMap);
            }
            campaignMap.put("needed_items", itemsList);

            result.add(campaignMap);
        }

        return Response.ok(result).build();
    }
}
