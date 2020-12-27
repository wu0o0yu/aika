package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.Scope;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Visitor.Transition.ACT;

public class PatternSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
        super();
    }

    public PatternSynapse(I input, PatternNeuron output, Synapse template) {
        super(input, output, template);
    }


    @Override
    public void updateReference(Link l) {
        Reference or = l.getOutput().getReference();
        Reference ir = l.getInput().getReference();

        l.getOutput().propagateReference(or == null ? ir : or.add(ir));
    }

    @Override
    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.startDir != Direction.INPUT || !act.getNeuron().isInputNeuron();
    }

    @Override
    protected boolean canBeLinked(Activation fromAct, Activation toAct, Visitor v) {
        return true;
    }

    @Override
    public PatternSynapse instantiateTemplate(I input, PatternNeuron output) {
        if(!input.getTemplates().contains(getInput())) {
            return null;
        }
        return new PatternSynapse(input, output, this);
    }

    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron()) {
            return null;
        }

        return oAct;
    }

    @Override
    public Collection<Scope> transition(Scope s, Direction dir) {
        return Collections.singleton(s);
    }

    @Override
    public byte getType() {
        return type;
    }
}
