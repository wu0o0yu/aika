package network.aika.neuron.meta;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;

public class NegMetaSynapse extends MetaSynapse {

    public NegMetaSynapse(Neuron input, Neuron output, Integer id, int lastCount) {
        super(input, output, id, lastCount);
    }

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegMetaSynapse s = (NegMetaSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new NegMetaSynapse(input, output, id, output.getModel().charCounter);
        }
    }
}
