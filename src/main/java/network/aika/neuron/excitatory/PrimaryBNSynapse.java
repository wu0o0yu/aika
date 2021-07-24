package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.LinkVisitor;
import network.aika.neuron.scope.Scope;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class PrimaryBNSynapse<I extends Neuron<?>> extends InputBNSynapse<I> {


    @Override
    public void samePatternTransitionLoop(ActVisitor v, Link l) {
        if(v.getStartDir() != v.getCurrentDir())
            return;

        Scope ns = INPUT.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return;

        l.follow(v, ns);
    }

    @Override
    public void inputPatternTransitionLoop(ActVisitor v, Link l) {
        Direction currentDir = v.getCurrentDir();
        if(v.getStartDir() != currentDir)
            return;

        Scope ns = currentDir.transition(v.getScope(), Scope.INPUT, Scope.SAME);
        if(ns == null)
            return;

        l.follow(v, ns);
    }

    @Override
    public void patternTransitionLoop(ActVisitor v, Link l) {
    }

    @Override
    public void inhibitoryTransitionLoop(ActVisitor v, Link l) {
        if (v.getStartDir() != v.getCurrentDir())
            return;

        l.follow(v);
    }
}
