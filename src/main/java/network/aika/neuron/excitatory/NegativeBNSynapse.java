package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


public class NegativeBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public NegativeBNSynapse(I input, BindingNeuron output) {
        super(input, output);

        this.isRecurrent = true;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.getTargetDir() == OUTPUT &&
                getOutput().computeBiasLB(act) >= 0.4;
    }

    public void updateReference(Link l) {
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !v.getSelfRef())
            return null;

        if (!v.getSelfRef()) {
            oAct = oAct.createBranch(this);
        }
        return oAct;
    }

    @Override
    public BindingNeuronSynapse instantiateTemplate(I input, BindingNeuron output) {
        assert input.getTemplateGroup().contains(getInput());

        BindingNeuronSynapse s = new NegativeBNSynapse(input, output);
        initFromTemplate(s);
        return s;
    }
}
