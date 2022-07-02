package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.fields.Field;
import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.SingleTransition;
import network.aika.neuron.conjunctive.LatentRelationNeuron;

public class LatentRelationActivation extends BindingActivation {

    protected LatentRelationActivation(int id, LatentRelationNeuron n) {
        super(id, n);
    }

    public LatentRelationActivation(int id, Thought t, LatentRelationNeuron n) {
        super(id, t, n);
    }

    @Override
    protected Field initNet() {
        return new ValueSortedQueueField(this, "net", 10.0);
    }

    public BindingSignal addLatentBindingSignal(BindingSignal bs, SingleTransition t) {
        BindingSignal latentBS = new BindingSignal(bs, t);
        latentBS.init(this);
        addBindingSignal(latentBS);
//        QueueField qf = (QueueField) latentBS.getOnArrived();
//        qf.process();

        return latentBS;
    }
}
