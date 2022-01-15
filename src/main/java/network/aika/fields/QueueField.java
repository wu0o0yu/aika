package network.aika.fields;


import network.aika.neuron.activation.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Step;

public class QueueField extends Field {

    private boolean isQueued;
    private FieldStep step;


    public QueueField(Element e, String label) {
        super();
        step = new FieldStep(e, this, label);
    }

    public QueueField(FieldUpdateEvent fieldListener, Element e, String label) {
        this(e, label);
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
