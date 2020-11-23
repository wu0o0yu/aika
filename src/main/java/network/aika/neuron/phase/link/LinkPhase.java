package network.aika.neuron.phase.link;

import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.Phase;


public interface LinkPhase extends Phase<Link> {

    LinkPhase INITIAL_LINKING = new Linking();
    LinkPhase FINAL_LINKING = new FinalLinking();
    LinkPhase COUNTING = new Counting();

}
