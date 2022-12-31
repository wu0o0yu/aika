package network.aika.visitor.linking;

import network.aika.elements.links.Link;
import network.aika.elements.synapses.Scope;

public interface LinkingCallback {

    boolean compatible(Scope from, Scope to);

    void createRelation(Link l);
}
