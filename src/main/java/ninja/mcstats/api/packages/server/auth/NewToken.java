package ninja.mcstats.api.packages.server.auth;

import java.io.Serializable;

public class NewToken implements Serializable {

    private final String token;

    public NewToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
