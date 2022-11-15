package network.aika.callbacks;

import network.aika.neuron.activation.Activation;

public interface ActivationCheckCallback {

    boolean check(Activation iAct);
}
