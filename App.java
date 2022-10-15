import java.io.*;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.regex.Pattern;

public class App {

    public int MEAN_INTER_REQUEST_DELAY;
    public int MEAN_CS_EXECUTION_TIME;
    public int NUM_REQUESTS;
    public final Mutex mutex;
    public final int nodeID;
    public final int portNum;
    InetSocketAddress[] neighbors;


    public static void main(String[] args) {
        String config_file = args[0];
        int id = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        App app = new App(config_file, id, port);
        app.start();
    }

    public App(String config_file, int nodeID, int portNum) {
        load_config(config_file, nodeID);
        this.nodeID = nodeID;
        this.portNum = portNum;
        mutex = new Mutex(neighbors.length, nodeID, neighbors, portNum);
    }

    public void load_config(String filename, int nodeID) {
        File configFile = new File(filename);
        System.out.println(configFile.exists());
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))){
            String line = br.readLine();
            Scanner scanner = new Scanner(line);
            int num_nodes = scanner.nextInt();
            neighbors = new InetSocketAddress[num_nodes];
            MEAN_INTER_REQUEST_DELAY = scanner.nextInt();
            MEAN_CS_EXECUTION_TIME = scanner.nextInt();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#") || !Pattern.matches("^\\d.*", line.trim())) {
                    continue;
                }
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf('#'));
                }
                line = line.trim();
                scanner = new Scanner(line);
                int node_id = scanner.nextInt();
                if (node_id == nodeID) {
                    continue;
                }
                String hostname = scanner.next();
                int port = scanner.nextInt();
                InetSocketAddress address = new InetSocketAddress(hostname, port);
                neighbors[node_id] = address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        int requests = 0;
        while (requests < NUM_REQUESTS) {
            int delay = (int) Math.log(1 - Math.random()) * -MEAN_INTER_REQUEST_DELAY;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
            int cs_execution_time = (int) Math.log(1 - Math.random()) * -MEAN_CS_EXECUTION_TIME;
            requests++;
            System.out.println("Entering Critical Section for " + cs_execution_time + "ms");
            mutex.cs_enter();
            try(BufferedWriter writer = new BufferedWriter(new FileWriter("~/lock.txt", true))){
                writer.write(nodeID + "\n");
                try {
                    Thread.sleep(cs_execution_time);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                writer.write(nodeID + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
            mutex.cs_leave();
            System.out.println("Leaving Critical Section");
        }
    }
}
