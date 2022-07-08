package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.fields.Field;
import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PrimitiveTransition;
import network.aika.neuron.bindingsignal.State;
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

    public BindingSignal addLatentBindingSignal(PatternActivation fromOriginAct, PrimitiveTransition t) {
        BindingSignal originBS = fromOriginAct.getBindingSignal(State.SAME);
        BindingSignal latentBS = new BindingSignal(originBS, t);
        latentBS.init(this);
        addBindingSignal(latentBS);
//        QueueField qf = (QueueField) latentBS.getOnArrived();
//        qf.process();

        return latentBS;
    }
}
