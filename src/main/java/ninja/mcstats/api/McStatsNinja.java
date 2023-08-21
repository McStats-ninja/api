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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.mcstats.api.McStatsNinja.State.*;

public class McStatsNinja {

    private static final McStatsNinja INSTANCE = new McStatsNinja();
    protected static HashMap<String, Ninja> ninjas = new HashMap<>();
    private State state = State.UNINITIALIZED;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Thread heartbeat = new Thread(() -> {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(30);
                logger.info("Connected: " + (socket == null ? "null" : socket.isConnected()));
                logger.info("Closed: " + (socket == null ? "null" : socket.isClosed()));
                logger.info("Output: " + (out != null));
                logger.info("Input: " + (in != null));
                logger.info("Sende Hearthbeat...");
                send(new Hearthbeat());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });
    private final Thread connection = new Thread(this::connect);

    private Network network;

    protected McStatsNinja() {
        state = LOADING;
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
        if (object instanceof SessionCancelled cancelled) {
            System.out.println("Session abgebrochen: " + cancelled.reason());
        }
        if (object instanceof SessionInitiated initiated) {
            System.out.println("Session gestartet: " + initiated.session());
        }
        if (object instanceof String message) {
            logger.info("Empfangene Nachricht: " + message);
        } else if (object instanceof TestMessage testMessage) {
            logger.info("Empfangene Test Nachricht: " + testMessage.getMessage());
        } else {
            logger.warn("Ungültiges Objekt empfangen.");
        }
    }

    public void send(Object object) {
        if (out == null) return;
        logger.info("Sende Objekt: " + object.toString());
        try {
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            logger.warn("Fehler beim Senden des Objekts: " + object + " ( " + e.getLocalizedMessage() + " )");
            try {
                socket.close();
                socket = null;
                in.close();
                in = null;
                out.close();
                out = null;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private ArrayList<SrvRecord> getRecords() {
        ArrayList<SrvRecord> records = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_bacon._tcp.mcstats.ninja", new String[]{"SRV"});
            if (attrs != null) {
                Attribute srvAttr = attrs.get("SRV");
                if (srvAttr != null) {
                    for (int i = 0; i < srvAttr.size(); i++) {
                        String srvRecord = (String) srvAttr.get(i);
                        String[] parts = srvRecord.split(" ");

                        int priority = Integer.parseInt(parts[0]);
                        int weight = Integer.parseInt(parts[1]);
                        int port = Integer.parseInt(parts[2]);
                        String hostname = parts[3];
                        SrvRecord record = new SrvRecord(priority, weight, port, hostname);
                        records.add(record);
                        logger.info("SRV-Eintrag gefunden: " + record + " ( " + records.size() + " ) ");
                    }
                } else {
                    logger.warn("Kein SRV-Eintrag gefunden.");
                }
                return records;
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>(0);
    }

    private Socket getBestServer() {
        AtomicReference<Socket> bestSocket = new AtomicReference<>(null);

        while (bestSocket.get() == null) {
            List<SrvRecord> records = getRecords();
            logger.info("Suche nach Server...");

            while (records.isEmpty()) {
                logger.warn("Keine Server Records gefunden. Automatischer Reconnect in 60 Sekunden...");
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                records = getRecords();
            }

            AtomicInteger bestPing = new AtomicInteger(Integer.MAX_VALUE);
            List<Thread> connectionThreads = new ArrayList<>();

            for (int i = 0; i < records.size(); i++) {
                logger.info("Verbinde mit Server " + (i + 1) + "/" + records.size());
                SrvRecord record = records.get(i);
                Thread thread = new Thread(() -> {
                    logger.info("Verbinde mit Server: " + record.hostname() + ":" + record.port());
                    try {
                        Socket tempSocket = new Socket();
                        long startTime = System.currentTimeMillis();
                        tempSocket.connect(new InetSocketAddress(record.hostname(), record.port()), 5000);
                        long endTime = System.currentTimeMillis();
                        long time = endTime - startTime;
                        long pingTime = (time > 5000 ? -1 : time);
                        logger.info("Ping für " + record.hostname() + ":" + record.port() + ": " + pingTime);
                        if (pingTime >= 0 && pingTime < bestPing.get()) {
                            bestPing.set((int) pingTime);
                            bestSocket.set(tempSocket);
                            logger.info("Server " + record.hostname() + ":" + record.port() + " ist der beste Server.");
                            // Hier Abbruch der anderen Threads
                            connectionThreads.forEach(Thread::interrupt);
                        } else {
                            tempSocket.close();
                        }
                    } catch (IOException e) {
                        logger.warn("Verbindung zu " + record.hostname() + " ist fehlgeschlagen.");
                    }
                });
                connectionThreads.add(thread);
                thread.start();
            }

            // Warte auf das Ende aller Threads
            for (Thread thread : connectionThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (bestSocket.get() == null) {
                logger.warn("Kein Server erreichbar. Automatischer Reconnect in 60 Sekunden...");
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bestSocket.get();
    }

    private void connect() {
        while (true) {
            try {
                socket = getBestServer();
                try {
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
                }
            } catch (IOException | InterruptedException e) {
                logger.warn("AHHHHHHHHHHHH 1");
                e.printStackTrace();
            }
        }
    }
}
