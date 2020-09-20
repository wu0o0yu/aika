package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Context;
import network.aika.neuron.activation.Direction;

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

    public Context transition(Context c) {
        return new Context(c, true);
    }
}
