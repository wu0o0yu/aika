package network.aika.fields;


import network.aika.callbacks.FieldObserver;
import network.aika.steps.FieldStep;

public interface IQueueField extends FieldOutput {

    void setValue(int r, double v);

    void addObserver(FieldObserver observer);

    void removeObserver(FieldObserver observer);

    void process(FieldStep step);

    FieldObject getReference();
}
