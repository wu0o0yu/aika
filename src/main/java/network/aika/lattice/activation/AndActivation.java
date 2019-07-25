package network.aika.lattice.activation;

import network.aika.Document;
import network.aika.lattice.AndNode;
import network.aika.lattice.NodeActivation;
import network.aika.lattice.refinement.RefValue;
import network.aika.lattice.refinement.Refinement;
import network.aika.neuron.activation.Activation;


public class AndActivation extends NodeActivation<AndNode> {

    public Link[] inputs;


    public AndActivation(Document doc, AndNode node) {
        super(doc, node);
        inputs = new Link[node.level];
    }


    public void link(Refinement ref, RefValue rv, InputActivation refAct, NodeActivation<?> input) {
        Link l = new Link(ref, rv, refAct, input, this);
        inputs[rv.refOffset] = l;
        input.outputsToAndNode.put(id, l);
    }


    public Activation getInputActivation(int i) {
        Link l = inputs[i];
        if(l != null) {
            return l.refAct.input;
        } else {
            for(int j = 0; j < inputs.length; j++) {
                if (j != i) {
                    l = inputs[j];
                    if(l != null) {
                        return l.input.getInputActivation(l.rv.reverseOffsets[i]);
                    }
                }
            }
            return null;
        }
    }


    public boolean isComplete() {
        int numberOfLinks = 0;
        for (Link l : inputs) {
            if (l != null) numberOfLinks++;
        }
        return getNode().parents.size() == numberOfLinks;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("A-ACT(");
        boolean first = true;
        for(int i = 0; i < inputs.length; i++) {
            Activation iAct = getInputActivation(i);
            if(iAct != null) {
                if(!first) {
                    sb.append(",");
                }
                sb.append(i + ":" + iAct.getLabel() + " " + iAct.slotsToString() + " (" + iAct.getId() + ")");

                first = false;
            }
        }
        sb.append(")");
        return sb.toString();
    }


    public static class Link {
        public Refinement ref;
        public RefValue rv;

        public NodeActivation<?> input;
        public InputActivation refAct;
        public AndActivation output;

        public Link(Refinement ref, RefValue rv, InputActivation refAct, NodeActivation<?> input, AndActivation output) {
            this.ref = ref;
            this.rv = rv;
            this.refAct = refAct;
            this.input = input;
            this.output = output;
        }
    }
}
