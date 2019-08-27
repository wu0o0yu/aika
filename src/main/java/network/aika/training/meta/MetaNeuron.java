package network.aika.training.meta;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.lattice.Converter;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.MetaModel;
import network.aika.training.PActivation;
import network.aika.training.TDocument;
import network.aika.training.TNeuron;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.inhibitory.InhibitorySynapse;
import network.aika.training.inhibitory.MetaInhibSynapse;
import network.aika.training.relation.WeightedRelation;

import java.util.*;
import java.util.function.Function;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.State.CURRENT;

public class MetaNeuron extends TNeuron {

    public InhibitoryNeuron inhibitoryNeuron;

    public Map<ExcitatoryNeuron, MappingLink> targetNeurons = new TreeMap<>();


    public MetaNeuron(MetaModel model, String label) {
        this(model, label, null);
    }


    public MetaNeuron(MetaModel model, String label, String outputText) {
        super(model, label, outputText, INeuron.Type.EXCITATORY, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT);
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

        Converter.convert(threadId, null, this, getInputSynapses());
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

                    for (Map.Entry<Integer, Relation> me : ts.getRelations().entrySet()) {
                        Integer relSynId = me.getKey();

                        if (relSynId != OUTPUT) {
                            ExcitatorySynapse relTargetSyn = (ExcitatorySynapse) ts.getOutput().getSynapseById(relSynId);

                            List<Neuron> candidates = new ArrayList<>();
                            collectCandidates(candidates, relTargetSyn.getInput());

                            for (Neuron cand : candidates) {
                                MetaSynapse relMetaSyn = lookupMetaSynapse(relTargetSyn, cand);

                                Relation mmr = ms.getRelations().get(relMetaSyn.getId());

                                for (Relation tr : me.getValue().getLeafRelations()) {
                                    WeightedRelation wtr = (WeightedRelation) tr;

                                    Relation mr = null;
                                    if (mmr != null) {
                                        mr = mmr.getRelation(tr);
                                    }

                                    if (mr == null) {
                                        mr = wtr.copy();

                                        Relation.addRelation(ms.getId(), relMetaSyn.getId(), getProvider(), mr);
                                    }
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
                for(Map.Entry<Integer, Relation> me: ms.getRelations().entrySet()) {
                    MetaSynapse relMS = (MetaSynapse) getProvider().getSynapseById(me.getKey());
                    for(Relation rel: me.getValue().getLeafRelations()) {
                        WeightedRelation wr = (WeightedRelation) rel;

                        Relation keyRel = wr.getKeyRelation();
                        if(keyRel instanceof PositionRelation) {
                            PositionRelation pr = (PositionRelation) keyRel;
                            InduceKey ik = new InduceKey(pr.fromSlot, keyRel);

                            List<MetaSynapse> l = tmp.get(ik);
                            if(l == null) {
                                l = new ArrayList<>();
                                tmp.put(ik, l);
                            }

                            l.add(relMS);
                        }

                    }
                }
            }
        }
        return changed;
    }


    public static class InduceKey implements Comparable<InduceKey> {
        public int beginSlot;
        Relation rel;

        public InduceKey(int beginSlot, Relation rel) {
            this.beginSlot = beginSlot;
            this.rel = rel;
        }

        @Override
        public int compareTo(InduceKey ik) {
            int r = Integer.compare(beginSlot, ik.beginSlot);
            if(r != 0) return r;
            return rel.compareTo(ik.rel);
        }
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

            Relation newRelation = null; //ref.rel.newInstance();
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


    protected Activation createActivation(Document doc, Map<Integer, Position> slots) {
        return new PActivation(doc, this, slots);
    }


    public InhibitoryNeuron getInhibitoryNeuron() {
        return inhibitoryNeuron;
    }


    public void setInhibitoryNeuron(InhibitoryNeuron inhibitoryNeuron) {
        this.inhibitoryNeuron = inhibitoryNeuron;
    }


    public void generateNeuron(Activation seedAct) {

    }

    @Override
    public boolean isMature() {
        return false;
    }

    public List<? extends TNeuron> getInputTargets(TDocument doc, Option in) {
        return doc.metaActivations.get(in);
    }

    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        ExcitatoryNeuron targetNeuron = createMetaNeuronTarget(metaAct, callback);

        if (isOutputMetaNeuron()) {
            targetNeuron.setOutputText(metaAct.getText());
        }
        return targetNeuron;
    }


    public ExcitatoryNeuron createMetaNeuronTarget(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return createMetaNeuronTarget(metaAct.getDocument(), callback.apply(metaAct));
    }


    public ExcitatoryNeuron createMetaNeuronTargetFromLabel(Document doc, String label, ExcitatoryNeuron targetNeuron) {
        return createMetaNeuronTarget(doc, new ExcitatoryNeuron(getModel(), getInhibitoryNeuron().getLabel().substring(2) + "-" + label, null));
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
        Converter.convert(doc.getThreadId(), doc, tn, tn.getProvider().getActiveInputSynapses());
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

                InhibitorySynapse targetSynapse = mis.transferTemplateSynapse(doc, targetNeuron);

                List<Synapse> modifiedSynapses = Collections.singletonList(targetSynapse);
                targetSynapse.getOutput().get().commit(modifiedSynapses);
                Converter.convert(doc.getThreadId(), doc, targetSynapse.getOutput().get(), modifiedSynapses);
            }
        }
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
