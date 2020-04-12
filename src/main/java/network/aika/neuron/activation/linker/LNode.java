package network.aika.neuron.activation.linker;

import network.aika.Model;
import network.aika.neuron.activation.Activation;

public class LNode {

    String label;

    LLink[] links;
    PatternType patternType;

    Byte neuronType;
    Class neuronClass;

    public LNode(PatternType patternType, Byte neuronType, String label) {
        this.patternType = patternType;
        this.neuronType = neuronType;
        this.neuronClass = Model.getClassForType(neuronType);
        this.label = label;
    }

    public void setLinks(LLink... links) {
        this.links = links;
    }

    public void follow(Activation act, LLink from, Activation startAct, Linker.CollectResults c) {
        if(neuronType != null && neuronType != act.getINeuron().getType()) {
            return;
        }

        for(int i = 0; i < links.length; i++) {
            LLink ln = links[i];
            if(ln == from) {
                continue;
            }

            ln.follow(act, this, startAct, c);
        }
    }

    public String toString() {
        return label + " " + (neuronClass != null ? neuronClass.getSimpleName() : "X") + " " + (patternType != null ? patternType : "X");
    }
}
