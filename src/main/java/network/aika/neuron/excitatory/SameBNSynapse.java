package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class SameBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public SameBNSynapse() {
    }

    public SameBNSynapse(boolean isRecurrent) {
        this.isRecurrent = isRecurrent;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if (v.getTargetDir() == INPUT)
            return !act.getNeuron().isInputNeuron() && isRecurrent;
        else if (v.getTargetDir() == OUTPUT)
            return getOutput().computeBiasLB(act) >= 0.4;

        throw new IllegalStateException();
    }
}
