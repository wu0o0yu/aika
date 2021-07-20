package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.scope.Scope;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class RelatedBNSynapse<I extends Neuron<?>> extends InputBNSynapse<I> {


    public RelatedBNSynapse() {
        super();
    }

    public RelatedBNSynapse(boolean recurrent) {
        super(recurrent);
    }

    @Override
    public LinkVisitor samePatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getStartDir() == v.getCurrentDir())
            return null;

        Scope ns = INPUT.transition(v.getScope(), Scope.RELATED, Scope.SAME);
        if(ns == null)
            ns = INPUT.transition(v.getScope(), Scope.INPUT, Scope.RELATED);

        if(ns == null)
            return null;

        return new LinkVisitor(v, this, l, ns);
    }

    @Override
    public LinkVisitor inputPatternTransitionLoop(ActVisitor v, Link l) {
        return null;
    }
}
