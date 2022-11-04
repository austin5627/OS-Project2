import java.io.*;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

public class App {

    public int MEAN_INTER_REQUEST_DELAY;
    public int MEAN_CS_EXECUTION_TIME;
    public int NUM_REQUESTS;
    public static final String LOGFILE = "./LOCK.txt";
    public final Mutex mutex;
    public final int nodeID;
    public final int portNum;
    InetSocketAddress[] neighbors;


    public static void main(String[] args) {
        String config_file = args[0];
        int id = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        App app = new App(config_file, id, port);
        if (app.nodeID == 0) {
            File file = new File(LOGFILE);
            file.delete();
        }
        app.start();
    }

    public App(String config_file, int nodeID, int portNum) {
        System.out.println("Starting node " + nodeID + " on port " + portNum);
        load_config(config_file, nodeID);
        this.nodeID = nodeID;
        this.portNum = portNum;
        mutex = new Mutex(neighbors.length, nodeID, neighbors, portNum);
        System.out.println("Node " + nodeID + " is up and running");
    }

    public void load_config(String filename, int nodeID) {
        File configFile = new File(filename);
        System.out.println(configFile.exists());
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))){
            String line = br.readLine();
            while(line.trim().isEmpty() || line.trim().startsWith("#") || !Pattern.matches("^\\d.*", line.trim())) {
                line = br.readLine();
            }
            Scanner scanner = new Scanner(line);
            int num_nodes = scanner.nextInt();
            neighbors = new InetSocketAddress[num_nodes];
            MEAN_INTER_REQUEST_DELAY = scanner.nextInt();
            MEAN_CS_EXECUTION_TIME = scanner.nextInt();
            NUM_REQUESTS = scanner.nextInt();
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
        int error_request = (int) (Math.random() * NUM_REQUESTS);
        while (requests < NUM_REQUESTS) {
            int delay = (int) (Math.log(1.0 - Math.random()) * -MEAN_INTER_REQUEST_DELAY);
            System.out.println("Non-critical section delay: " + delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
            int cs_execution_time = (int) (Math.log(1.0 - Math.random()) * -MEAN_CS_EXECUTION_TIME);
            requests++;
            System.out.println("Requesting to enter Critical Section for " + cs_execution_time + "ms");
            if (requests == error_request && nodeID == 0) {
                System.out.println("\033[47;41mERROR: entering critical section without permission during request " + requests + "\033[0m");
            } else {
                mutex.cs_enter();
            }
            System.out.println("\033[47;41mEntering Critical Section\033[0m " + requests + " at " + System.currentTimeMillis());
            try{
                BufferedWriter writer = new BufferedWriter(new FileWriter(LOGFILE, true));
                writer.write(nodeID + " " + requests + " ENTER\n");
                writer.close();
                try {
                    Thread.sleep(cs_execution_time);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                writer = new BufferedWriter(new FileWriter(LOGFILE, true));
                writer.write(nodeID + " " + requests + " EXIT\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
            if (requests == error_request && nodeID == 0) {
                System.out.println("\033[47;42mLeaving Critical Section after illegal enter during request " + requests + "\033[0m");
            } else {
                mutex.cs_leave();
            }
            System.out.println("\033[47;42mLeaving Critical Section\033[0m  " + requests + " at " + System.currentTimeMillis());
        }
        System.out.println("\033[0;44mFinished all requests\033[0m");
        mutex.terminate();
        if (nodeID == 0) {
            System.out.println("Critical Section is mutually exclusive: " + checkLog());
        }
    }

    public boolean checkLog(){
        try {
            BufferedReader in = new BufferedReader(new FileReader(LOGFILE));
            String line;
            int[] last_request = new int[mutex.numProc];
            for (int i = 0; i < mutex.numProc; i++) {
                last_request[i] = 0;
            }
            while ((line = in.readLine()) != null) {
                String nextLine = in.readLine();
                Scanner s1 = new Scanner(line);
                Scanner s2 = new Scanner(nextLine);
                int node_l1 = s1.nextInt();
                int node_l2 = s2.nextInt();
                int request_l1 = s1.nextInt();
                int request_l2 = s2.nextInt();
                if (node_l1 != node_l2 || request_l1 != request_l2 || request_l1 != last_request[node_l1] + 1
                        || !s1.next().equals("ENTER") || !s2.next().equals("EXIT")) {
                    System.out.println("Problem with line " + line + " and " + nextLine);
                    return false;
                }
                last_request[node_l1] = request_l1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        return true;
    }
}
