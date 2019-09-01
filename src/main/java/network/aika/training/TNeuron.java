package network.aika.training;


import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.relation.MultiRelation;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.excitatory.ExcitatoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.activation.link.Direction.INPUT;
import static network.aika.neuron.activation.link.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class TNeuron extends INeuron {

    public static double THRESHOLD = 0.5;
    public static double RELIABILITY_THRESHOLD = 10.0;


    public double countValue;
    public double binaryFrequency;
    public double posFrequency;
    public double negFrequency;
    public double N;
    public int lastCount;
    public double alpha = 0.99;

    public double lengthSum;
    public int numActs;


    public double trainingBias = 0.0;

    public boolean isOutputMetaNeuron;

//    public boolean debugFirst = true;


    public Option init(Option inputOpt) {
        return null;
    }

    public void initCountValues() {
        countValue = 0.0;

        for (Synapse s : getProvider().getActiveInputSynapses()) {
            TSynapse ts = (TSynapse) s;

            ts.initCountValues();
        }
        for (Synapse s : getProvider().getActiveOutputSynapses()) {
            TSynapse ts = (TSynapse) s;

            ts.initCountValues();
        }
    }


    public double getAvgLength() {
        return lengthSum / (double) numActs;
    }


    public void count(Option o) {
        countValue += o.getState().value * o.getP();

        countSynapses(o, INPUT);
        countSynapses(o, OUTPUT);
    }


    private void countSynapses(Option o, Direction dir) {
        Set<Synapse> rest = new TreeSet<>(dir == INPUT ? Synapse.INPUT_SYNAPSE_COMP : Synapse.OUTPUT_SYNAPSE_COMP);
        rest.addAll(dir == INPUT ? getProvider().getActiveInputSynapses() : getProvider().getActiveOutputSynapses());

        for(Map.Entry<Link, Option> me: (dir == INPUT ? o.inputOptions: o.outputOptions).entrySet()) {
            Link l = me.getKey();
            Option lo = me.getValue();
            TSynapse ts = (TSynapse)l.getSynapse();

            rest.remove(l.getSynapse());

            if(dir == INPUT) {
                ts.updateCountValue(lo, o);
            } else if(dir == OUTPUT) {
                ts.updateCountValue(o, lo);
            }
        }

        for(Synapse s: rest) {
            TSynapse ts = (TSynapse)s;

            if(dir == INPUT) {
                ts.updateCountValue(null, o);
            } else if(dir == OUTPUT) {
                ts.updateCountValue(o, null);
            }
        }
    }


    public void updateFrequencies(Activation act) {
        int beginPos = StatUtil.getCurrentPos(act, BEGIN);
        int endPos = StatUtil.getCurrentPos(act, END);

        int stepsBefore = beginPos - lastCount;
        int stepsWithin = endPos - beginPos;

        lengthSum += stepsWithin;
        numActs++;

        lastCount = endPos;

        posFrequency = (stepsWithin * countValue) + (alpha * posFrequency);
        negFrequency = stepsBefore + ((double) stepsWithin * (1.0 - countValue)) + (alpha * negFrequency);
        binaryFrequency = (countValue > 0.0 ? 1.0 : 0.0) + (alpha * binaryFrequency);
        N = stepsBefore + stepsWithin + (alpha * N);

        updateSynapseFrequencies(act, INPUT);
        updateSynapseFrequencies(act, OUTPUT);
    }


    private void updateSynapseFrequencies(Activation act, Direction dir) {
        Set<Synapse> rest = new TreeSet<>(dir == INPUT ? Synapse.INPUT_SYNAPSE_COMP : Synapse.OUTPUT_SYNAPSE_COMP);
        rest.addAll(dir == INPUT ? getProvider().getActiveInputSynapses() : getProvider().getActiveOutputSynapses());

        for(Link l: (dir == INPUT ? act.getInputLinks(): act.getOutputLinks()).collect(Collectors.toList())) {
            TSynapse ts = (TSynapse) l.getSynapse();

            rest.remove(ts);

            if(dir == INPUT) {
                ts.updateFrequencies(alpha, l.getInput(), act);
            } else if(dir == OUTPUT) {
                ts.updateFrequencies(alpha, act, l.getOutput());
            }
        }

        for(Synapse s: rest) {
            TSynapse ts = (TSynapse) s;

            if(dir == INPUT) {
                ts.updateFrequencies(alpha, null, act);
            } else if(dir == OUTPUT) {
                ts.updateFrequencies(alpha, act, null);
            }
        }
    }


    public double[] getP() {
        double p = posFrequency / N;
        return new double[] {
                p,
                1.0 - p
        };
    }


    public List<? extends TNeuron> getInputTargets(TDocument doc, Option in) {
        return Collections.singletonList(this);
    }


    public String freqToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(posFrequency));
        sb.append(" Neg:" + Utils.round(N - posFrequency));
        return sb.toString();
    }

    public String propToString() {
        double[] p = getP();
        StringBuilder sb = new StringBuilder();
        sb.append("Pos:" + Utils.round(p[0]));
        sb.append(" Neg:" + Utils.round(p[1]));
        return sb.toString();
    }


    public double getReliability() {
        return binaryFrequency >= RELIABILITY_THRESHOLD ? Math.log(binaryFrequency - (RELIABILITY_THRESHOLD - 1.0)) : 0.0;
    }


    public boolean isOutputMetaNeuron() {
        return isOutputMetaNeuron;
    }


    public void setOutputMetaNeuron(boolean outputMetaNeuron) {
        isOutputMetaNeuron = outputMetaNeuron;
    }


    public void train(Activation act, TDocument.Config config, TDocument.DebugDocument ddoc) {

    }


    // Implemented only for meta and target neurons
    public void trainMeta(Activation metaAct, double threshold, Function<Activation, ExcitatoryNeuron> callback) {
        Document doc = metaAct.getDocument();
        doc.createV = doc.getNewVisitedId();

        for (Option o : metaAct.getOptions()) {
            if (o.getP() > threshold && getTrainingNetValue(o) > 0.0) {
                ExcitatoryNeuron targetNeuron = getTargetNeuron(metaAct, callback);

                collectTarget((TDocument) doc, o, targetNeuron);
            }
        }
    }

    private void collectTarget(TDocument mDoc, Option metaAct, ExcitatoryNeuron targetNeuron) {
        if(metaAct == null) {
            return;
        }

        List<ExcitatoryNeuron> targets = mDoc.metaActivations.get(metaAct);
        if (targets == null) {
            targets = new ArrayList<>();
            mDoc.metaActivations.put(metaAct, targets);
        }

        targets.add(targetNeuron);
    }


    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return null;
    }


    public double getTrainingNetValue(Option o) {
        return o.getState().net;
    }


    public void clearOutputRelations() {
        for(Map.Entry<Integer, Relation> me: getOutputRelations().entrySet()) {
            Relation rel = me.getValue();

            if(rel instanceof MultiRelation) {
                MultiRelation mr = (MultiRelation) rel;

                for(Relation r: mr.getLeafRelations()) {
                    Relation.removeRelation(Synapse.OUTPUT, me.getKey(), getProvider(), r.invert());
                }
            } else {
                Relation.removeRelation(Synapse.OUTPUT, me.getKey(), getProvider(), rel.invert());
            }
        }

        getOutputRelations().clear();
    }


    public void computeOutputRelations() {

    }

    static boolean alreadyCreated = false;

    public void generateNeuron(Activation seedAct) {
        if(isMature()) {
            for(Option o: seedAct.getOptions()) {
//                if((o.p * (1.0 - getCoverage(o))) > THRESHOLD) {
                    if(!alreadyCreated) {
                        alreadyCreated = true;
                        generateNeurons(o);
                    }
//                }
            }
        }
    }


    private void generateNeurons(Option o) {
        ExcitatoryNeuron targetNeuron = new ExcitatoryNeuron(getModel(), "DERIVED-FROM-(" + o.getAct().getLabel() + ")", null);

        targetNeuron.init(o);
    }



    private double getCoverage(Option seedOpt) {
        double maxCoverage = 0.0;
        for(Map.Entry<Link, Option> me: seedOpt.outputOptions.entrySet()) {
            maxCoverage = Math.max(maxCoverage, getCoverage(me.getKey(), seedOpt, me.getValue()));
        }

        return maxCoverage;
    }


    private static double getCoverage(Link l, Option in, Option out) {
        INeuron n = out.getAct().getINeuron();
        return Math.min(Math.max(0.0, out.getState().net), Math.max(0.0, in.getState().value * l.getSynapse().getWeight())) / n.getBias();
    }


    public abstract boolean isMature();


    public List<Integer> getOutputSlots() {
        List<Integer> slots = new ArrayList<>();
        for(Map.Entry<Integer, Relation> me: getOutputRelations().entrySet()) {
            Relation rel = me.getValue();
            if(rel instanceof PositionRelation) {
                PositionRelation posRel = (PositionRelation) rel;

                slots.add(posRel.toSlot);
            }
        }

        return slots;
    }


    public void generateSynapses(Activation act) {

    }


    public static boolean checkSelfReferencing(Option current, Option inhib) {
        if(inhib == null) {
            return false;
        }

        for(Option in: inhib.inputOptions.values()) {
            if(in == current) {
                return true;
            }
        }

        return false;
    }


    protected String toDetailedString() {
        return super.toDetailedString() + " f:(" + freqToString() + ")";
    }


    public TNeuron(MetaModel model, String label, String outputText, Type type, ActivationFunction actF) {
        super(model, label, outputText, type, actF);
        this.lastCount = model.charCounter;
    }


    public MetaModel getModel() {
        return (MetaModel) super.getModel();
    }


    // TODO: store and retrieve targetSynapseIds
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(posFrequency);
        out.writeDouble(N);
        out.writeInt(lastCount);

        out.writeDouble(trainingBias);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        posFrequency = in.readDouble();
        N = in.readDouble();
        lastCount = in.readInt();

        trainingBias = in.readDouble();
    }

}
