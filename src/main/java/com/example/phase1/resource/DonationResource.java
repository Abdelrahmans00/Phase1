package com.example.phase1.resource;

import com.example.phase1.dto.AdvanceStatusRequest;
import com.example.phase1.dto.CommitDonationRequest;
import com.example.phase1.dto.UpdateDonationRequest;
import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.Donation;
import com.example.phase1.entity.User;
import com.example.phase1.service.CampaignService;
import com.example.phase1.service.DonationService;
import com.example.phase1.service.UserService;
import com.example.phase1.util.SecurityUtil;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/donations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DonationResource {

    @EJB
    private DonationService donationService;

    @EJB
    private UserService userService;

    @EJB
    private CampaignService campaignService;

    // Commit to Donate
    @POST
    @Path("/commit")
    public Response commit(CommitDonationRequest request) {
        if (request.donorEmail == null || request.campaign_id == null ||
                request.item_name == null || request.quantity == null) {
            return Response.status(400)
                    .entity("{\"message\":\"Missing required fields\"}")
                    .build();
        }

        if (request.quantity < 1) {
            return Response.status(400)
                    .entity("{\"message\":\"Quantity must be at least 1\"}")
                    .build();
        }

        User donor = userService.findByEmail(request.donorEmail);
        if (donor == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donor not found\"}")
                    .build();
        }

        if (!SecurityUtil.isDonor(donor)) {
            return Response.status(403)
                    .entity("{\"message\":\"Only donors can commit to donate\"}")
                    .build();
        }

        Campaign campaign = campaignService.findById(request.campaign_id);
        if (campaign == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Campaign not found\"}")
                    .build();
        }

        if (!"Open".equals(campaign.getStatus())) {
            return Response.status(400)
                    .entity("{\"message\":\"Campaign is not open for donations\"}")
                    .build();
        }

        Donation donation = donationService.commit(donor, request.campaign_id, request.item_name, request.quantity);
        if (donation == null) {
            return Response.status(400)
                    .entity("{\"message\":\"Item not found in campaign or insufficient remaining quantity\"}")
                    .build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Commitment recorded. Please drop off items at the warehouse.");
        response.put("donation_id", donation.getId());
        return Response.ok(response).build();
    }

    // Edit Commitment (only if COMMITTED)
    @PUT
    @Path("/{id}")
    public Response updateDonation(@PathParam("id") Long id, UpdateDonationRequest request) {
        if (request.donorEmail == null || request.quantity == null) {
            return Response.status(400)
                    .entity("{\"message\":\"donorEmail and quantity are required\"}")
                    .build();
        }

        if (request.quantity < 1) {
            return Response.status(400)
                    .entity("{\"message\":\"Quantity must be at least 1\"}")
                    .build();
        }

        Donation donation = donationService.findById(id);
        if (donation == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donation not found\"}")
                    .build();
        }

        // Verify ownership
        if (!donation.getDonor().getEmail().equals(request.donorEmail)) {
            return Response.status(403)
                    .entity("{\"message\":\"You are not authorized to update this donation\"}")
                    .build();
        }

        // Status guard — only COMMITTED can be edited
        if (!"COMMITTED".equals(donation.getStatus())) {
            return Response.status(400)
                    .entity("{\"message\":\"Cannot edit a donation that has already been received\"}")
                    .build();
        }

        Donation updated = donationService.updateQuantity(donation, request.quantity);
        if (updated == null) {
            return Response.status(400)
                    .entity("{\"message\":\"Insufficient remaining quantity in campaign for this update\"}")
                    .build();
        }

        return Response.ok("{\"message\":\"Donation updated successfully.\"}").build();
    }

    // Cancel Commitment (only if COMMITTED)
    @DELETE
    @Path("/{id}")
    public Response cancelDonation(@PathParam("id") Long id, @QueryParam("donorEmail") String donorEmail) {
        if (donorEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"donorEmail query param is required\"}")
                    .build();
        }

        Donation donation = donationService.findById(id);
        if (donation == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donation not found\"}")
                    .build();
        }

        if (!donation.getDonor().getEmail().equals(donorEmail)) {
            return Response.status(403)
                    .entity("{\"message\":\"You are not authorized to cancel this donation\"}")
                    .build();
        }

        if (!"COMMITTED".equals(donation.getStatus())) {
            return Response.status(400)
                    .entity("{\"message\":\"Cannot cancel a donation that has already been received\"}")
                    .build();
        }

        donationService.cancel(donation);
        return Response.ok("{\"message\":\"Donation cancelled successfully.\"}").build();
    }

    // Advance Status (COMMITTED->RECEIVED or RECEIVED->DISTRIBUTED)
    @PUT
    @Path("/{id}/status")
    public Response advanceStatus(@PathParam("id") Long id, AdvanceStatusRequest request) {
        Donation donation = donationService.findById(id);
        if (donation == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donation not found\"}")
                    .build();
        }

        // Verify the requester is the campaign's organization
        if (request.organizationEmail != null) {
            User org = userService.findByEmail(request.organizationEmail);
            if (org == null || !org.getId().equals(donation.getCampaign().getOrganization().getId())) {
                return Response.status(403)
                        .entity("{\"message\":\"You are not authorized to update this donation status\"}")
                        .build();
            }
        }

        Donation updated = donationService.advanceStatus(donation);
        if (updated == null) {
            return Response.status(400)
                    .entity("{\"message\":\"Donation is already distributed\"}")
                    .build();
        }

        return Response.ok("{\"message\":\"Donation status updated to " + updated.getStatus() + ".\"}").build();
    }

    // Get contribution history
    @GET
    @Path("/history")
    public Response getHistory(@QueryParam("donorEmail") String donorEmail) {
        if (donorEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"donorEmail query param is required\"}")
                    .build();
        }

        User donor = userService.findByEmail(donorEmail);
        if (donor == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donor not found\"}")
                    .build();
        }

        List<Donation> history = donationService.getContributionHistory(donor.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Donation d : history) {
            Map<String, Object> map = new HashMap<>();
            map.put("donation_id", d.getId());
            map.put("campaign_title", d.getCampaign().getTitle());
            map.put("item_name", d.getItemName());
            map.put("quantity", d.getQuantity());
            map.put("status", d.getStatus());
            map.put("distributed_at", d.getUpdatedAt());
            result.add(map);
        }

        return Response.ok(result).build();
    }

    // Get all my donations (donor view)
    @GET
    @Path("/my")
    public Response getMyDonations(@QueryParam("donorEmail") String donorEmail) {
        if (donorEmail == null) {
            return Response.status(400)
                    .entity("{\"message\":\"donorEmail query param is required\"}")
                    .build();
        }

        User donor = userService.findByEmail(donorEmail);
        if (donor == null) {
            return Response.status(404)
                    .entity("{\"message\":\"Donor not found\"}")
                    .build();
        }

        List<Donation> donations = donationService.getDonationsByDonor(donor.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Donation d : donations) {
            Map<String, Object> map = new HashMap<>();
            map.put("donation_id", d.getId());
            map.put("campaign_title", d.getCampaign().getTitle());
            map.put("item_name", d.getItemName());
            map.put("quantity", d.getQuantity());
            map.put("status", d.getStatus());
            map.put("created_at", d.getCreatedAt());
            result.add(map);
        }

        return Response.ok(result).build();
    }
}