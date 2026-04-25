package com.example.phase1.service;

import com.example.phase1.entity.User;
import com.example.phase1.entity.Warehouse;
import com.example.phase1.entity.WarehouseItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class WarehouseService {

    @PersistenceContext
    private EntityManager em;

    public Warehouse findWarehouseById(Long warehouseId) {
        return em.find(Warehouse.class, warehouseId);
    }

    public Warehouse findOrCreateDefaultWarehouse(User organization) {
        List<Warehouse> warehouses = em.createQuery(
                        "SELECT w FROM Warehouse w WHERE w.organization.id = :orgId ORDER BY w.id ASC",
                        Warehouse.class)
                .setParameter("orgId", organization.getId())
                .setMaxResults(1)
                .getResultList();

        if (!warehouses.isEmpty()) {
            return warehouses.get(0);
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(organization.getName() + " Warehouse");
        warehouse.setDescription("Auto-created warehouse for organization");
        warehouse.setOrganization(organization);
        em.persist(warehouse);
        return warehouse;
    }

    public WarehouseItem findItemByName(Long warehouseId, String normalizedItemName) {
        List<WarehouseItem> items = em.createQuery(
                        "SELECT wi FROM WarehouseItem wi WHERE wi.warehouse.id = :warehouseId AND LOWER(TRIM(wi.itemName)) = :itemName",
                        WarehouseItem.class)
                .setParameter("warehouseId", warehouseId)
                .setParameter("itemName", normalizedItemName)
                .setMaxResults(1)
                .getResultList();
        return items.isEmpty() ? null : items.get(0);
    }

    public WarehouseItem addInventory(Warehouse warehouse, String itemName, int quantity, String category) {
        String normalizedItemName = itemName.trim().toLowerCase();
        WarehouseItem item = findItemByName(warehouse.getId(), normalizedItemName);
        if (item == null) {
            item = new WarehouseItem();
            item.setWarehouse(warehouse);
            item.setItemName(itemName.trim());
            item.setQuantity(quantity);
            item.setCategory((category == null || category.trim().isEmpty()) ? "General" : category.trim());
            em.persist(item);
            return item;
        }

        item.setQuantity(item.getQuantity() + quantity);
        if (category != null && !category.trim().isEmpty()) {
            item.setCategory(category.trim());
        }
        return item;
    }

    public List<WarehouseItem> getInventoryByOrganization(Long organizationId) {
        return em.createQuery(
                        "SELECT wi FROM WarehouseItem wi WHERE wi.warehouse.organization.id = :orgId ORDER BY wi.warehouse.id ASC, wi.itemName ASC",
                        WarehouseItem.class)
                .setParameter("orgId", organizationId)
                .getResultList();
    }
}
