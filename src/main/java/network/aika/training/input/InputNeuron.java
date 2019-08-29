package network.aika.training.input;

import network.aika.ActivationFunction;
import network.aika.neuron.INeuron;
import network.aika.training.MetaModel;
import network.aika.training.TNeuron;
import network.aika.training.excitatory.ExcitatoryNeuron;

import java.util.Arrays;
import java.util.List;

import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;

public class InputNeuron extends TNeuron {

    private List<Integer> outputSlots = Arrays.asList(BEGIN, END);


    public InputNeuron(MetaModel model, String label) {
        super(model, label, null, INeuron.Type.INPUT, ActivationFunction.NULL_FUNCTION);
    }


    public boolean isMature() {
        return posFrequency > ExcitatoryNeuron.MATURITY_THRESHOLD;
    }


    public void setOutputSlots(Integer... slots) {
        outputSlots = Arrays.asList(slots);
    }


    public List<Integer> getOutputSlots() {
        return outputSlots;
    }


    public String typeToString() {
        return "INPUT";
    }
}
