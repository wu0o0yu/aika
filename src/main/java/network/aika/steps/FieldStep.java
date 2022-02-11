package network.aika.steps;

import network.aika.neuron.activation.Element;
import network.aika.fields.QueueField;

public class FieldStep<E extends Element> extends Step<E> {

    private QueueField field;
    private String label;

    public FieldStep(E e, QueueField qf, String label) {
        super(e);
        this.label = label;
        this.field = qf;
        this.field.setStep(this);
    }

    @Override
    public void process() {
        field.process();
    }

    @Override
    public Phase getPhase() {
        return Phase.PROCESSING;
    }

    @Override
    public boolean checkIfQueued() {
        return false;
    }

    public String toString() {
        return label + ": " + field + " " + getElement();
    }
}
