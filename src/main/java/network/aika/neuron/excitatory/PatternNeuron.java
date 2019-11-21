package network.aika.neuron.excitatory;

public class PatternNeuron extends ExcitatoryNeuron {


    @Override
    public boolean isRecurrent(boolean isNegativeSynapse) {
        return true;
    }
}
