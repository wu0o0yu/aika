package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Context;

public class PatternSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternNeuron> {

    public Context transition(Context c) {
        return new Context(c, true);
    }
}
