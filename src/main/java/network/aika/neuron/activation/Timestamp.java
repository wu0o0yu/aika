package network.aika.neuron.activation;

import java.util.Comparator;

public class Timestamp implements Comparable<Timestamp> {

    public static Timestamp NOT_SET = new Timestamp(Long.MIN_VALUE);

    public static Timestamp MIN = new Timestamp(0);
    public static Timestamp MAX = new Timestamp(Long.MAX_VALUE);

    public static Comparator<Timestamp> NOT_SET_AFTER = Comparator
            .<Timestamp>comparingInt(ts -> ts == NOT_SET ? 1 : 0)
            .thenComparingLong(Timestamp::getTimestamp);

    private long timestamp;

    public Timestamp(long ts) {
        this.timestamp = ts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        if(this == NOT_SET)
            return "NOT_SET";

        if(this == MIN)
            return "MIN";

        if(this == MAX)
            return "MAX";

        return "" + timestamp;
    }

    @Override
    public int compareTo(Timestamp ts) {
        return Long.compare(timestamp, ts.timestamp);
    }
}
