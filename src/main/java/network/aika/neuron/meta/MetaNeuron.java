package network.aika.neuron.meta;

import network.aika.ActivationFunction;
import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.MetaActivation;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.MetaInhibSynapse;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.State.CURRENT;

public class MetaNeuron extends TNeuron<MetaActivation> {

    public static double COVERED_THRESHOLD = 5.0;

    public InhibitoryNeuron inhibitoryNeuron;

    public Map<ExcitatoryNeuron, MappingLink> targetNeurons = new TreeMap<>();


    private MetaNeuron() {
        super();
    }


    public MetaNeuron(Neuron p) {
        super(p);
    }


    public MetaNeuron(Model model, String label) {
        super(model, label);
    }


    @Override
    public boolean isRecurrent(boolean isNegativeSynapse) {
        return false;
    }


    public boolean isWeak(Synapse s, Synapse.State state) {
        double w = s.getLimit(state) * s.getWeight(state);

       return w < getBias();
    }


    public String getType() {
        return "M";
    }


    @Override
    public void dumpStat() {
        System.out.println("OUT:  " +getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")");

        for(Synapse s: getProvider().getActiveInputSynapses()) {
            TSynapse ts = (TSynapse) s;

            System.out.println("IN:  " + ts.getInput().getLabel());
            System.out.println("     Freq:(" + ts.freqToString() + ")");
            System.out.println("     PXi(" + ts.pXiToString() + ")");
            System.out.println("     PXout(" + ts.pXoutToString() + ")");
            System.out.println("     P(" + ts.propToString() + ")");
            System.out.println("     Rel:" + ts.getReliability());
        }
    }


    public ActivationFunction getActivationFunction() {
        return ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
    }


    public double getTotalBias(Synapse.State state) {
        return getBias(state) - synapseSummary.getPosSum(state);
    }

    public String typeToString() {
        return "META";
    }


