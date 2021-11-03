package network.aika.neuron.activation;

public class Timestamp implements Comparable<Timestamp> {

    public static Timestamp NOT_SET = new Timestamp(-1);

    public static Timestamp MIN = new Timestamp(0);
    public static Timestamp MAX = new Timestamp(Long.MAX_VALUE);

    private long timestamp;

    public Timestamp(long ts) {
        this.timestamp = ts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return this == NOT_SET ? "[NOT_SET]" : "" + timestamp;
    }

    @Override
    public int compareTo(Timestamp ts) {
        return Long.compare(timestamp, ts.timestamp);
    }
}
