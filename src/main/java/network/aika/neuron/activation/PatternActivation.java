package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.steps.activation.Linking;
import network.aika.neuron.steps.activation.TemplateLinking;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class PatternActivation extends Activation<PatternNeuron> {

    public static PatternActivation MIN_PATTERN_ACT = new PatternActivation(0, null);
    public static PatternActivation MAX_PATTERN_ACT = new PatternActivation(Integer.MAX_VALUE, null);

    protected PatternActivation(int id, PatternNeuron n) {
        super(id, n);
    }

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }


    @Override
    protected Activation newInstance() {
        return new PatternActivation(id, thought, neuron);
    }

    @Override
    public byte getType() {
        return 0;
    }

    protected void registerBindingSignal(Activation targetAct, Byte scope) {
        super.registerBindingSignal(targetAct, scope);
        Linking.add(targetAct, this, scope);
        TemplateLinking.add(targetAct, this, scope, List.of(INPUT, OUTPUT));
    }

    public boolean isSelfRef(Activation iAct) {
        return reverseBindingSignals.containsKey(iAct);
    }
}
