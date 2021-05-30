package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.activation.visitor.Visitor;
import network.aika.neuron.steps.link.SumUpLink;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.sign.Sign.POS;


public class NegativeBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public NegativeBNSynapse() {
        this.isRecurrent = true;
    }

    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isActive(true)) {
            addWeight(delta);

            QueueEntry.add(
                    l,
                    new SumUpLink(l.getInputValue(POS) * delta)
            );
        }
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.getTargetDir() == OUTPUT &&
                getOutput().computeBiasLB(act) >= 0.4;
    }

    public void updateReference(Link l) {
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !v.getSelfRef())
            return null;

        if (!v.getSelfRef()) {
            oAct = oAct.createBranch(this);
        }
        return oAct;
    }
}
