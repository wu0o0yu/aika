package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class InputPPSynapse<I extends Neuron<?>> extends PatternPartSynapse<I> {

    public InputPPSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);
    }


    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.startDir == OUTPUT &&
                getOutput().computeBiasLB(act) >= 0.4 &&
                act.getNeuron() instanceof PatternNeuron;
    }

    @Override
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        assert input.getTemplates().contains(getInput());

        PatternPartSynapse s = new InputPPSynapse(input, output, this);
        initFromTemplate(s);
        return s;
    }
}
