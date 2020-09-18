package network.aika.neuron.excitatory;

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
}
