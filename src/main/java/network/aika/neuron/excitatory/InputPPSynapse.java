package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.scopes.ScopeEntry;
import network.aika.neuron.activation.direction.Direction;

import java.util.Collections;
import java.util.Set;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class InputPPSynapse<I extends Neuron<?>> extends PatternPartSynapse<I> {

    public InputPPSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);
    }

    @Override
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        assert input.getTemplates().contains(getInput());

        PatternPartSynapse s = new InputPPSynapse(input, output, this);
        initFromTemplate(s);
        return s;
    }

    /*
    @Override
    public Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement) {
        if (dir == INPUT) {
            if (checkFinalRequirement && s.getScope() != PP_SAME) {
                return Collections.emptySet();
            }

            switch (s.getScope()) {
                case PP_SAME:
                    return s.nextSet(PP_INPUT);
                case PP_RELATED_SAME:
                case PP_INPUT:
                    return s.nextSet(PP_RELATED_INPUT);
                case I_SAME:
                    return s.nextSet(I_INPUT);
            }
        } else {
            if (checkFinalRequirement && s.getScope() != PP_INPUT) {
                return Collections.emptySet();
            }

            switch (s.getScope()) {
                case PP_INPUT:
                    if (startDir == INPUT) {
                        return s.nextSet(PP_SAME, PP_RELATED_INPUT);
                    } else if (startDir == OUTPUT) {
                        return s.nextSet(PP_SAME);
                    }
                case PP_RELATED_INPUT:
                    return s.nextSet(PP_RELATED_SAME);
                case I_INPUT:
                    return s.nextSet(I_SAME);
            }
        }

        return Collections.emptySet();
    }

     */
}
