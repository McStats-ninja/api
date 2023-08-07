package ninja.mcstats.api;

import ninja.mcstats.api.packages.client.auth.TokenAuthentication;
import ninja.mcstats.api.packages.server.TestMessage;
import ninja.mcstats.api.packages.server.auth.ValidatedTokenAuthentication;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class McStatsNinja {

    private static final McStatsNinja INSTANCE = new McStatsNinja();
    protected static HashMap<String, Ninja> ninjas = new HashMap<>();
    private State state = State.UNINITIALIZED;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Thread connection = new Thread(this::connect);

    protected McStatsNinja() {
        state = State.LOADING;
        connection.start();
        while (state != State.INITIALIZED) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Ninja init(String key) {
        return ninjas.getOrDefault(key, new Ninja(key));
    }

    public void handle(Object object) {
        if (object == null) return;
        if (object instanceof ValidatedTokenAuthentication) {
            System.out.println("Token validiert.");
            state = State.INITIALIZED;
            return;
        }
        if (object instanceof String message) {
            System.out.println("Empfangene Nachricht: " + message);
        } else if (object instanceof TestMessage testMessage) {
            System.out.println("Empfangene Test Nachricht: " + testMessage.getMessage());
        } else {
            System.out.println("Ungültiges Objekt empfangen.");
        }
    }

    public void send(Object object) {
        try {
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        while (true) {
            try {
                Socket bestSocket = null;
                long lowestPing = Long.MAX_VALUE;

                try {
                    Hashtable<String, String> env = new Hashtable<>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                    DirContext ctx = new InitialDirContext(env);
                    Attributes attrsTxt = ctx.getAttributes("mcstats.ninja", new String[]{"TXT"});
                    System.out.println("TXT: " + attrsTxt);
                    Attributes attrs = ctx.getAttributes("_bacon._tcp.mcstats.ninja", new String[]{"SRV"});
                    int maxPing = 5;
                    if (attrs != null) {
                        Attribute srvAttr = attrs.get("SRV");
                        if (srvAttr != null) {
                            while (bestSocket == null) {
                                System.out.println("MaxPing: " + maxPing);
                                for (int i = 0; i < srvAttr.size(); i++) {
                                    System.out.println("-----------------------");
                                    String srvRecord = (String) srvAttr.get(i);
                                    String[] parts = srvRecord.split(" ");

                                    int priority = Integer.parseInt(parts[0]);
                                    int weight = Integer.parseInt(parts[1]);
                                    int port = Integer.parseInt(parts[2]);
                                    String hostname = parts[3];

                                    System.out.println("Priority: " + priority);
                                    System.out.println("Weight: " + weight);
                                    System.out.println("Port: " + port);
                                    System.out.println("Hostname: " + hostname);

                                    System.out.println("Pinge " + hostname + ":" + port + "...");

                                    long startTime = System.currentTimeMillis();
                                    try {
                                        Socket socket1 = new Socket();
                                        socket1.connect(new InetSocketAddress(hostname, port), maxPing);
                                        long endTime = System.currentTimeMillis();
                                        long time = endTime - startTime;
                                        long pingTime = (time > maxPing ? -1 : time);
                                        System.out.println("Ping: " + pingTime);
                                        if (pingTime >= 0 && pingTime < lowestPing) {
                                            if (bestSocket != null) bestSocket.close();
                                            lowestPing = pingTime;
                                            bestSocket = socket1;
                                        } else {
                                            socket1.close();
                                        }
                                    } catch (IOException ignored) {
                                        System.out.println("Ping: zu hoch!");
                                    }
                                    System.out.println("-----------------------");
                                }
                                maxPing += 50;
                            }
                        } else {
                            System.out.println("Kein SRV-Eintrag gefunden.");
                        }
                    }
                } catch (NamingException e) {
                    e.printStackTrace();
                }
                if (bestSocket != null) {
                    System.out.println("Bester Server: " + bestSocket.getInetAddress().getHostAddress() + ":" + bestSocket.getPort() + " mit einem Ping von " + lowestPing + " ms");
                    socket = bestSocket;
                    try {
                        System.out.println("Verbunden mit Server.");

                        out = new ObjectOutputStream(socket.getOutputStream());
                        in = new ObjectInputStream(socket.getInputStream());

                        send(new TokenAuthentication("test"));

                        while (true) {
                            try {
                                Object receivedObject = in.readObject();
                                handle(receivedObject);
                            } catch (EOFException ignored) {
                            }
                        }
                    } catch (SocketException e) {
                        System.out.println(e.getMessage());
                        state = State.ERROR;
                        System.out.println("Verbindung zum Server fehlgeschlagen. Automatischer Reconnect in 5 Sekunden...");
                        TimeUnit.SECONDS.sleep(5);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        // Hier solltest du sicherstellen, dass die Socket-Verbindung geschlossen wird
                        if (socket != null && !socket.isClosed()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    System.out.println("Kein Server erreichbar.");
                    System.out.println("Verbindung zum Server fehlgeschlagen. Automatischer Reconnect in 60 Sekunden...");
                    TimeUnit.MINUTES.sleep(1);
                }


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
