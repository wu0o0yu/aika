package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class PatternSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
        super();
    }

    public PatternSynapse(I input, PatternNeuron output, Synapse template) {
        super(input, output, template);
    }

    public Synapse instantiateTemplate(I input, PatternNeuron output) {
        if(getInput() != input.getTemplate()) {
            return null;
        }
        return new PatternSynapse(input, output, this);
    }

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
    }

    @Override
    public byte getType() {
        return type;
    }
}
