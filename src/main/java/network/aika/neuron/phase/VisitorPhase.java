package network.aika.neuron.phase;

import network.aika.Config;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;

public interface VisitorPhase {

    void tryToLink(Activation act, Visitor v);

    void propagate(Activation act, Visitor v);

    ActivationPhase[] getNextActivationPhases(Config c);

    LinkPhase[] getNextLinkPhases(Config c);
}
