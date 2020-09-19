package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Scope;

public class SamePatternPartSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternPartNeuron> {

    public static byte type;

    public SamePatternPartSynapse() {
        super();
    }

    public SamePatternPartSynapse(PatternPartNeuron input, PatternPartNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }
}
