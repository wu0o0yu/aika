package network.aika.neuron.excitatory;

import network.aika.neuron.inhibitory.InhibitoryNeuron;

public class NegativeRecurrentSynapse extends ExcitatorySynapse<InhibitoryNeuron, PatternPartNeuron> {

    public static byte type;

    public NegativeRecurrentSynapse() {
        super();
    }

    public NegativeRecurrentSynapse(InhibitoryNeuron input, PatternPartNeuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }
}
