package ninja.mcstats.api.packages.server.identifier;

import ninja.mcstats.api.models.Error;

import java.io.Serializable;

public class IdentifierResponse implements Serializable {

    private String identifier;
    private Error error;

    public IdentifierResponse(String identifier) {
        this.identifier = identifier;
    }

    public IdentifierResponse(Error error) {
        this.error = error;
    }

    
}
