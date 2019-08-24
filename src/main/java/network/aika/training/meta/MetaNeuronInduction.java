package network.aika.training.meta;

import network.aika.neuron.Neuron;
import network.aika.training.MetaModel;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.inhibitory.InhibitoryNeuron;

import java.util.List;
import java.util.stream.Collectors;

import static network.aika.neuron.INeuron.Type.EXCITATORY;

public class MetaNeuronInduction {

    MetaModel model;

    public static double COVERED_THRESHOLD = 5.0;

    public MetaNeuronInduction(MetaModel model) {
        this.model = model;
    }

    public void process(int threadId) {
        for(Neuron n: model.getActiveNeurons()) {
            List<ExcitatorySynapse> candidateSynapses = n
                    .getActiveOutputSynapses()
                    .stream()
                    .filter(s -> s.getOutput().getType() == EXCITATORY)
                    .map(s -> (ExcitatorySynapse) s)
                    .collect(Collectors.toList());

            double coveredScore = coveredSum(candidateSynapses);

            if(coveredScore > COVERED_THRESHOLD) {
                createNewMetaNeuron(threadId, n, candidateSynapses);
            }
        }
    }

    public void createNewMetaNeuron(int threadId, Neuron inputNeuron, List<ExcitatorySynapse> candidateSynapses) {
        MetaNeuron mn = new MetaNeuron(model,"");

        MetaSynapse ms = new MetaSynapse(inputNeuron, mn.getProvider(), 0, model.charCounter);
        ms.link();

        for(ExcitatorySynapse ts: candidateSynapses) {
            new MetaNeuron.MappingLink(mn, (ExcitatoryNeuron) ts.getOutput().get(), ts.getUncovered()).link();
            new MetaSynapse.MappingLink(ms, ts).link();
        }

        mn.train(threadId);

        InhibitoryNeuron.induceOutgoing(threadId, mn);
    }


    public double coveredSum(List<ExcitatorySynapse> syns) {
        double sum = 0.0;
        for(ExcitatorySynapse s: syns) {
            sum += s.getUncovered();
        }
        return sum;
    }


}
