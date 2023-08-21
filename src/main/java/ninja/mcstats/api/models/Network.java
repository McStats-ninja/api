package ninja.mcstats.api.models;

public class Network {

    private final String name;

    public Network(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
