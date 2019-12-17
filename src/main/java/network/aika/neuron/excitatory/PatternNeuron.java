package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.Neuron;

public class PatternNeuron extends ExcitatoryNeuron {

    public static final String TYPE_STR = "P";

    public PatternNeuron(Neuron p) {
        super(p);
    }


    public PatternNeuron(Model model, String label) {
        super(model, label);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }
}
