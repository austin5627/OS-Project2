
public class Request implements Comparable<Request>{
    public final int nodeID;
    public final int clock;
    public Request(int nodeID, int clock) {
        this.nodeID = nodeID;
        this.clock = clock;
    }

    @Override
    public int compareTo(Request r) {
        if (r.clock == this.clock) {
            return Integer.compare(this.nodeID, r.nodeID);
        } else {
            return Integer.compare(this.clock, r.clock);
        }
    }

    public String toString() {
        return "(ID:" + nodeID + ", C:" + clock + ")";
    }
}
