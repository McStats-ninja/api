package ninja.mcstats.api.packages.server.session;

import ninja.mcstats.api.models.Session;

import java.io.Serializable;
import java.util.Date;

public class SessionInitiated implements Serializable {
    private final Session session;

    private final Date date = new Date();
    
    public SessionInitiated(Session session){
        this.session = session;
    }

    public Session session(){
        return session;
    }

    public Date date() {
        return date;
    }
}
