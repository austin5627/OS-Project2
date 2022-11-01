import com.sun.nio.sctp.SctpChannel;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class ChannelThread extends Thread {
    SctpChannel sc;
    final Mutex mutex;
    int connected_id;

    public ChannelThread(SctpChannel sc, Mutex mutex, int connected_id) {
        this.sc = sc;
        this.mutex = mutex;
        this.connected_id = connected_id;
    }

    public void run() {
        try {

            ByteBuffer buf = ByteBuffer.allocateDirect(Message.MAX_MSG_SIZE); // Messages are received over SCTP using ByteBuffer
            sc.configureBlocking(true); // Ensures that the channel will block until a message is received
            while (sc.isOpen()) {
                // listen for msg
                Message message = Message.receiveMessage(sc);
                if (message == null) {
                    continue;
                }
                if (message.msgType == MessageType.connect){
                    continue;
                }
                mutex.updateClock(message.clock);
                if (MessageType.request == message.msgType) {
                    Request req = new Request(message.sender, message.clock);
                    mutex.pq.put(req);
                    System.out.println("Received request from " + message.sender);
                    // Send reply
                    Message reply = new Message(mutex.nodeID, MessageType.reply, "REPLY", mutex.logClock.get());
                    reply.send(sc);
                    mutex.updateClock();
                    System.out.println("Sent reply to " + message.sender);
                }
                else if (MessageType.release == message.msgType) {
                    //System.out.println("Received release from " + message.sender + " removing from queue");
                    mutex.pq.remove();
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
                else if (MessageType.terminate == message.msgType) {
                    synchronized (mutex) {
                        mutex.numAlive.getAndDecrement();
                        mutex.canTerminate.set(true);
                        mutex.notify();
                    }
                }
                if (mutex.requestTime.get() < message.clock) {
                    // System.out.println("Received message with higher clock value from " + message.sender);
                    mutex.higherTimestamp.add(message.sender);
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            }
        } catch (ClosedChannelException e){
            System.out.println("Received all messages");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
