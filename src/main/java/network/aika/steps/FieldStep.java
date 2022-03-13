package network.aika.steps;

import network.aika.neuron.activation.Element;
import network.aika.fields.QueueField;

public class FieldStep<E extends Element> extends Step<E> {

    private QueueField field;


    public FieldStep(E e, QueueField qf) {
        super(e);
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
        return field.getLabel() + ": " + field + " " + getElement();
    }
}
