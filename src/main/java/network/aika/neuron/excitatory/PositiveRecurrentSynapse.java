package network.aika.neuron.excitatory;



public class PositiveRecurrentSynapse extends ExcitatorySynapse<PatternNeuron, PatternPartNeuron> {

    public static byte type;

    public PositiveRecurrentSynapse() {
        super();
    }

    public PositiveRecurrentSynapse(PatternNeuron input, PatternPartNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }
}
