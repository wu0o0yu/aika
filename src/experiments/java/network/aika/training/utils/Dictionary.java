package network.aika.training.utils;

import network.aika.neuron.Neuron;
import network.aika.training.MetaModel;
import network.aika.training.input.InputNeuron;

import java.util.Map;
import java.util.TreeMap;

public class Dictionary {

    MetaModel model;
    Map<String, Neuron> dictionary = new TreeMap<>();


    public Dictionary(MetaModel model) {
        this.model = model;
    }

    public Neuron lookupWord(String w) {
        Neuron wn = dictionary.get(w);
        if (wn == null) {
            wn = new InputNeuron(model, "W-" + w).getProvider();

            dictionary.put(w, wn);
        }
        return wn;
    }

}
