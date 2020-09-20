package network.aika.neuron.excitatory;


import network.aika.neuron.activation.Context;

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

    public Context transition(Context c) {
        return new Context(c, true);
    }
}
