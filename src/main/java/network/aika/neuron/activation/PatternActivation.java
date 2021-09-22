package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.excitatory.PatternNeuron;

import java.util.Map;
import java.util.TreeMap;

public class PatternActivation extends Activation<PatternNeuron> {

    public static PatternActivation MIN_PATTERN_ACT = new PatternActivation(0, null);
    public static PatternActivation MAX_PATTERN_ACT = new PatternActivation(Integer.MAX_VALUE, null);

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
        addBindingSignal(this, (byte) 0);
    }


    @Override
    protected Activation newInstance() {
        return new PatternActivation(id, thought, neuron);
    }

    @Override
    public byte getType() {
        return 0;
    }



    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }
}
