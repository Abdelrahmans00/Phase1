package com.example.phase1.service;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/GivingHandNotificationsQueue"),
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
        }
)
public class NotificationListenerMDB implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getText();
                System.out.println("[NotificationListenerMDB] Received notification: " + payload);
            } else {
                System.out.println("[NotificationListenerMDB] Received non-text JMS message.");
            }
        } catch (JMSException ex) {
            System.err.println("[NotificationListenerMDB] Failed to read JMS message: " + ex.getMessage());
        }
    }
}
