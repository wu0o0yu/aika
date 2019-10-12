package network.aika.training;

import network.aika.Document;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Linker;
import network.aika.neuron.activation.search.Option;
import network.aika.training.excitatory.ExcitatoryNeuron;

import java.util.*;
import java.util.function.Function;

public class TDocument extends Document {


    public TDocument(MetaModel model, String content) {
        super(model, content, 0);
    }

    public TDocument(MetaModel model, String content, int threadId) {
        super(model, content, threadId);
    }

    public TDocument(MetaModel model, int id, String content) {
        super(model, id, content, 0);
    }

    public TDocument(MetaModel model, int id, String content, int threadId) {
        super(model, id, content, threadId);
    }


    protected Linker initLinker() {
        return new MetaLinker(this);
    }


    public MetaModel getModel() {
        return (MetaModel) super.getModel();
    }


    public void train(Config c) {
        createV = getNewVisitedId();

        Function<Activation, ExcitatoryNeuron> callback = act -> new ExcitatoryNeuron(getModel(), act.getLabel(), null);

        for(Activation act: new ArrayList<>(getActivations(false))) {
            if(act.getUpperBound() > 0.0) {
                TNeuron n = act.getINeuron();

                for (Option o : act.getOptions()) {
                    n.prepareTrainingStep(c, o, callback);
                }
            }
        }

        for(Activation act: new ArrayList<>(getActivations(false))) {
            if(act.getUpperBound() > 0.0) {
                TNeuron n = act.getINeuron();

                n.updateFrequencies(act);
                n.initCountValues();

                for (Option o : act.getOptions()) {
                    n.train(c, o);
                }
            }
        }

        propagate();

        getModifiedWeights().forEach((n, inputSyns) -> {
            TNeuron tn = (TNeuron) n;
            tn.computeOutputRelations();
        });

        commit();

        getModel().charCounter += length();
    }
}
