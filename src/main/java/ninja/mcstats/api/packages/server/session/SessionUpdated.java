package ninja.mcstats.api.packages.server.session;

import ninja.mcstats.api.models.Session;

import java.io.Serializable;
import java.util.Date;

public class SessionUpdated implements Serializable {

    private Session session;
    private Date date = new Date();

    public SessionUpdated(Session session){
        this.session = session;
    }

    public Session session() {
        return session;
    }

    public Date date() {
        return date;
    }
}
