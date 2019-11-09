package network.aika.neuron.inhibitory;


import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TSynapse;

public class InhibitorySynapse extends TSynapse {


    public InhibitorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }

    @Override
    public boolean storeOnInputSide() {
        return false;
    }

    @Override
    public boolean storeOOutputSide() {
        return true;
    }


    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new InhibitorySynapse(input, output, id);
        }
    }
}
