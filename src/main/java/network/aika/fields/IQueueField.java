package network.aika.fields;


import network.aika.callbacks.FieldObserver;
import network.aika.neuron.activation.Element;
import network.aika.steps.FieldStep;

public interface IQueueField extends FieldOutput {


    void setCurrentValue(double currentValue);

    void setNewValue(double newValue);

    void addObserver(FieldObserver observer);

    void removeObserver(FieldObserver observer);

    void setStep(FieldStep step);

    void process();

    Element getReference();
}
