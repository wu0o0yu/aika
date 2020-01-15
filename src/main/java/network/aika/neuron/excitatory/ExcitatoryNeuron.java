/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.excitatory;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.meta.MetaSynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.Synapse.State.CURRENT;


/**
 *
 * @author Lukas Molzberger
 */
public class ExcitatoryNeuron extends ConjunctiveNeuron<ExcitatorySynapse> {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    public static final String TYPE_STR = Model.register("NE", ExcitatoryNeuron.class);

    enum WeightBias {
        WEIGHT,
        BIAS
    }

    enum XlMode {
        WEIGHT_KPOS_UPOS((l, out) -> l.getX(Sign.POS) * actFDelta(out), Sign.POS, WeightBias.WEIGHT),
        BIAS_KPOS_UPOS((l, out) -> 0.0, Sign.POS, WeightBias.BIAS),

        WEIGHT_KPOS_UNEG((l, out) -> -l.getX(Sign.NEG) * actFDelta(out), Sign.POS, WeightBias.WEIGHT),
        BIAS_KPOS_UNEG((l, out) -> l.getX(Sign.NEG) * actFDelta(out), Sign.POS, WeightBias.BIAS),

        WEIGHT_KNEG_UPOS((l, out) -> 0.0, Sign.NEG, WeightBias.WEIGHT),
        BIAS_KNEG_UPOS((l, out) -> l.getX(Sign.POS) * actFDelta(out), Sign.NEG, WeightBias.BIAS),

        WEIGHT_KNEG_UNEG((l, out) -> -l.getX(Sign.NEG) * actFDelta(out), Sign.NEG, WeightBias.WEIGHT),
        BIAS_KNEG_UNEG((l, out) -> l.getX(Sign.NEG) * actFDelta(out), Sign.NEG, WeightBias.BIAS);

        ActDelta actDelta;
        Sign k;
        WeightBias wb;

        XlMode(ActDelta actDelta, Sign k, WeightBias wb) {
            this.actDelta = actDelta;
            this.k = k;
            this.wb = wb;
        }

        public double getActDelta(Input l, Activation out) {
            return actDelta.getActDelta(l, out);
        }

        public Sign getK() {
            return k;
        }

        public WeightBias getWB() {
            return wb;
        }
    }


    public interface ActDelta {
        double getActDelta(Input l, Activation out);
    }


    public ExcitatoryNeuron() {
        super();
    }


    public ExcitatoryNeuron(Neuron p) {
        super(p);
    }


    public ExcitatoryNeuron(Model model, String label) {
        super(model, label);
    }


    public String getType() {
        return TYPE_STR;
    }


    public ExcitatorySynapse getMaxInputSynapse(Synapse.State state) {
        ExcitatorySynapse maxSyn = null;
        for(ExcitatorySynapse s: getInputSynapses()) {
            if(maxSyn == null || maxSyn.getNewWeight() < s.getNewWeight()) {
                maxSyn = s;
            }
        }
        return maxSyn;
    }



    public String typeToString() {
        return "EXCITATORY";
    }


    public ExcitatorySynapse createOrLookupSynapse(Document doc, MetaSynapse ms, Neuron inputNeuron) {
        inputNeuron.get(doc);

        ExcitatorySynapse synapse = (ExcitatorySynapse) getProvider().getInputSynapse(inputNeuron);

        // s -> ((ExcitatorySynapse)s).isMappedToMetaSynapse(ms)

        if(synapse == null) {
            synapse = new ExcitatorySynapse(inputNeuron, getProvider(), false, true);

            synapse.link();
        }
        return synapse;
    }


    public void train(Config c, Activation o) {
        super.train(c, o);

        createCandidateSynapses(c, o);

        trainLTL(c, o);
    }


    public Activation init(Activation iAct) {
        Document doc = iAct.getDocument();

        setBias(2.0);

        int actBegin = 0; // iAct.getSlot(BEGIN).getFinalPosition();
        lastCount += actBegin;

        ExcitatorySynapse s = new ExcitatorySynapse(iAct.getNeuron(), getProvider(), false, true, actBegin);

        s.updateDelta(doc, 2.0);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Activation targetAct = new Activation(doc, this, null, 0);

        Link l = new Link(s, iAct, targetAct);
        targetAct.addLink(l);

        return targetAct;
    }


    private void createCandidateSynapses(Config c, Activation targetAct) {
        Document doc = targetAct.getDocument();

        if(log.isDebugEnabled()) {
            log.debug("Created Synapses for Neuron: " + targetAct.getINeuron().getId() + ":" + targetAct.getINeuron().getLabel());
        }

        ArrayList<Activation> candidates = new ArrayList<>();

        targetAct.followDown(doc.getNewVisitedId(), act -> {
            Synapse is = targetAct.getNeuron().getInputSynapse(act.getNeuron());
            if(is != null) return;

            candidates.add(act);
        });

        candidates
                .forEach(act -> createCandidateSynapse(c, act, targetAct));
    }


