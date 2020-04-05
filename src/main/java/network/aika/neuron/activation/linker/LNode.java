package network.aika.neuron.activation.linker;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.stream.Stream;

public class LNode {

    LLink[] links;
    PatternType patternType;

    enum PatternType {
        CURRENT,
        INPUT,
        RELATED
    }


    public LNode(PatternType patternType, LLink... links) {
        this.links = links;
        this.patternType = patternType;
    }

    public void follow(Activation act, LLink from, long v, Linker.CollectResults c) {
        if(act.visited == v) return;
        act.visited = v;

        for(int i = 0; i < links.length; i++) {
            LLink ln = links[i];
            if(ln == from) {
                continue;
            }

            Stream<Link> s = null;
            if(this == ln.input) {
                s = act.outputLinks.values().stream();
            } else if(this == ln.output) {
                s = act.inputLinks.values().stream();
            }

            s.forEach(l -> ln.follow(l, this, v, c));
        }
    }
}
