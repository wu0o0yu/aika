package org.aika.training;


import org.aika.corpus.Document;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;

public class InterprSupprTraining {


    public static double LEARN_RATE = -0.1;


    public static void train(Document doc, double learnRate) {
        for(INeuron n: doc.activatedNeurons) {
            for(Activation act: n.getActivations(doc)) {
                if(!act.isFinalActivation() && act.maxActValue > 0.0 && n.type != INeuron.Type.META) {
                    act.errorSignal += learnRate * act.maxActValue;
                }
            }
        }
    }
}
