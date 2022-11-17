package network.aika.fields;


import network.aika.neuron.activation.Element;
import network.aika.steps.FieldStep;

public interface IQueueField {


    void setStep(FieldStep step);

    void process();

    Element getReference();
}
