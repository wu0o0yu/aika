package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.fields.Field;
import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.conjunctive.LatentRelationNeuron;

public class LatentRelatedActivation extends BindingActivation {

    protected LatentRelatedActivation(int id, LatentRelationNeuron n) {
        super(id, n);
    }

    public LatentRelatedActivation(int id, Thought t, LatentRelationNeuron n) {
        super(id, t, n);
    }

    @Override
    protected Field initNet() {
        return new ValueSortedQueueField(this, "net", 10.0);
    }
}
