package network.aika;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;

public interface PassiveInputFunction {

    double getActivationValue(Synapse s, Activation outputActivation);
}
