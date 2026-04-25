## JMS setup for Member 4 features

The application expects this destination on WildFly/JBoss EAP:

- Queue lookup: `java:/jms/queue/GivingHandNotificationsQueue`

### Example WildFly CLI commands

Run server with the full profile, then execute via `jboss-cli` while the server is up:

```bash
standalone.bat -c standalone-full.xml
```

Then:

```bash
/subsystem=messaging-activemq/server=default/jms-queue=GivingHandNotificationsQueue:add(entries=["java:/jms/queue/GivingHandNotificationsQueue"])
```

The app uses:

- Producer: `NotificationService` (sends JSON text messages).
- Consumer: `NotificationListenerMDB` (logs to server console).
