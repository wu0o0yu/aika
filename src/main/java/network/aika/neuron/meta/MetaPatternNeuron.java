package network.aika.neuron.meta;

import network.aika.Model;
import network.aika.neuron.Neuron;

public class MetaPatternNeuron extends MetaNeuron {

    public static final String TYPE_STR = "MP";


    public MetaPatternNeuron(Neuron p) {
        super(p);
    }

    public MetaPatternNeuron(Model model, String label) {
        super(model, label);
    }

    @Override
    public String getType() {
        return TYPE_STR;
    }

}
