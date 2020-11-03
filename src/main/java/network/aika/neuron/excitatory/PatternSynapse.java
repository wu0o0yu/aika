package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.Visitor;

public class PatternSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
        super();
    }

    public PatternSynapse(I input, PatternNeuron output) {
        super(input, output);
    }

    @Override
    public boolean inductionRequired(Class<? extends Neuron> type) {
        return false;
    }

    @Override
    public Visitor transition(Visitor v) {
        Visitor nv = v.copy();
        nv.incrementPathLength();

        return nv;
    }

    @Override
    public byte getType() {
        return type;
    }
}
