package network.aika.training;

import network.aika.Document;
import network.aika.lattice.Converter;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.link.Linker;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.relation.MultiRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.training.relation.WeightedRelation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.Synapse.OUTPUT;

public class TDocument extends Document {

    public static class Config {
        public double learnRate;

        public Config setLearnRate(double learnRate) {
            this.learnRate = learnRate;
            return this;
        }
    }

    public Map<Option, List<ExcitatoryNeuron>> metaActivations = new TreeMap<>();

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
        generateNeurons();
        generateSynapses();

        count();

//        trainMeta();

        trainLTL(c);
    }


    public void generateNeurons() {
        for(Activation seedAct: new ArrayList<>(getActivations(false))) {
            ((TNeuron) seedAct.getINeuron()).generateNeuron(seedAct);
        }
    }


    public void generateSynapses() {
        for(Activation targetAct: new ArrayList<>(getActivations(false))) {
            ((TNeuron) targetAct.getINeuron()).generateSynapses(targetAct);
        }
    }


    public void count() {
        for(Activation act: getActivations(false)) {
            TNeuron n = (TNeuron) act.getINeuron();

            n.initCountValues();
        }

        for(Activation act: getActivations(false)) {
            TNeuron n = (TNeuron) act.getINeuron();
            for (Option o : act.getOptions()) {
                n.count(o);
            }
        }

        for(Activation act: getActivations(false)) {
            TNeuron n = (TNeuron) act.getINeuron();
            n.updateFrequencies(act);
        }

        getModel().charCounter += length();
    }


    public void trainLTL(Config config) {
        for(Activation act: getActivations(false)) {
            if(act.getUpperBound() > 0.0) {
                ((TNeuron)act.getINeuron()).train(act, config);
            }
        }

        computeOutputRelations(this);

        commit();
    }


    private static void computeOutputRelations(Document doc) {
        doc.getModifiedWeights().forEach((n, inputSyns) -> {
            TNeuron tn = (TNeuron) n;
            tn.computeOutputRelations();
        });
    }


    public void trainMeta(double threshold) {
        trainMeta(threshold, act -> new ExcitatoryNeuron(getModel(), act.getLabel(), null));
    }


    public void trainMeta(double threshold, Function<Activation, ExcitatoryNeuron> callback) {
        for (Activation metaAct : getActivations(false)) {
            TNeuron n = (TNeuron) metaAct.getINeuron();
            n.trainMeta(metaAct, threshold, callback);
        }

        processMetaActivations(threshold);
    }


    private void processMetaActivations(double threshold) {
        for(Map.Entry<Option, List<ExcitatoryNeuron>> me: metaActivations.entrySet()) {
            for(ExcitatoryNeuron tn: me.getValue()) {
                transferMetaSynapses(threshold, me.getKey(), tn);
            }
        }

        propagate();
    }

    private void transferMetaSynapses(double threshold, Option metaActOption, ExcitatoryNeuron targetNeuron) {
        TreeMap<Link, List<Synapse>> targetMapping = new TreeMap<>(Link.INPUT_COMP);
        List<Synapse> targetInputSynapses = new ArrayList();

        metaActOption.inputOptions.entrySet().stream()
                .filter(me -> me.getValue() != null && me.getValue().getP() >= threshold)
                .forEach(me -> {
                    Link l = me.getKey();
                    Option inOption = me.getValue();

                    MetaSynapse templateSynapse = (MetaSynapse) l.getSynapse();

                    if (templateSynapse != null) {
                        TNeuron in = (TNeuron) l.getInput().getINeuron();

                        List<? extends TNeuron> inputTargets = in.getInputTargets(this, inOption);

                        for (TNeuron inputNeuron : inputTargets) {
                            TSynapse targetSynapse = templateSynapse.transferTemplateSynapse(this, inputNeuron, targetNeuron, l);

                            if (targetSynapse != null) {
                                transferInputMetaRelations(metaActOption, l, l.getSynapse(), targetSynapse, targetMapping);

                                addTargetMapping(targetMapping, l, targetSynapse);

                                targetInputSynapses.add(targetSynapse);
                            }
                        }
                    }
                });

        targetNeuron.commit(targetInputSynapses);
        Converter.convert(getThreadId(), this, targetNeuron, targetInputSynapses);
    }




    private static void addTargetMapping(TreeMap<Link, List<Synapse>> targetMapping, Link l, Synapse targetSynapse) {
        List<Synapse> ts = targetMapping.get(l);
        if (ts == null) {
            ts = new ArrayList<>();
            targetMapping.put(l, ts);
        }

        ts.add(targetSynapse);
    }


    public static void transferInputMetaRelations(Option metaActOption, Link metaLink, Synapse templateSynapse, TSynapse targetSynapse, Map<Link, List<Synapse>> targetMapping) {
        if (!targetSynapse.isConverted()) {
            for (Map.Entry<Integer, MultiRelation> me : templateSynapse.getRelations().entrySet()) {
                Integer relId = me.getKey();

                for(Relation r: me.getValue().getRelations().values()) {
                    WeightedRelation rel = (WeightedRelation) r;

                    if (relId != OUTPUT) {
                        getRelatedLinks(metaActOption.getAct(), metaLink, relId, rel)
                                .forEach(rl -> {
                                    List<Synapse> syns = targetMapping.get(rl);
                                    if (syns != null) {
                                        WeightedRelation targetRel = rel.createTargetRelation(metaLink.getInput(), rl.getInput());
                                        if (targetRel != null) {
                                            syns.forEach(relTargetSynapse -> {
                                                targetRel.link(relTargetSynapse.getRelations(), targetSynapse.getRelations(), relTargetSynapse.getId(), targetSynapse.getId(), targetSynapse.getOutput());
//                                            Relation.addRelation(targetSynapse.getRelations(), relTargetSynapse.getId(), targetSynapse.getId(), targetSynapse.getOutput(), targetRel);
//                                            Relation.addRelation(relTargetSynapse.getRelations(), targetSynapse.getId(), relTargetSynapse.getId(), targetSynapse.getOutput(), targetRel.invert());

                                                System.out.println("  Transfer Template Relation:" +
                                                        " From: " + relTargetSynapse.getId() +
                                                        " To: " + targetSynapse.getId() +
                                                        " Template Rel: " + rel +
                                                        " Target Rel: " + targetRel
                                                );
                                            });
                                        }
                                    }
                                });
                    }
                }

            }

            targetSynapse.setConverted(true);
        }
    }

    public static void transferOutputMetaRelations(TSynapse templateSynapse, TSynapse targetSynapse, Activation act, Activation relatedAct) {
        if (!targetSynapse.isConverted()) {
            for (Map.Entry<Integer, MultiRelation> me : templateSynapse.getRelations().entrySet()) {
                Integer relId = me.getKey();
                for(Relation leafRel: me.getValue().getRelations().values()) {
                    WeightedRelation rel = (WeightedRelation) leafRel;

                    if (relId == OUTPUT) {
                        WeightedRelation targetRel = rel.createTargetRelation(act, relatedAct);

                        targetRel.link(Relation.getRelationsMap(OUTPUT, targetSynapse.getOutput()), targetSynapse.getRelations(), OUTPUT, targetSynapse.getId(), targetSynapse.getOutput());
//                        Relation.addRelation(targetSynapse.getRelations(), OUTPUT, targetSynapse.getId(), targetSynapse.getOutput(), targetRel);
//                        Relation.addRelation(Relation.getRelationsMap(OUTPUT, targetSynapse.getOutput()), targetSynapse.getId(), OUTPUT, targetSynapse.getOutput(), targetRel.invert());

                        System.out.println("  Transfer Template Relation:" +
                                " From: OUTPUT" +
                                " To: " + targetSynapse.getId() +
                                " Template Rel: " + rel +
                                " Target Rel: " + targetRel
                        );
                    }
                }
            }
        }
    }


    private static Collection<Link> getRelatedLinks(Activation metaAct, Link metaLink, Integer relId, Relation rel) {
        return metaAct.getInputLinks()
                .filter(l -> l.getSynapse().getId() == relId)
                .filter(l -> rel.test(metaLink.getInput(), l.getInput(), false))
                .collect(Collectors.toList());
    }
}
