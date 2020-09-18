package network.aika.neuron.excitatory;

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
}
