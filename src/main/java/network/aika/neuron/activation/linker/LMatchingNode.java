package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

public class LMatchingNode<N extends INeuron> extends LNode<N> {

    public LMatchingNode(PatternType patternType, Class<N> neuronClass, String label) {
        super(patternType, neuronClass, label);
    }

    public void follow(Mode m, Link l, LLink from, Activation startAct) {
        Activation act = from.getToActivation(l, this);

        if(neuronClass != null && act.getINeuron().getClass().equals(neuronClass)) {
            return;
        }

        if(act.isConflicting()) return;

        act.lNode = this;

        links.stream()
                .filter(nl -> nl != from)
                .forEach(nl -> nl.follow(m, act, this, startAct));

        act.lNode = null;
    }
}
