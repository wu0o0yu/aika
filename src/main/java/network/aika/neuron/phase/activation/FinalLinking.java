package network.aika.neuron.phase.activation;


public class FinalLinking extends Linking {

    public boolean isFinal() {
        return true;
    }

    @Override
    public int getRank() {
        return 4;
    }
}
