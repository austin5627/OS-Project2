import com.sun.nio.sctp.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class AcceptThread extends Thread {
    private final Mutex mutex;
    private final int portNum;
    public boolean acceptNew = true;

    public AcceptThread(Mutex mutex, int portNum) {
        this.mutex = mutex;
        this.portNum = portNum;
    }

    public void run() {
        try (SctpServerChannel ssc = SctpServerChannel.open()){
            InetSocketAddress addr = new InetSocketAddress(portNum); // Get address from port number
            ssc.bind(addr);//Bind server channel to address
            while (acceptNew) {
                SctpChannel sc = ssc.accept();
                // Should get a message immediately from client with the nodeNum of the remote device
                Message message = Message.receiveMessage(sc);
                int connected_id = message.sender;
                ChannelThread ct = new ChannelThread(sc, mutex, connected_id);
                ct.start();
                acceptNew = !mutex.addConnection(connected_id, sc);
            }
            System.out.println("Finished accepting new connections");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

    }
}
