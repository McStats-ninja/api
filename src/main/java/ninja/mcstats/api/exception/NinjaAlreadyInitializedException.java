package ninja.mcstats.api.exception;

import ninja.mcstats.api.Ninja;

public class NinjaAlreadyInitializedException extends Exception{

    private final Ninja ninja;
    public NinjaAlreadyInitializedException(String message, Ninja ninja){
        super(message);
        this.ninja = ninja;
    }

    public Ninja getNinja() {
        return ninja;
    }
}
