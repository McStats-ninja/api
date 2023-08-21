package ninja.mcstats.api.packages.client.session;

import java.io.Serializable;
import java.util.HashMap;

public class SessionIniate implements Serializable {

    private final HashMap<String, String> data;

    public SessionIniate(HashMap<String, String> data) {
        this.data = data;
    }

    public HashMap<String, String> data() {
        return data;
    }
}
