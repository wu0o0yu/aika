package network.aika.neuron.activation.linker;

import network.aika.Model;
import network.aika.neuron.activation.Activation;

import java.util.ArrayList;
import java.util.List;

public class LNode {

    String label;

    List<LLink> links = new ArrayList<>();
    PatternType patternType;

    Byte neuronType;
    Class neuronClass;

    public LNode(PatternType patternType, Byte neuronType, String label) {
        this.patternType = patternType;
        this.neuronType = neuronType;
        this.neuronClass = Model.getClassForType(neuronType);
        this.label = label;
    }

    public void addLink(LLink l) {
        links.add(l);
    }

    public void follow(Mode m, Activation act, LLink from, Activation startAct) {
        if(neuronType != null && neuronType != act.getINeuron().getType()) {
            return;
        }

        if(act.isConflicting()) return;

        act.lNode = this;

        links.stream()
                .filter(l -> l != from)
                .forEach(l -> l.follow(m, act, this, startAct));

        act.lNode = null;
    }

    public String toString() {
        return label + " " + (neuronClass != null ? neuronClass.getSimpleName() : "X") + " " + (patternType != null ? patternType : "X");
    }
}
