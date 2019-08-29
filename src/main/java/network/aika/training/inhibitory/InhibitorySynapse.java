package network.aika.training.inhibitory;


import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.TSynapse;

public class InhibitorySynapse extends TSynapse {


    public InhibitorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }




    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new InhibitorySynapse(input, output, id);
        }
    }
}
