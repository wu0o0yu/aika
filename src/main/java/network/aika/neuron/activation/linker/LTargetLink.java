package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.PatternScope;
import network.aika.neuron.activation.Activation;

import java.util.stream.Stream;


public class LTargetLink extends LLink {

    protected PatternScope patternScope;


    public LTargetLink(LNode input, LNode output, PatternScope patternScope) {
        super(input, output);
        this.patternScope = patternScope;
    }

    public void follow(Activation act, LNode from, long v, Linker.CollectResults c) {
        Neuron n = act.getNeuron();

        Stream<Synapse> syns = null;
        if(from == input) {
            syns = n.getActiveOutputSynapses().stream();
        } else if(from == output) {
            syns = n.getActiveInputSynapses().stream();
        }

        syns.forEach(s -> follow(act, s, c));
    }

    public void follow(Activation act, Synapse s, Linker.CollectResults c) {
        if(patternScope != null && patternScope != s.getPatternScope()) {
            return;
        }

        c.collect(act);
    }
}
