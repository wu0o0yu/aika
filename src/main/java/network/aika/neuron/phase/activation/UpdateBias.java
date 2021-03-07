package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

import static network.aika.neuron.activation.Element.RoundType.FREQUENCY;
import static network.aika.neuron.activation.Element.RoundType.GRADIENT;
import static network.aika.neuron.phase.link.LinkPhase.UPDATE_WEIGHT;

public class UpdateBias extends RankedImpl implements ActivationPhase {

    @Override
    public Ranked getPreviousRank() {
        return UPDATE_WEIGHT;
    }

    @Override
    public void process(Activation act) {
        act.getNeuron().updateBias(act);
    }

    public String toString() {
        return "Act-Phase: Update Bias";
    }

    @Override
    public int compare(Activation o1, Activation o2) {
        return 0;
    }
}
