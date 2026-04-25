package com.example.phase1.service;

import com.example.phase1.entity.Campaign;
import com.example.phase1.entity.CampaignItem;
import com.example.phase1.entity.User;
import com.example.phase1.entity.Warehouse;
import com.example.phase1.entity.WarehouseItem;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;

@Stateless
public class InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private NotificationService notificationService;

    public AllocationResult allocate(
            Long warehouseId,
            Long campaignId,
            String itemName,
            int quantity,
            String organizationEmail
    ) {
        Warehouse warehouse = em.find(Warehouse.class, warehouseId);
        if (warehouse == null) {
            throw notFound("Warehouse not found");
        }

        Campaign campaign = em.find(Campaign.class, campaignId);
        if (campaign == null) {
            throw notFound("Campaign not found");
        }

        User organization = warehouse.getOrganization();
        if (organization == null || !organization.getId().equals(campaign.getOrganization().getId())) {
            throw forbidden("Warehouse and campaign must belong to the same organization");
        }

        if (organizationEmail == null || !organizationEmail.equalsIgnoreCase(organization.getEmail())) {
            throw forbidden("You are not authorized to allocate inventory for this organization");
        }

        if (!"Open".equals(campaign.getStatus())) {
            throw badRequest("Allocation is allowed only when campaign status is Open");
        }

        String normalizedName = normalize(itemName);
        WarehouseItem warehouseItem = findWarehouseItem(warehouseId, normalizedName);
        if (warehouseItem == null) {
            throw badRequest("Item not found in warehouse inventory");
        }

        CampaignItem campaignItem = findCampaignItem(campaignId, normalizedName);
        if (campaignItem == null) {
            throw badRequest("Item does not exist in campaign requirements");
        }

        if (warehouseItem.getQuantity() < quantity) {
            throw badRequest("Insufficient stock");
        }

        warehouseItem.setQuantity(warehouseItem.getQuantity() - quantity);
        campaignItem.setReceivedQuantity(campaignItem.getReceivedQuantity() + quantity);

        if (warehouseItem.getQuantity() <= LOW_STOCK_THRESHOLD) {
            notificationService.notifyLowStock(warehouseId, warehouseItem.getItemName(), warehouseItem.getQuantity());
        }

        return new AllocationResult(
                warehouse.getId(),
                campaign.getId(),
                campaignItem.getName(),
                quantity,
                warehouseItem.getQuantity(),
                campaignItem.getReceivedQuantity()
        );
    }

    private WarehouseItem findWarehouseItem(Long warehouseId, String normalizedName) {
        List<WarehouseItem> items = em.createQuery(
                        "SELECT wi FROM WarehouseItem wi WHERE wi.warehouse.id = :warehouseId AND LOWER(TRIM(wi.itemName)) = :itemName",
                        WarehouseItem.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("itemName", normalizedName)
                .setMaxResults(1)
                .getResultList();
        return items.isEmpty() ? null : items.get(0);
    }

    private CampaignItem findCampaignItem(Long campaignId, String normalizedName) {
        List<CampaignItem> items = em.createQuery(
                        "SELECT ci FROM CampaignItem ci WHERE ci.campaign.id = :campaignId AND LOWER(TRIM(ci.name)) = :itemName",
                        CampaignItem.class)
                .setParameter("campaignId", campaignId)
                .setParameter("itemName", normalizedName)
                .setMaxResults(1)
                .getResultList();
        return items.isEmpty() ? null : items.get(0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(jsonMessage(message))
                .build());
    }

    private WebApplicationException forbidden(String message) {
        return new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(jsonMessage(message))
                .build());
    }

    private WebApplicationException notFound(String message) {
        return new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                .entity(jsonMessage(message))
                .build());
    }

    private String jsonMessage(String message) {
        return "{\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
    }

    public static class AllocationResult {
        private final Long warehouseId;
        private final Long campaignId;
        private final String itemName;
        private final Integer allocatedQuantity;
        private final Integer remainingWarehouseQuantity;
        private final Integer campaignReceivedQuantity;

        public AllocationResult(
                Long warehouseId,
                Long campaignId,
                String itemName,
                Integer allocatedQuantity,
                Integer remainingWarehouseQuantity,
                Integer campaignReceivedQuantity
        ) {
            this.warehouseId = warehouseId;
            this.campaignId = campaignId;
            this.itemName = itemName;
            this.allocatedQuantity = allocatedQuantity;
            this.remainingWarehouseQuantity = remainingWarehouseQuantity;
            this.campaignReceivedQuantity = campaignReceivedQuantity;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public Long getCampaignId() {
            return campaignId;
        }

        public String getItemName() {
            return itemName;
        }

        public Integer getAllocatedQuantity() {
            return allocatedQuantity;
        }

        public Integer getRemainingWarehouseQuantity() {
            return remainingWarehouseQuantity;
        }

        public Integer getCampaignReceivedQuantity() {
            return campaignReceivedQuantity;
        }
    }
}
