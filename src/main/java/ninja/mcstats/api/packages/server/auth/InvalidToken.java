package ninja.mcstats.api.packages.server.auth;

import java.io.Serializable;

public class InvalidToken implements Serializable {

    private final String token;

    public InvalidToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
