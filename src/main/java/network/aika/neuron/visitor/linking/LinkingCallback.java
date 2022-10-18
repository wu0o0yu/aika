package network.aika.neuron.visitor.linking;

import network.aika.neuron.activation.Link;
import network.aika.neuron.conjunctive.Scope;

public interface LinkingCallback {

    boolean compatible(Scope from, Scope to);

    void createRelation(Link l);
}
