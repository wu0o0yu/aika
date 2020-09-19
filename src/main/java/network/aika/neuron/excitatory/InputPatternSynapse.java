package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Scope;

public class InputPatternSynapse extends ExcitatorySynapse<PatternNeuron, PatternPartNeuron> {

    public static byte type;

    public InputPatternSynapse() {
        super();
    }

    public InputPatternSynapse(PatternNeuron input, PatternPartNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public Scope transition(Scope s, Direction dir) {
        return s.getNext(dir);
    }
}
