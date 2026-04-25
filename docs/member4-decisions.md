## Member 4 implementation decisions

This file locks the preconditions and confirmation gates used to implement Feature 4 and Feature 5.

### Preconditions agreed

- **Target server**: JBoss EAP / WildFly with `java:/jboss/datasources/ExampleDS`.
- **Campaign received counts**: stored in `CampaignItem.receivedQuantity`.
- **Member 3 handoff**: Member 3 calls `POST /api/notifications/donation-received` when donation status becomes `Received`.

### API and rule decisions

- **Warehouse add endpoint**: `POST /api/warehouse/{warehouse_id}/add`
- **Warehouse dashboard endpoint**: `GET /api/warehouse/dashboard?organizationEmail=...`
- **Auth style**: `organizationEmail` in request body/query to match the existing project style.
- **Allocation endpoint**: `POST /api/inventory/allocate`
- **Item matching rule**: trim + case-insensitive name match.
- **Campaign status rule**: allocation is only allowed when campaign status is `Open`.

### JMS and notification decisions

- **JMS destination**: `java:/jms/queue/GivingHandNotificationsQueue`
- **Low stock threshold**: quantity `<= 10` after allocation.
- **`GET /api/notifications` storage**: persisted using `NotificationEvent` entity.
