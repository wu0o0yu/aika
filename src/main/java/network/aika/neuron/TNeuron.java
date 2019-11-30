package network.aika.neuron;


import network.aika.Config;
import network.aika.Model;
import network.aika.Utils;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.Activation.Link;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.link.Direction.INPUT;
import static network.aika.neuron.activation.link.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class TNeuron<A extends Activation, S extends Synapse> extends INeuron<A, S> {

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


    protected TNeuron() {
        super();
    }


    public TNeuron(Neuron p) {
        super(p);
    }


    public TNeuron(Model model, String label) {
        super(model, label);
        this.lastCount = model.charCounter;
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
        countValue += o.getState().lb * o.getP();

        countSynapses(o, INPUT);
        countSynapses(o, OUTPUT);
    }


    private void countSynapses(Option o, Direction dir) {
        Set<Synapse> rest = new TreeSet<>(dir == INPUT ? Synapse.INPUT_SYNAPSE_COMP : Synapse.OUTPUT_SYNAPSE_COMP);
        rest.addAll(dir == INPUT ? getProvider().getActiveInputSynapses() : getProvider().getActiveOutputSynapses());

        for(Option.Link ol: (dir == INPUT ? o.inputOptions: o.outputOptions).keySet()) {
            TSynapse ts = (TSynapse)ol.getActivationLink().getSynapse();
            Option lo = (dir == INPUT ? ol.getInput(): ol.getOutput());

            rest.remove(ol.getActivationLink().getSynapse());

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


    public void prepareTrainingStep(Config c, Option o, Function<Activation, ExcitatoryNeuron> callback) {
        prepareMetaTraining(c, o, callback);
        count(o);
    }


    public void train(Config c, Option o) {
//      if((o.p * (1.0 - getCoverage(o))) > THRESHOLD) {
        if (isMature(c)) {
            generateNeuron(o);
        }
//      }
    }


    // Implemented only for meta and target neurons
    public void prepareMetaTraining(Config c, Option o, Function<Activation, ExcitatoryNeuron> callback) {
        if (o.getP() > c.getMetaThreshold() && getTrainingNetValue(o) > 0.0) {
            o.targetNeuron = getTargetNeuron(o.getAct(), callback);
        }
    }


    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return null;
    }


    public double getTrainingNetValue(Option o) {
        return o.getState().net;
    }


    public void computeOutputRelations() {

    }


    // Entfernen und durch transferMetaSynapses ersetzen.
    private void generateNeuron(Option o) {
        ExcitatoryNeuron targetNeuron = new ExcitatoryNeuron(getModel(), "DERIVED-FROM-(" + o.getAct().getLabel() + ")");

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


    public abstract boolean isMature(Config c);


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

    public abstract void dumpStat();

}
