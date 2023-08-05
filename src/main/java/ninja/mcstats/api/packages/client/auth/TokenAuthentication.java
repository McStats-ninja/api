package ninja.mcstats.api.packages.client.auth;

import java.io.Serializable;

public class TokenAuthentication implements Serializable {

    private final String token;

    public TokenAuthentication(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
