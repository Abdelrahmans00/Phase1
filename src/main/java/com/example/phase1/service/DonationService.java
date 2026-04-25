package com.example.phase1.service;

import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.CampaignItem;
import com.example.phase1.entity.Donation;
import com.example.phase1.entity.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless
public class DonationService {

    @PersistenceContext
    private EntityManager em;

    public Donation commit(User donor, Long campaignId, String itemName, int quantity) {
        // Re-fetch campaign within this transaction/session
        Campaign campaign = em.find(Campaign.class, campaignId);
        if (campaign == null) return null;

        CampaignItem matchedItem = null;
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                matchedItem = item;
                break;
            }
        }

        if (matchedItem == null) return null;
        if (matchedItem.getQuantity() < quantity) return null;

        matchedItem.setQuantity(matchedItem.getQuantity() - quantity);

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

    public Donation updateQuantity(Donation donation, int newQuantity) {
        int diff = newQuantity - donation.getQuantity();
        // Re-fetch to ensure session is active
        Campaign campaign = em.find(Campaign.class, donation.getCampaign().getId());
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(donation.getItemName())) {
                int remaining = item.getQuantity() - diff;
                if (remaining < 0) return null;
                item.setQuantity(remaining);
                break;
            }
        }
        donation.setQuantity(newQuantity);
        donation.setUpdatedAt(new Date());
        return donation;
    }

    public void cancel(Donation donation) {
        Campaign campaign = em.find(Campaign.class, donation.getCampaign().getId());
        for (CampaignItem item : campaign.getItems()) {
            if (item.getName().equalsIgnoreCase(donation.getItemName())) {
                item.setQuantity(item.getQuantity() + donation.getQuantity());
                break;
            }
        }
        em.remove(em.contains(donation) ? donation : em.merge(donation));
    }

    // Advance status: COMMITTED -> RECEIVED or RECEIVED -> DISTRIBUTED
    public Donation advanceStatus(Donation donation) {
        switch (donation.getStatus()) {
            case "COMMITTED":
                donation.setStatus("RECEIVED");
                break;
            case "RECEIVED":
                donation.setStatus("DISTRIBUTED");
                break;
            default:
                return null;
        }

        donation.setUpdatedAt(new Date());

        return em.merge(donation);
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