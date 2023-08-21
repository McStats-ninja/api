package ninja.mcstats.api.packages.client.network;

import java.io.Serializable;

public class NetworkJoinRequest implements Serializable {

    private final String key;

    public NetworkJoinRequest(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
