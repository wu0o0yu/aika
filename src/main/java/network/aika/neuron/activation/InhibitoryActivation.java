package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.Neuron;

public class InhibitoryActivation extends Activation {


    public InhibitoryActivation(int id, Thought t, Neuron neuron) {
        super(id, t, neuron);
    }


    @Override
    protected Activation newInstance() {
        return new InhibitoryActivation(id, thought, neuron);
    }

    @Override
    public byte getType() {
        return 2;
    }
}
