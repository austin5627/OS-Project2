import com.sun.nio.sctp.SctpChannel;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Mutex extends Thread {
    public final AtomicInteger logClock = new AtomicInteger(0);
    public final AtomicInteger requestTime = new AtomicInteger(0);
    public AtomicInteger numAlive;
    public final AtomicBoolean canTerminate = new AtomicBoolean(false);
    public final PriorityBlockingQueue<Request> pq = new PriorityBlockingQueue<>();
    public final Set<Integer> higherTimestamp = ConcurrentHashMap.newKeySet();

    public final int numProc;
    public final int nodeID;
    public final int port;
    private final ConcurrentHashMap<Integer, SctpChannel> channelMap = new ConcurrentHashMap<>();

    public Mutex(int numProc, int nodeID, InetSocketAddress[] addresses, int port) {
        this.nodeID = nodeID;
        this.numProc = numProc;
        this.numAlive = new AtomicInteger(numProc);
        this.port = port;
        initialize_connections(addresses);
    }

    public void initialize_connections(InetSocketAddress[] addresses) {
        AcceptThread acceptThread = new AcceptThread(this, port);
        acceptThread.start();
        int i = 0;
        while (i < nodeID) {
            try {
                SctpChannel channel = SctpChannel.open(addresses[i], 1, 1);
                Message msg = new Message(nodeID, MessageType.connect, "Connecting from " + nodeID, logClock.get());
                msg.send(channel);
                channelMap.put(i, channel);
                ChannelThread channelThread = new ChannelThread(channel, this, i);
                channelThread.start();
                System.out.println("Established connection with " + i);
                i++;
            } catch (ConnectException e) {
                System.out.println("Connection refused from " + i + ", retrying in 1 second...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.exit(0);
                }
            } catch (Exception e){
                e.printStackTrace();
                System.exit(0);
            }
        }
        System.out.println("All outgoing connections established");
        try {
            acceptThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("All incoming connections established");
        System.out.print("All connections: ");
        for (int k : channelMap.keySet()) {
            System.out.print(k + " ");
        }
        System.out.println();
    }

    public void broadcast(Message msg) {
        for (SctpChannel channel : channelMap.values()) {
            try {
                if (channel.isOpen()) {
                    msg.send(channel);
                } else {
                    System.out.println("Channel is closed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cs_enter() {
        Request req = new Request(nodeID, logClock.incrementAndGet());
        requestTime.set(req.clock);
        higherTimestamp.clear();
        pq.put(req);
        Message reqMsg = new Message(nodeID, MessageType.request, req, req.clock);
        broadcast(reqMsg);
        while(higherTimestamp.size() < numProc - 1 || (pq.peek() != null && pq.peek().compareTo(req) != 0)) {
            try {
                synchronized(this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    public void cs_leave() {
        try {
            Request req = pq.take();
            Message releaseMsg = new Message(nodeID, MessageType.release, req, logClock.incrementAndGet());
            broadcast(releaseMsg);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void terminate() {
        // Broadcast terminate message to all nodes
        Message terminateMsg = new Message(nodeID, MessageType.terminate, "TERMINATE", logClock.incrementAndGet());
        if (nodeID == 0)  {
            while (numAlive.get() > 1) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
            broadcast(terminateMsg);
        } else {
            terminateMsg.send(channelMap.get(0));
            try {
                synchronized (this) {
                    while(!canTerminate.get()) {
                        wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        closeConnections();
    }

    public void closeConnections() {
        System.out.println("Closing connections");
        for (SctpChannel c : channelMap.values()) {
            try {
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addConnection(int nodeID, SctpChannel channel) {
        channelMap.put(nodeID, channel);
    }

    public void updateClock(int msgClock) {
        // Since the get and the set are separate, I am not sure if this is safe
        logClock.set(Math.max(logClock.get(), msgClock) + 1);
    }

    public void updateClock() {
        logClock.getAndIncrement();
    }
}
