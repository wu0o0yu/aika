package network.aika.neuron.meta;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;

public class NegMetaSynapse extends MetaSynapse {

    public static final String TYPE_STR = "NM";


    public NegMetaSynapse() {
        super();
    }

    public NegMetaSynapse(Neuron input, Neuron output, Integer id, boolean recurrent, int lastCount) {
        super(input, output, id, recurrent, lastCount);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegMetaSynapse s = (NegMetaSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new NegMetaSynapse(input, output, id, recurrent, output.getModel().charCounter);
        }
    }
}
