package network.aika.neuron.input;

import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.Config;
import network.aika.neuron.TNeuron;

import java.util.Arrays;
import java.util.List;

import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;

public class InputNeuron extends TNeuron {

    private List<Integer> outputSlots = Arrays.asList(BEGIN, END);


    public InputNeuron(Model model, String label) {
        super(model, label, null, INeuron.Type.INPUT, ActivationFunction.NULL_FUNCTION);
    }


    public boolean isMature(Config c) {
        return posFrequency > c.getMaturityThreshold();
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
