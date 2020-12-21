package network.aika.neuron.phase;

public class RankedImpl implements Ranked {

    private int rank;

    public RankedImpl(int rank) {
        this.rank = rank;
    }

    @Override
    public int getRank() {
        return rank;
    }
}
