package network.aika.neuron.activation.linker;

public class LTargetNode extends LNode {

    Boolean assumePosRecLinks;

    public LTargetNode(PatternType patternType, Byte neuronType, String label) {
        super(patternType, neuronType, label);
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
