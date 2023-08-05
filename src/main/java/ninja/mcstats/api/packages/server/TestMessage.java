package ninja.mcstats.api.packages.server;

import java.io.Serializable;

public class TestMessage implements Serializable {

    private final String message;

    public TestMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
