package network.aika.neuron.activation.fields;


import network.aika.neuron.steps.FieldStep;
import network.aika.neuron.steps.Step;

public class QueueField extends Field {

    private boolean isQueued;
    private FieldStep step;

    public void setStep(FieldStep s) {
        this.step = s;
    }

    public void triggerUpdate() {
        if(!isQueued) {
            Step.add(step);
            isQueued = true;
        }
    }

    public void process() {
        triggerInternal();
    }
}
