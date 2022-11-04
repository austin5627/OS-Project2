import java.io.Serializable;
import java.util.Objects;

public class Request implements Comparable<Request>, Serializable {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return nodeID == request.nodeID && clock == request.clock;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeID, clock);
    }

    public String toString() {
        return "(ID:" + nodeID + ", C:" + clock + ")";
    }
}
