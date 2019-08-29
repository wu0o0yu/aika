package network.aika.training;

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.util.Map;

public class PActivation extends Activation {


    public PActivation(Document doc, INeuron neuron, Map<Integer, Position> slots) {
        super(doc, neuron, slots);
    }
}
