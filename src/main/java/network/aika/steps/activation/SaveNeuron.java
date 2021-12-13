package network.aika.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.StepType;

public class SaveNeuron extends Step<Activation> {

    public static void add(Activation act) {
        Step.add(new SaveNeuron(act));
    }

    private SaveNeuron(Activation act) {
        super(act);
    }

    @Override
    public Phase getPhase() {
        return Phase.COUNTING;
    }

    @Override
    public StepType getStepType() {
        return StepType.TRAINING;
    }

    public boolean checkIfQueued() {
        return true;
    }

    @Override
    public void process() {
        getElement()
                .getNeuron()
                .getProvider()
                .save();
    }

    public String toString() {
        return "Act-Step: Store neuron " + getElement().toShortString();
    }
}