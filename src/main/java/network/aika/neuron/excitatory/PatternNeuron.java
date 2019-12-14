package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;

public class PatternNeuron extends ExcitatoryNeuron {

    public static final String TYPE_STR = "P";

    public PatternNeuron(Neuron p) {
        super(p);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }
}
