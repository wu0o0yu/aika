package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Visitor;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;


public class NegativePPSynapse<I extends Neuron<?>> extends PatternPartSynapse<I> {

    public NegativePPSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);

        this.isRecurrent = true;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.startDir == OUTPUT &&
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
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        assert input.getTemplates().contains(getInput());

        PatternPartSynapse s = new NegativePPSynapse(input, output, this);
        initFromTemplate(s);
        return s;
    }
}
