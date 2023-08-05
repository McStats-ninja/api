package ninja.mcstats.api;

import ninja.mcstats.api.exception.UnknownShurikenKeyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class Ninja {

    private final List<String> knownShurikenKeys = new ArrayList<>();
    private final HashMap<String, Shuriken<?>> shurikens = new HashMap<>();
    private State state = State.UNINITIALIZED;

    protected Ninja(String key) {
        McStatsNinja.ninjas.put(key, this);
        state = State.LOADING;
    }

    public void addShuriken(String key, Shuriken<?> shuriken) throws UnknownShurikenKeyException {
        if (!knownShurikenKeys.contains(key))
            throw new UnknownShurikenKeyException("Unknown Shuriken Key: " + key);
        shurikens.put(key, shuriken);
    }


    public static class Shuriken<T> {
        private final Supplier<T> value;

        public Shuriken(Supplier<T> value) {
            this.value = value;
        }
    }


}
