package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.scopes.ScopeEntry;
import network.aika.neuron.activation.direction.Direction;

import java.util.Collections;
import java.util.Set;


import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

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

    /*
    @Override
    public Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement) {
        if (checkFinalRequirement) {
            if (dir == INPUT && s.getScope() != PP_SAME) {
                return Collections.emptySet();
            }
            if (dir == OUTPUT && s.getScope() != PP_RELATED_SAME && s.getScope() != PP_SAME) {
                return Collections.emptySet();
            }
        }

        switch (s.getScope()) {
            case PP_SAME:
                return s.nextSet(PP_RELATED_SAME, PP_SAME);
            case PP_RELATED_SAME:
                return s.nextSet(PP_SAME);
            case PP_RELATED_INPUT:
            case PP_INPUT:
            case P_SAME:
                return Collections.singleton(s);
        }

        return Collections.emptySet();
    }
     */
}
