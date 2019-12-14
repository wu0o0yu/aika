package network.aika.neuron.input;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Fired;


public class InputNeuron extends TNeuron<Synapse> {

    public static final String TYPE_STR = "IN";


    private InputNeuron() {
        super();
    }


    public InputNeuron(Neuron p) {
        super(p);
    }


    public InputNeuron(Model model, String label) {
        super(model, label);
    }


    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }


    public boolean isWeak(Synapse s, Synapse.State state) {
        return false;
    }


    public String getType() {
        return TYPE_STR;
    }

    @Override
    public ActivationFunction getActivationFunction() {
        return ActivationFunction.NULL_FUNCTION;
    }


    @Override
    public double getTotalBias(Synapse.State state) {
        return 0;
    }

    @Override
    public void dumpStat() {
        System.out.println(getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")  Rel:" + getReliability());
    }


    public boolean isMature(Config c) {
        return posFrequency > c.getMaturityThreshold();
    }


    public String typeToString() {
        return "INPUT";
    }

}
