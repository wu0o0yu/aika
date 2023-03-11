package network.aika.fields;


import network.aika.FieldObject;
import network.aika.callbacks.FieldObserver;
import network.aika.steps.FieldStep;

public interface IQueueField extends FieldOutput {


    void setCurrentValue(double currentValue);

    void setNewValue(double newValue);

    void setValue(double v);

    void addObserver(FieldObserver observer);

    void removeObserver(FieldObserver observer);

    void setStep(FieldStep step);

    void process();

    FieldObject getReference();
}
