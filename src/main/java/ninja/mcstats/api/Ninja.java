package ninja.mcstats.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

public class Ninja {

    private HashMap<String, Shuriken<?>> shurikens = new HashMap<>();
    private ArrayList<String> knownShurikens = new ArrayList<>();

    private State state = State.UNINITIALIZED;

    protected Ninja(String key) {
        McStatsNinja.ninjas.put(key, this);
    }

    public Shuriken<?> shuriken(String key, Shuriken<?> shuriken) {
        shurikens.put(key, shuriken);
        return shurikens.get(key);
    }

    protected void knownShurikens(ArrayList<String> known) {
        this.knownShurikens = known;
    }

    public Optional<Shuriken<?>> shuriken(String key) {
        return shurikens.get(key) == null ? Optional.empty() : Optional.of(shurikens.get(key));
    }


    /*
     * Types:
     *  - Active: Based on events to send data
     *  - OTD: One time data
     *
     *
     */


    public static class Shuriken<T> {
        private final Supplier<T> value;

        public Shuriken(Supplier<T> value) {
            this.value = value;
        }

        public void send(T data) {

        }
    }

    enum State {
        LOADING,
        READY,
        UNINITIALIZED,
        INITIALIZED, ERROR
    }


}
