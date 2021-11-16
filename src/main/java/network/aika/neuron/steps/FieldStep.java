package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.fields.QueueField;

public class FieldStep<E extends Element> extends Step<E> {

    private QueueField field;
    private String label;
    private Phase phase;
    private StepType stepType;


    public FieldStep(E e, QueueField qf, String label, Phase p, StepType st) {
        super(e);
        this.label = label;
        this.phase = p;
        this.stepType = st;
        this.field = qf;
        this.field.setStep(this);
    }

    @Override
    public void process() {
        field.process();
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    @Override
    public StepType getStepType() {
        return stepType;
    }

    @Override
    public boolean checkIfQueued() {
        return false;
    }

    public String toString() {
        return "Field-Step: " + label + "  " + getElement().toShortString();
    }
}
