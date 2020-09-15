package network.aika.neuron.excitatory;

public class PatternPartSynapse extends ExcitatorySynapse<PatternPartNeuron, ExcitatoryNeuron> {

    public static byte type;

    public PatternPartSynapse() {
        super();
    }

    public PatternPartSynapse(PatternPartNeuron input, ExcitatoryNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }
}
