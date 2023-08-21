package ninja.mcstats.api.models;

public class Error {

    private int id;
    private String message;

    public Error(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public int id() {
        return id;
    }

    public String message() {
        return message;
    }
}
