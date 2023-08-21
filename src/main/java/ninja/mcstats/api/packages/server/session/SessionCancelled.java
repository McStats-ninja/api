package ninja.mcstats.api.packages.server.session;

import ninja.mcstats.api.models.Session;

import java.io.Serializable;

public class SessionCancelled implements Serializable {

    private final Session session;
    private final String reason;

    public SessionCancelled(Session session, String reason) {
        this.session = session;
        this.reason = reason;
    }

    public Session session() {
        return session;
    }

    public String reason() {
        return reason;
    }
}
