package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

import static network.aika.neuron.activation.linker.Mode.LINKING;

public class LMatchingLink<S extends Synapse> extends LLink<S> {

    boolean dir;

    public LMatchingLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, boolean dir) {
        super(input, output, patternScope, synapseClass, label);

        this.dir = dir;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Stream<Link> s = null;
        if(from == input) {
            if(!act.isFinal && act.lastRound != null) {
                act = act.lastRound;
            }
            s = act.outputLinks.values().stream();
        } else if(from == output) {
            s = act.inputLinks.values().stream();
        }

        s.forEach(l -> follow(m, l, from, startAct));
    }

    public void followBackwards(Mode m, Link l) {
        Activation startAct = l.getOutput();
        startAct.lNode = output;
        follow(m, l, output, startAct);
    }

    public void follow(Mode m, Link l, LNode from, Activation startAct) {
        LNode to = getTo(from);
        if(!checkSynapse(l.getSynapse())) {
            return;
        }

        Activation act = getToActivation(l, to);
        INeuron n = getToNeuron(l.getSynapse(), to);
        to.follow(m, n, act, this, startAct);
    }

    @Override
    public String getTypeStr() {
        return "M";
    }
}
