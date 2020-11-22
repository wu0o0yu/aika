package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class PatternSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
        super();
    }

    public PatternSynapse(I input, PatternNeuron output) {
        super(input, output);
    }

    /*
    @Override
    public boolean inductionRequired(Class<? extends Neuron> type) {
        return false;
    }
*/

    public Activation getOutputActivationToLink(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron()) {
            return null;
        }

        return oAct;
    }

    @Override
    public void transition(Visitor v, Activation fromAct, Activation nextAct, boolean create) {
        Visitor nv = v.prepareNextStep();
        nv.incrementPathLength();

        next(null, nextAct, nv, create);
//        nv.follow(nextAct);
    }

    @Override
    public byte getType() {
        return type;
    }
}
