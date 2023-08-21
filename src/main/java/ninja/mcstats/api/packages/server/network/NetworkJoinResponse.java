package ninja.mcstats.api.packages.server.network;

import ninja.mcstats.api.models.Error;
import ninja.mcstats.api.models.Network;

import java.io.Serializable;

public class NetworkJoinResponse implements Serializable {

    private Error error;

    private Network network;

    public NetworkJoinResponse(Network network) {
        this.network = network;
    }

    public NetworkJoinResponse(Error error) {
        this.error = error;
    }


    public Error error() {
        return error;
    }

    public Network network() {
        return network;
    }
}
