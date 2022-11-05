import com.sun.nio.sctp.SctpChannel;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelThread extends Thread {
    SctpChannel sc;
    final Mutex mutex;
    int connected_id;
    Logger logger;

    public ChannelThread(SctpChannel sc, Mutex mutex, int connected_id) {
        this.sc = sc;
        this.mutex = mutex;
        this.connected_id = connected_id;
        logger = Logger.getLogger("App");
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
                    Request req = (Request) message.message;
                    mutex.pq.put(req);
                    logger.log(Level.CONFIG, "Received request from " + message.sender);
                    // Send reply
                    Message reply = new Message(mutex.nodeID, MessageType.reply, "REPLY", mutex.logClock.get());
                    reply.send(sc);
                    mutex.updateClock();
                    logger.log(Level.CONFIG, "Sent reply to " + message.sender);
                }
                else if (MessageType.release == message.msgType) {
                    logger.log(Level.CONFIG, "Received release from " + message.sender + " removing from queue");
                    Request req = (Request) message.message;
                    if (!mutex.pq.remove(req)) {
                        logger.log(Level.WARNING, "Request not found in queue " + req);
                    }
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
                else if (MessageType.terminate == message.msgType) {
                    synchronized (mutex) {
                        mutex.respTime.addAndGet((long) message.message);
                        mutex.numAlive.getAndDecrement();
                        mutex.canTerminate.set(true);
                        mutex.notify();
                    }
                }
                if (mutex.requestTime.get() < message.clock) {
                    logger.log(Level.CONFIG, "Received message with higher clock value from " + message.sender);
                    mutex.higherTimestamp.add(message.sender);
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            }
        } catch (ClosedChannelException e){
            logger.log(Level.CONFIG, "Received all messages");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
