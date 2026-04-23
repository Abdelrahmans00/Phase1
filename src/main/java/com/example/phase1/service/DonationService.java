package com.example.phase1.service;

import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.CampaignItem;
import com.example.phase1.entity.Donation;
import com.example.phase1.entity.User;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless
public class DonationService {

    @PersistenceContext
    private EntityManager em;

    public Donation commit(User donor, Campaign campaign, String itemName, int quantity) {
        // Find the matching CampaignItem and reduce remaining quantity
        CampaignItem matchedItem = null;
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                matchedItem = item;
                break;
            }
        }

        if (matchedItem == null) {
            return null; // item not found in campaign
        }

        if (matchedItem.getQuantity() < quantity) {
            return null; // not enough remaining needed
        }

        // Decrease remaining needed quantity
        matchedItem.setQuantity(matchedItem.getQuantity() - quantity);

        // Create donation
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setCampaign(campaign);
        donation.setItemName(itemName);
        donation.setQuantity(quantity);
        donation.setStatus("COMMITTED");
        donation.setCreatedAt(new Date());

        em.persist(donation);
        return donation;
    }

    public Donation findById(Long id) {
        return em.find(Donation.class, id);
    }

    // Edit quantity — only if COMMITTED
    public Donation updateQuantity(Donation donation, int newQuantity) {
        int oldQuantity = donation.getQuantity();
        int diff = newQuantity - oldQuantity;

        // Find the campaign item and adjust remaining quantity
        Campaign campaign = donation.getCampaign();
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(donation.getItemName())) {
                int remaining = item.getQuantity() - diff;
                if (remaining < 0) return null; // not enough remaining
                item.setQuantity(remaining);
                break;
            }
        }

        donation.setQuantity(newQuantity);
        donation.setUpdatedAt(new Date());
        return donation;
    }

    // Cancel — only if COMMITTED, restore quantity to campaign
    public boolean cancel(Donation donation) {
        Campaign campaign = donation.getCampaign();
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(donation.getItemName())) {
                item.setQuantity(item.getQuantity() + donation.getQuantity());
                break;
            }
        }
        em.remove(em.contains(donation) ? donation : em.merge(donation));
        return true;
    }

    // Advance status: COMMITTED -> RECEIVED or RECEIVED -> DISTRIBUTED
    public Donation advanceStatus(Donation donation) {
        if ("COMMITTED".equals(donation.getStatus())) {
            donation.setStatus("RECEIVED");
        } else if ("RECEIVED".equals(donation.getStatus())) {
            donation.setStatus("DISTRIBUTED");
        } else {
            return null; // already DISTRIBUTED, can't advance
        }
        donation.setUpdatedAt(new Date());
        return donation;
    }

    // Get all donations by a donor
    public List<Donation> getDonationsByDonor(Long donorId) {
        return em.createQuery(
                "SELECT d FROM Donation d WHERE d.donor.id = :donorId ORDER BY d.createdAt DESC",
                Donation.class
        ).setParameter("donorId", donorId).getResultList();
    }

    // Get distributed donations by donor (contribution history)
    public List<Donation> getContributionHistory(Long donorId) {
        return em.createQuery(
                "SELECT d FROM Donation d WHERE d.donor.id = :donorId AND d.status = 'DISTRIBUTED' ORDER BY d.updatedAt DESC",
                Donation.class
        ).setParameter("donorId", donorId).getResultList();
    }
}