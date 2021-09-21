package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.excitatory.PatternNeuron;

import java.util.Map;
import java.util.TreeMap;

public class PatternActivation extends Activation<PatternNeuron> {

    protected Map<Activation, Byte> reverseBindingSignals = new TreeMap<>();

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
        addPatternBindingSignal(this, (byte) 0);
    }

    public Map<Activation, Byte> getReverseBindingSignals() {
        return reverseBindingSignals;
    }

    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }
}
