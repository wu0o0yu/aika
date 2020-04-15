package network.aika.neuron.activation.linker;

import network.aika.neuron.PatternScope;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class LMatchingLink extends LLink {

    boolean dir;

    public LMatchingLink(LNode input, LNode output, PatternScope patternScope, String label, boolean dir) {
        super(input, output, patternScope, label);

        this.dir = dir;
    }

    protected LNode getTo(LNode from) {
        if(from == input) {
            return output;
        }
        if(from == output) {
            return input;
        }
        return null;
    }

    protected Activation getToActivation(Link l, LNode from) {
        if(from == input) {
            return l.getOutput();
        }
        if(from == output) {
            return l.getInput();
        }
        return null;
    }

    public void follow(Activation act, LNode from, Activation startAct, Linker.CollectResults c) {
        Stream<Link> s = null;
        if(from == input) {
            s = act.outputLinks.values().stream();
        } else if(from == output) {
            s = act.inputLinks.values().stream();
        }

        s.forEach(l -> follow(l, from, startAct, c));
    }

    private void follow(Link l, LNode from, Activation startAct, Linker.CollectResults c) {
        LNode to = getTo(from);
        if(!checkLink(l)) {
            return;
        }

        to.follow(getToActivation(l, from), this, startAct, c);
    }

    private boolean checkLink(Link l) {
        if(patternScope != null && patternScope != l.getSynapse().getPatternScope()) {
            return false;
        }

        return true;
    }

    @Override
    public String getTypeStr() {
        return "M";
    }
}