    private void createCandidateSynapse(Config c, Activation iAct, Activation targetAct) {
        Neuron targetNeuron = targetAct.getNeuron();

        Neuron inputNeuron = iAct.getNeuron();

        if(!((TNeuron) inputNeuron.get()).isMature(c)) {
            return;
        }

        int lastCount = 0; //iAct.getSlot(BEGIN).getFinalPosition();

        ExcitatorySynapse s = new ExcitatorySynapse(inputNeuron, targetNeuron, false, false, lastCount);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Link l = new Link(s, iAct, targetAct);

        targetAct.addLink(l);
    }



    public boolean isMature(Config c) {
        Synapse maxSyn = getMaxInputSynapse(CURRENT);
        if(maxSyn == null) {
            return false;
        }

        TSynapse se = (TSynapse) maxSyn;

        return se.getCounts()[1] >= c.getMaturityThreshold();  // Sign.NEG, Sign.POS
    }



    public void trainLTL(Config config, Activation act) {
        Document doc = act.getDocument();
        double[] pXout = getP();

        double delta = getReliability() * Math.log(pXout[0]);

        for(Input i : getInputs(act)) {
            ExcitatorySynapse si = i.getSynapse();
            double[] pXiXout = si.getPXiXout();

            double[] pXi = i.getPXi();

            double covi = i.getSynapse().getCoverage();
            if (covi == 0.0) {
                continue;
            }

            for(Sign k : Sign.values()) {
                int sii = k.ordinal();
                double Xi = i.getX(k);

                if(pXiXout[sii] == 0.0) {
                    continue;
                }
                double G = Math.log(pXiXout[sii]) - (Math.log(pXi[sii]) + Math.log(pXout[0]));

                double d = Xi * i.getReliability() * i.getP() * covi * G;
                delta += d;

                if(si.getWeight() <= getBias()) {
                    double covDelta = config.learnRate * Xi * i.getReliability() * i.getP() * act.value * G;

                    double weightDelta = covDelta * (1.0 / getBias());
                    double biasDelta = covDelta * -(si.getWeight() / Math.pow(getBias(), 2.0));

                    si.updateDelta(
                            doc,
                            weightDelta
                    );
                    updateBiasDelta(biasDelta);
                }
            }
        }

        if(delta == 0.0) {
            return;
        }

        for(Input l : getInputs(act)) {
            double weightDelta = 0.0;
            double biasDelta = 0.0;

            for (XlMode u : XlMode.values()) {
                if(delta < 0.0 && u.getK() == Sign.POS || delta > 0.0 && u.getK() == Sign.NEG) {
                    continue;
                }

                double IGDelta = u.getActDelta(l, act) * delta;

                double d = config.learnRate * act.getP() * l.getP() * l.getReliability() * IGDelta;

                if (d == 0.0) {
                    continue;
                }

                if (u.getWB() == WeightBias.WEIGHT) {
                    weightDelta += d;
                    biasDelta += d;
                } else if (u.getWB() == WeightBias.BIAS) {
                    biasDelta += d;
                }
            }

            Synapse sl = l.getSynapse();
            sl.updateDelta(
                    doc,
                    weightDelta
            );
            updateBiasDelta(biasDelta);

            //sl.setInactive(NEXT, sl.getWeight(NEXT) <= 0.0);
        }
    }


    public static double actFDelta(Activation act) {
        double net = act.net;
        if(net <= 0.0) return 0.0;

        switch(act.getINeuron().getActivationFunction()) {
            case RECTIFIED_HYPERBOLIC_TANGENT:
                return 1.0 - Math.pow(Math.tanh(net), 2.0);
            case LIMITED_RECTIFIED_LINEAR_UNIT:
                return 1.0;
        }

        return 0.0;
    }


    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return this;
    }


    public double getTrainingNetValue(Activation act) {
        return act.net + trainingBias; //  + getInactiveWeights(o)
    }


    public List<Input> getInputs(Activation act) {
        assert act.getINeuron() == this;

        ArrayList<Input> results = new ArrayList<>();
        Set<Synapse> inputSynapses = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        inputSynapses.addAll(getInputSynapses());

        for(Link l: act.inputLinks.values()) {
            Input i = new Input(l, l.getInput());

            inputSynapses.remove(i.getSynapse());

            if(i.getP() != 0.0 && i.getReliability() != 0.0) {
                results.add(i);
            }
        }

        for(Synapse si: inputSynapses) {
            Input i = new Input(si);

            if(i.getReliability() != 0.0) {
                results.add(i);
            }
        }

        return results;
    }


    public static class Input {

        Link l;
        Activation act;
        ExcitatorySynapse s;

        public Input(Link l, Activation act)  {
            this.l = l;
            this.act = act;
            this.s = (ExcitatorySynapse) l.getSynapse();
        }


        public Input(Synapse s) {
            this.s = (ExcitatorySynapse) s;
        }

        public ExcitatorySynapse getSynapse() {
            return s;
        }

        public TNeuron getNeuron() {
            return s.getInput();
        }

        public String getLabel() {
            return s.getInput().getLabel();
        }

        public double getP() {
            return act != null ? act.getP() : 1.0;
        }

        public double getReliability() {
            return Math.min((s.getInput()).getReliability(), s.getReliability());
        }

        public double[] getPXi() {
            return (getSynapse().getInput()).getP();
        }

        public double getX(Sign s) {
            return s.getX(act != null ? act.value : 0.0);
        }

        public String toString() {
            return getSynapse().getInput().getLabel();
        }
    }
}
