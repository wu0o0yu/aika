package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class SamePPSynapse<I extends Neuron<?>> extends PatternPartSynapse<I> {

    public SamePPSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);
    }

    public SamePPSynapse(I input, PatternPartNeuron output, Synapse template, boolean isRecurrent) {
        super(input, output, template);

        this.isRecurrent = isRecurrent;
    }

    @Override
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        assert input.getTemplates().contains(getInput());

        PatternPartSynapse s = new SamePPSynapse(input, output, this);
        initFromTemplate(s);
        return s;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if (v.startDir == INPUT)
            return !act.getNeuron().isInputNeuron() && isRecurrent;
        else
            return getOutput().computeBiasLB(act) >= 0.4;
    }
}
