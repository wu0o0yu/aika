package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.scope.Scope;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class PrimaryBNSynapse<I extends Neuron<?>> extends InputBNSynapse<I> {


    @Override
    public LinkVisitor samePatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getStartDir() != v.getCurrentDir())
            return null;

        Scope ns = INPUT.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return null;

        return new LinkVisitor(v, this, l);
    }

    @Override
    public LinkVisitor inputPatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getStartDir() == v.getCurrentDir())
            return null;

        Scope ns = INPUT.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return null;

        return new LinkVisitor(v, this, l, ns);
    }
}
