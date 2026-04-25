## Member 4 smoke checklist

- Start WildFly/JBoss with datasource `java:/jboss/datasources/ExampleDS`.
- Ensure JMS queue exists: `java:/jms/queue/GivingHandNotificationsQueue`.
- Deploy `Phase1.war`.

### API checks

1. Call `POST /api/warehouse/{warehouse_id}/add` with org email and valid item.
2. Call `GET /api/warehouse/dashboard?organizationEmail=...` and verify item appears.
3. Call `POST /api/inventory/allocate` with valid quantity and confirm:
   - warehouse quantity decreases
   - campaign item `received_quantity` increases
4. Call allocation again with very large quantity and verify `400 Insufficient stock`.
5. Call `POST /api/notifications/donation-received`.
6. Call `GET /api/notifications` and verify `STOCK_LOW_ALERT` / `DONATION_RECEIVED` entries.

### MDB log checks

Look at server console for lines like:

- `[NotificationListenerMDB] Received notification: {"event_type":"STOCK_LOW_ALERT",...}`
- `[NotificationListenerMDB] Received notification: {"event_type":"DONATION_RECEIVED",...}`
