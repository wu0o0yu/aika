package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.link.LinkStep;

import static network.aika.neuron.activation.direction.Direction.INPUT;

public class CheckIfFired implements ActivationStep {

    @Override
    public void process(Activation act) {
        if(!act.checkIfFired())
            return;

        QueueEntry.add(act, LINK_AND_PROPAGATE);
        QueueEntry.add(act, USE_FINAL_BIAS);

        if (act.hasBranches())
            QueueEntry.add(act, DETERMINE_BRANCH_PROBABILITY);

        QueueEntry.add(act, COUNTING);
        act.addLinksToQueue(INPUT, LinkStep.COUNTING);
    }

    @Override
    public Phase getPhase() {
        return Phase.LINKING;
    }
}
