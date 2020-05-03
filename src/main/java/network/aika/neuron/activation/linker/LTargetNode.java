package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

public class LTargetNode<N extends INeuron> extends LNode<N> {

    Boolean assumePosRecLinks;

    public LTargetNode(PatternType patternType, Class<N> neuronClass, String label) {
        super(patternType, neuronClass, label);
    }

    public void follow(Mode m, Link l, LLink from, Activation startAct) {
    }

    /*
        propagationTargets
                .forEach(s ->
                        addLinkToQueue(queue, s, act,
                                new Activation(doc.getNewActivationId(), doc, s.getOutput())
                        )
                );
*/
}
