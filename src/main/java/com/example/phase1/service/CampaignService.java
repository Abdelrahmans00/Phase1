package com.example.phase1.service;

import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.CampaignItem;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless
public class CampaignService {

    @PersistenceContext
    private EntityManager em;

    public Campaign createCampaign(Campaign campaign, List<CampaignItem> items) {
        campaign.setStatus("Open");
        campaign.setCreatedDate(new Date());

        em.persist(campaign);

        for (CampaignItem item : items) {
            item.setCampaign(campaign);
            if (item.getReceivedQuantity() == null) {
                item.setReceivedQuantity(0);
            }
            em.persist(item);
        }

        campaign.setItems(items);
        return campaign;
    }

    public Campaign findById(Long id) {
        return em.find(Campaign.class, id);
    }

    // Update status
    public Campaign updateStatus(Long campaignId, String status) {
        Campaign campaign = em.find(Campaign.class, campaignId);
        if (campaign == null) {
            return null;
        }
        campaign.setStatus(status);
        return campaign;
    }

    public Campaign updateItems(Long campaignId, List<CampaignItem> newItems) {
        Campaign campaign = em.find(Campaign.class, campaignId);
        if (campaign == null) {
            return null;
        }

        // rem old
        campaign.getItems().clear();
        em.flush();

        // add new
        for (CampaignItem item : newItems) {
            item.setCampaign(campaign);
            if (item.getReceivedQuantity() == null) {
                item.setReceivedQuantity(0);
            }
            campaign.getItems().add(item);
        }

        return campaign;
    }

    public List<Campaign> getActiveCampaigns() {
        return em.createQuery(
                "SELECT c FROM Campaign c WHERE c.status = 'Open' ORDER BY c.createdDate DESC",
                Campaign.class).getResultList();
    }

    public List<Campaign> getActiveCampaignsByCategory(String category) {
        return em.createQuery(
                "SELECT c FROM Campaign c WHERE c.status = 'Open' AND c.category = :category ORDER BY c.createdDate DESC",
                Campaign.class).setParameter("category", category).getResultList();
    }

    public List<Campaign> getCampaignsByOrganization(Long organizationId) {
        return em.createQuery(
                "SELECT c FROM Campaign c WHERE c.organization.id = :orgId ORDER BY c.createdDate DESC",
                Campaign.class).setParameter("orgId", organizationId).getResultList();
    }
}
