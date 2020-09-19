package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Scope;

import static network.aika.neuron.activation.Direction.INPUT;

public class InputPatternPartSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternPartNeuron> {

    public static byte type;

    public InputPatternPartSynapse() {
        super();
    }

    public InputPatternPartSynapse(PatternPartNeuron input, PatternPartNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public Scope transition(Scope s, Direction dir) {
        if(dir == INPUT) {
            switch(s) {
                case SAME: return Scope.INPUT;
                case INPUT: return Scope.RELATED;
                default: throw new UnsupportedOperationException();
            }
        } else {

        }
    }
}
