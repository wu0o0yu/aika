package network.aika.neuron.phase.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.phase.Ranked;
import network.aika.neuron.phase.RankedImpl;

import java.util.Comparator;

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

    @Override
    public Comparator<Activation> getElementComparator() {
        return Comparator.naturalOrder();
    }

    public String toString() {
        return "Act-Phase: Update Bias";
    }
}
