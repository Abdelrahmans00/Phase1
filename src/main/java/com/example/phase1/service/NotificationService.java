package com.example.phase1.service;

import com.example.phase1.entity.NotificationEvent;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Stateless
public class NotificationService {

    private static final String STOCK_LOW_ALERT = "STOCK_LOW_ALERT";
    private static final String DONATION_RECEIVED = "DONATION_RECEIVED";

    @PersistenceContext
    private EntityManager em;

    @Resource(lookup = "java:/jms/queue/GivingHandNotificationsQueue")
    private Queue notificationsQueue;

    @Inject
    private JMSContext jmsContext;

    public NotificationEvent createAndDispatch(String eventType, String message, Date timestamp) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(eventType);
        event.setMessage(message);
        event.setTimestamp(timestamp == null ? new Date() : timestamp);
        em.persist(event);
        sendToQueue(event);
        return event;
    }

    public NotificationEvent notifyLowStock(Long warehouseId, String itemName, int remainingQuantity) {
        String message = String.format(
                Locale.ENGLISH,
                "Low stock alert in warehouse %d for item '%s'. Remaining quantity: %d",
                warehouseId,
                itemName,
                remainingQuantity
        );
        return createAndDispatch(STOCK_LOW_ALERT, message, new Date());
    }

    public NotificationEvent notifyDonationReceived(String message, Date timestamp) {
        return createAndDispatch(DONATION_RECEIVED, message, timestamp == null ? new Date() : timestamp);
    }

    public List<NotificationEvent> getRecentNotifications(int limit) {
        return em.createQuery(
                        "SELECT n FROM NotificationEvent n ORDER BY n.timestamp DESC",
                        NotificationEvent.class)
                .setMaxResults(limit)
                .getResultList();
    }

    private void sendToQueue(NotificationEvent event) {
        String json = String.format(
                Locale.ENGLISH,
                "{\"event_type\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                escape(event.getEventType()),
                escape(event.getMessage()),
                formatTimestamp(event.getTimestamp())
        );
        jmsContext.createProducer().send(notificationsQueue, json);
    }

    private String formatTimestamp(Date timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(timestamp);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
