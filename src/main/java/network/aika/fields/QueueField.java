package network.aika.fields;


import network.aika.neuron.activation.Element;
import network.aika.neuron.steps.FieldStep;
import network.aika.neuron.steps.Phase;
import network.aika.neuron.steps.Step;
import network.aika.neuron.steps.StepType;

public class QueueField extends Field {

    private boolean isQueued;
    private FieldStep step;


    public QueueField(Element e, String label, Phase p, StepType st) {
        super();
        step = new FieldStep(e, this, label, p, st);
    }


    public QueueField(FieldUpdateEvent fieldListener, Element e, String label, Phase p, StepType st) {
        this(e, label, p, st);
        setFieldListener(fieldListener);
    }

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
