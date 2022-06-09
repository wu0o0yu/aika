package network.aika.neuron.bindingsignal;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;

import java.util.Comparator;

public class BSKey {

    public static final Comparator<BSKey> COMPARATOR = Comparator.
            <BSKey>comparingLong(k -> k.neuronId)
            .thenComparing(k -> k.bsStateOrdinal)
            .thenComparingInt(k -> k.activationId);

    private Long neuronId;
    private int bsStateOrdinal;
    private int activationId;

    public BSKey(Neuron n, int bsStateOrdinal, int activationId) {
        this.neuronId = n.getId();
        this.bsStateOrdinal = bsStateOrdinal;
        this.activationId = activationId;
    }

    public static BSKey createReverseKey(BindingSignal bs) {
        Activation act = bs.getActivation();
        return new BSKey(act.getNeuron(), bs.getState().ordinal(), act.getId());
    }

    public static BSKey createKey(BindingSignal bs) {
        Activation originAct = bs.getOriginActivation();
        return new BSKey(originAct.getNeuron(), bs.getState().ordinal(), originAct.getId());
    }

    public static BSKey createKey(Activation originAct, State s) {
        return new BSKey(originAct.getNeuron(), s.ordinal(), originAct.getId());
    }
}
