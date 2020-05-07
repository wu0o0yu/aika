package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

import java.util.ArrayList;
import java.util.List;

public abstract class LNode<N extends INeuron> {

    String label;

    List<LLink> links = new ArrayList<>();

    Class<N> neuronClass;

    public abstract Activation follow(Mode m, INeuron n, Activation act, LLink from, Activation startAct);

    public LNode(Class<N> neuronClass, String label) {
        this.neuronClass = neuronClass;
        this.label = label;
    }

    public boolean checkNeuron(INeuron n) {
        if(neuronClass != null && !n.getClass().equals(neuronClass)) {
            return false;
        }

        return true;
    }

    public void addLink(LLink l) {
        links.add(l);
    }

    public String toString() {
        return label + " " + (neuronClass != null ? neuronClass.getSimpleName() : "X");
    }
}
