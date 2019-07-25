package network.aika.neuron.activation.search;

import network.aika.Document;
import network.aika.neuron.activation.Activation;


public class Branch {
    boolean searched;
    double weight = 0.0;
    double weightSum = 0.0;

    SearchNode child = null;

    public boolean prepareStep(Document doc, SearchNode c) {
        child = c;

        child.updateActivations(doc);

        if (!child.followPath()) {
            return true;
        }

        searched = true;
        return false;
    }


    public void postStep(double returnWeight, double returnWeightSum) {
        weight = returnWeight;
        weightSum = returnWeightSum;

        child.setWeight(returnWeightSum);
        child.changeState(Activation.Mode.OLD);
    }
}