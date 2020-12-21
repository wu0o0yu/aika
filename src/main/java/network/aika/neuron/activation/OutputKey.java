package network.aika.neuron.activation;

import network.aika.neuron.NeuronProvider;

public class OutputKey implements Comparable<OutputKey> {

    private NeuronProvider n;
    private Integer actId;

    public OutputKey(NeuronProvider n, Integer actId) {
        this.n = n;
        this.actId = actId;
    }

    public String toString() {
        return "[" + n.getId() + "]:" + actId;
    }

    @Override
    public int compareTo(OutputKey ok) {
        int r = n.compareTo(ok.n);
        if(r != 0) return r;
        return actId.compareTo(ok.actId);
    }
}