    public String toStringWithSynapses() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toStringWithSynapses());
        for (MappingLink ml : targetNeurons.values()) {
            sb.append("  ");
            sb.append(ml.toString());
            sb.append("\n");
        }

        return sb.toString();
    }


    public static void induce(Model model, int threadId) {
        for(Neuron n: model.getActiveNeurons()) {
            if(n.get() instanceof ExcitatoryNeuron) {
                List<ExcitatorySynapse> candidateSynapses = n
                        .getActiveOutputSynapses()
                        .stream()
                        .map(s -> (ExcitatorySynapse) s)
                        .collect(Collectors.toList());

                double coveredScore = coveredSum(candidateSynapses);

                if (coveredScore > COVERED_THRESHOLD) {
                    createNewMetaNeuron(model, threadId, n, candidateSynapses);
                }
            }
        }
    }


    public static void createNewMetaNeuron(Model model, int threadId, Neuron inputNeuron, List<ExcitatorySynapse> candidateSynapses) {
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


    public static double coveredSum(List<ExcitatorySynapse> syns) {
        double sum = 0.0;
        for(ExcitatorySynapse s: syns) {
            sum += s.getUncovered();
        }
        return sum;
    }



    public void train(int threadId) {
        do {
            double diff;
            do {
                for (Synapse s : getProvider().getActiveInputSynapses()) {
                    MetaSynapse ms = (MetaSynapse) s;

                    ms.updateWeight();
                }
                updateBias();

                commit(getInputSynapses());

                diff = 0.0;
                for (MappingLink ml : targetNeurons.values()) {
                    diff += ml.computeNij();
                }
            } while (diff > 1.0);

            propagateToOutgoingInhibNeurons(threadId);

        } while(induceInputInhibNeurons() || expand(threadId));
    }


    private void propagateToOutgoingInhibNeurons(int threadId) {
        for(Synapse s: getProvider().getActiveOutputSynapses()) {
            if(s instanceof MetaInhibSynapse) {
                MetaInhibSynapse ms = (MetaInhibSynapse) s;

                InhibitoryNeuron in = (InhibitoryNeuron) ms.getOutput().get();

                in.train(threadId, this);
            }
        }
    }


    private double getNijSum() {
        double sum = 0.0;
        for(MappingLink ml: targetNeurons.values()) {
            sum += ml.nij;
        }
        return sum;
    }


    private boolean expand(int threadId) {
        boolean changed = false;
        double sumNij = getNijSum();

        for(Synapse s: getInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) s;

            if(ms.getWeight() > 0.5) {
                for (MetaSynapse.MappingLink ml : ms.targetSynapses.values()) {
                    ExcitatorySynapse ts = ml.targetSynapse;

                    for (Relation.Key trk : ts.getRelations()) {
                        Integer relSynId = trk.getRelatedId();

                        if (relSynId != OUTPUT) {
                            ExcitatorySynapse relTargetSyn = (ExcitatorySynapse) ts.getOutput().getSynapseById(relSynId);

                            List<Neuron> candidates = new ArrayList<>();
                            collectCandidates(candidates, relTargetSyn.getInput());

                            for (Neuron cand : candidates) {
                                MetaSynapse relMetaSyn = lookupMetaSynapse(relTargetSyn, cand);

                                Relation.Key mrk = ms.getRelation(new Relation.Key(relMetaSyn.getId(), trk.getRelation(), trk.getDirection()));
                                WeightedRelation wtr = (WeightedRelation) trk.getRelation();

                                if (mrk == null) {
                                    Relation mr = wtr.copy();

                                    mr.link(getProvider(), ms.getId(), relMetaSyn.getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }


    private boolean induceInputInhibNeurons() {
        boolean changed = false;
        for(Synapse s: getInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) s;

            if (ms.getWeight() > 0.5) {
                Map<InduceKey, List<MetaSynapse>> tmp = new TreeMap<>();
                for(Relation.Key rk : ms.getRelations()) {
                    MetaSynapse relMS = (MetaSynapse) getProvider().getSynapseById(rk.getRelatedId());
                    WeightedRelation wr = (WeightedRelation) rk.getRelation();

                    Relation keyRel = wr.getKeyRelation();
                    if (keyRel instanceof PositionRelation) {
                        PositionRelation pr = (PositionRelation) keyRel;
                        InduceKey ik = new InduceKey(pr.fromSlot, keyRel, rk.getDirection());

                        List<MetaSynapse> l = tmp.get(ik);
                        if (l == null) {
                            l = new ArrayList<>();
                            tmp.put(ik, l);
                        }

                        l.add(relMS);
                    }
                }
            }
        }
        return changed;
    }


    private MetaSynapse lookupMetaSynapse(ExcitatorySynapse targetSyn, Neuron cand) {
        MetaSynapse.MappingLink ml = targetSyn.getMetaSynapse(cand, getProvider());

        if (ml != null) {
            return ml.metaSynapse;
        } else {
            int newSynId = getNewSynapseId();

            MetaSynapse metaSyn = new MetaSynapse(cand, getProvider(), newSynId, getModel().charCounter);
            ml = new MetaSynapse.MappingLink(metaSyn, targetSyn);

            metaSyn.targetSynapses.put((ExcitatoryNeuron) targetSyn.getOutput().get(), ml);
            targetSyn.metaSynapses.put(metaSyn, ml);

            metaSyn.link();

            return metaSyn;
        }
    }

/*
    private void applyRefinement(int threadId, Refinement ref, List<ExcitatorySynapse> targetSyns) {
        int newSynId = getNewSynapseId();

        if(ref.target == null) {
            ref.target = InhibitoryNeuron.induceIncoming(getModel(), threadId, targetSyns);
        }

        MetaSynapse nms = new MetaSynapse(ref.target.getProvider(), getProvider(), newSynId, getModel().charCounter);

        if(nms != null) {
            nms.link();

            for(ExcitatorySynapse ts: targetSyns) {
                new MetaSynapse.MappingLink(nms, ts).link();
            }

            Relation newRelation = null; //ref.keyRelation.newInstance();
            newRelation.link(getProvider(), ref.anchor.getId(), nms.getId());
        }
    }
*/

    public static void collectCandidates(List<Neuron> results, Neuron n) {
        results.add(n);

        for(Synapse s: n.getActiveOutputSynapses()) {
            if(s.getOutput().getType() == INeuron.Type.INHIBITORY) {
                collectCandidates(results, n);
            }
        }
    }


    public void updateBias() {
        double sum = 0.0;
        double norm = 0.0;

        for(MappingLink nml: targetNeurons.values()) {
            double nij = nml.nij;
            double bj = nml.targetNeuron.getBias();

            sum += nij * bj;
            norm += nij;
        }

        setBias(sum / norm);
    }


    protected MetaActivation createActivation(Document doc) {
        return new MetaActivation(doc, this);
    }


    public InhibitoryNeuron getInhibitoryNeuron() {
        return inhibitoryNeuron;
    }


    public void setInhibitoryNeuron(InhibitoryNeuron inhibitoryNeuron) {
        this.inhibitoryNeuron = inhibitoryNeuron;
    }


    public void train(Config c, Option o) {
        transferMetaSynapses(c, o);

        super.train(c, o);
    }


    @Override
    public boolean isMature(Config c) {
        return false;
    }
/*
    public TNeuron getInputTargets(TDocument doc, Option in) {
        return doc.metaActivations.get(in);
    }
*/
    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        ExcitatoryNeuron targetNeuron = createMetaNeuronTarget(metaAct, callback);

        return targetNeuron;
    }


    public ExcitatoryNeuron createMetaNeuronTarget(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return createMetaNeuronTarget(metaAct.getDocument(), callback.apply(metaAct));
    }


    public ExcitatoryNeuron createMetaNeuronTargetFromLabel(Document doc, String label, ExcitatoryNeuron targetNeuron) {
        return createMetaNeuronTarget(doc, new ExcitatoryNeuron(getModel(), getInhibitoryNeuron().getLabel().substring(2) + "-" + label));
    }


    public ExcitatoryNeuron createMetaNeuronTarget(Document doc, ExcitatoryNeuron targetNeuron) {
        System.out.println("New Meta Neuron Instance: " + targetNeuron.getLabel() + " Bias:" + getBias());

        initMetaNeuronTarget(doc, targetNeuron);

        return targetNeuron;
    }


    private void initMetaNeuronTarget(Document doc, ExcitatoryNeuron tn) {
        tn.setInhibitoryNeuron(getInhibitoryNeuron());
        new MappingLink(this, tn, 1.0).link();

        transferNegativeMetaInputSynapses(doc, tn);
        transferMetaOutputSynapses(doc, getInhibitoryNeuron(), tn);

        tn.trainingBias = trainingBias;
        tn.setBias(getBias() - trainingBias);

        tn.computeOutputRelations();

        tn.commit(tn.getProvider().getActiveInputSynapses());
    }


    private void transferNegativeMetaInputSynapses(Document doc, ExcitatoryNeuron targetNeuron) {
        for(Synapse templateSynapse: getProvider().getActiveInputSynapses()) {
            MetaSynapse ms = (MetaSynapse) templateSynapse;

            if(ms != null && templateSynapse.isNegative(CURRENT)) {
                ms.transferTemplateSynapse(doc, (TNeuron) templateSynapse.getInput().get(doc), targetNeuron, null);
            }
        }
    }


    private void transferMetaOutputSynapses(Document doc, InhibitoryNeuron inhibNeuron, ExcitatoryNeuron targetNeuron) {
        for(Synapse templateSynapse: getProvider().getActiveOutputSynapses()) {
            if(templateSynapse.getOutput().getId() == inhibNeuron.getId()) {
                MetaInhibSynapse mis = (MetaInhibSynapse) templateSynapse;

                InhibitorySynapse targetSynapse = mis.transferMetaSynapse(doc, targetNeuron);

                List<Synapse> modifiedSynapses = Collections.singletonList(targetSynapse);
                targetSynapse.getOutput().get().commit(modifiedSynapses);
            }
        }
    }


    public void transferMetaSynapses(Config config, Option metaActOption) {
        if(metaActOption.targetNeuron == null) {
            return;
        }

        Document doc = metaActOption.getAct().getDocument();

        metaActOption.inputOptions.entrySet().stream()
                .filter(me -> me.getValue() != null && me.getValue().getP() >= config.getMetaThreshold())
                .forEach(me -> {
                    Link l = me.getKey();
                    Option inOption = me.getValue();

                    MetaSynapse templateSynapse = (MetaSynapse) l.getSynapse();

                    if (templateSynapse != null) {
                        TNeuron inputNeuron = inOption.targetNeuron;

                        templateSynapse.transferTemplateSynapse(doc, inputNeuron, metaActOption.targetNeuron, l);
                    }
                });
    }



    public static class MappingLink {
        public MetaNeuron metaNeuron;
        public ExcitatoryNeuron targetNeuron;

        public double nij;

        public MappingLink(MetaNeuron metaNeuron, ExcitatoryNeuron targetNeuron, double nij) {
            this.metaNeuron = metaNeuron;
            this.targetNeuron = targetNeuron;
            this.nij = nij;
        }


        public void link() {
            metaNeuron.targetNeurons.put(targetNeuron, this);
            targetNeuron.metaNeurons.put(metaNeuron, this);
        }


        public String toString() {
            return "TM \"" + targetNeuron.toString() + "\" nij:" + nij;
        }

        public double computeNij() {
            double max = 0.0;
            for(Synapse s: metaNeuron.getProvider().getActiveInputSynapses()) {
                MetaSynapse ms = (MetaSynapse) s;

                MetaSynapse.MappingLink ml = ms.targetSynapses.get(targetNeuron);
                double targetCoverage = 0.0;
                if(ml != null) {
                    ExcitatorySynapse ts = ml.targetSynapse;
                    targetCoverage = ts.getCoverage();
                }
                max = Math.max(max, ms.getCoverage() - targetCoverage);
            }
            double newNij = 1.0 - max;
            double diff = Math.abs(newNij - nij);
            nij = newNij;

            return diff;
        }
    }
}
