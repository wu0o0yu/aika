package network.aika.training.excitatory;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.State;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.*;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.training.relation.RelationStatistic;
import network.aika.training.relation.WeightedRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static network.aika.neuron.INeuron.Type.EXCITATORY;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.Synapse.State.NEXT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.activation.link.Link.INPUT_COMP;


public class ExcitatoryNeuron extends TNeuron {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);


    public InhibitoryNeuron inhibitoryNeuron;

    public Map<MetaNeuron, MetaNeuron.MappingLink> metaNeurons = new TreeMap<>();

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

        public double getActDelta(Input l, Option out) {
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
        double getActDelta(Input l, Option out);
    }


    public ExcitatoryNeuron(MetaModel model, String label) {
        super(model, label, null, EXCITATORY, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT);
    }


    public ExcitatoryNeuron(MetaModel model, String label, String outputText) {
        super(model, label, outputText, EXCITATORY, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT);
    }


    public String typeToString() {
        return "EXCITATORY";
    }


    public ExcitatorySynapse createOrLookupSynapse(Document doc, MetaSynapse ms, Neuron inputNeuron) {
        inputNeuron.get(doc);

        ExcitatorySynapse synapse = (ExcitatorySynapse) getProvider().selectInputSynapse(
                inputNeuron,
                s -> ((ExcitatorySynapse)s).isMappedToMetaSynapse(ms)
        );

        if(synapse == null) {
            synapse = new ExcitatorySynapse(inputNeuron, getProvider(), getNewSynapseId());

            synapse.link();
        }
        return synapse;
    }


    public InhibitoryNeuron getInhibitoryNeuron() {
        return inhibitoryNeuron;
    }


    public void setInhibitoryNeuron(InhibitoryNeuron inhibitoryNeuron) {
        this.inhibitoryNeuron = inhibitoryNeuron;
    }


    public void train(Config c, Option o) {
        super.train(c, o);

        createCandidateSynapses(c, o);

        trainLTL(c, o);
    }



    // TODO: entfernen und durch transferMetaSynapses ersetzen.
    public Option init(Option inputOpt) {
        Activation iAct = inputOpt.getAct();
        Document doc = iAct.getDocument();

        setBias(2.0);


        int actBegin = iAct.getSlot(BEGIN).getFinalPosition();
        lastCount += actBegin;

        ExcitatorySynapse s = new ExcitatorySynapse(iAct.getNeuron(), getProvider(), getProvider().getNewSynapseId(), actBegin);

        s.updateDelta(doc, 2.0,  0.0);
        s.setInactive(CURRENT, false);
        s.setInactive(NEXT, false);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Activation targetAct = new Activation(doc, this, new TreeMap<>(iAct.getSlots()));
        register(targetAct);

        Link l = new Link(s, inputOpt.getAct(), targetAct);

        targetAct.addLink(Direction.OUTPUT, l);

        Option targetOpt = new Option(null, targetAct, null);
        targetAct.rootOption = targetOpt;
        targetAct.currentOption = targetOpt;
        targetAct.finalOption = targetOpt;

        targetOpt.link(l, inputOpt);

        double net = getBias();
        double value = getActivationFunction().f(net);

        targetAct.setUpperBound(value);
        targetOpt.setP(inputOpt.getP());
//        targetOpt.state = targetAct.computeValueAndWeight(0);
        targetOpt.setState(new State(value, value, net, inputOpt.getState().fired + 1, 0.0));

        return targetOpt;
    }


    private void createCandidateSynapses(Config c, Option targetOpt) {
        if(log.isDebugEnabled()) {
            log.debug("Created Synapses for Neuron: " + targetOpt.getAct().getINeuron().getId() + ":" + targetOpt.getAct().getINeuron().getLabel());
        }

        TreeMap<Link, Option> tmp = new TreeMap<>(INPUT_COMP); // Vermutlich aufgrund einer Conc. Mod. Exception notwendig.
        tmp.putAll(targetOpt.inputOptions);
        for(Map.Entry<Link, Option> me: tmp.entrySet()) {
            Link l = me.getKey();
            Option o = me.getValue();

            if (!l.isInactive() && !l.isNegative(CURRENT) && checkStrength(l)) {
                Set<Activation> conflicts = getConflicts(l.getInput());

                for (Map.Entry<Integer, Position> mea : l.getInput().getSlots().entrySet()) {
                    Position p = mea.getValue();

                    for (Activation inputAct : p.getActivations()) {  // TODO: Other Relations than EQUAL
                        if (inputAct != l.getInput() && inputAct != targetOpt.getAct() && !conflicts.contains(inputAct)) {
                            Option inputOpt = getMaxOption(inputAct);

                            createCandidateSynapse(c, inputOpt, targetOpt);
                        }
                    }
                }
            }
        }
    }


    public Set<Activation> getConflicts(Activation act) {
        return act.getOutputLinks()
                .filter(cl -> !cl.isNegative(CURRENT))
                .map(cl -> cl.getInput())
                .collect(Collectors.toSet());
    }


    private Option getMaxOption(Activation inputAct) {
        return inputAct
                .getOptions()
                .stream()
                .max(Comparator.comparingDouble(o -> o.getP()))
                .orElse(null);
    }


    private void createCandidateSynapse(Config c, Option inputOpt, Option targetOpt) {
        Activation targetAct = targetOpt.getAct();
        Neuron targetNeuron = targetAct.getNeuron();

        Activation iAct = inputOpt.getAct();
        Neuron inputNeuron = iAct.getNeuron();

        if(checkIfSynapseExists(inputOpt, targetOpt)) {
            return;
        }

        if(!((TNeuron) inputOpt.getAct().getINeuron()).isMature(c)) {
            return;
        }

        int synId = targetNeuron.getNewSynapseId();
        int lastCount = iAct.getSlot(BEGIN).getFinalPosition();

        ExcitatorySynapse s;
        if(inputOpt.getAct().getINeuron() instanceof InhibitoryNeuron && checkSelfReferencing(targetOpt, inputOpt)) {
            s = new NegExcitatorySynapse(inputNeuron, targetNeuron, synId, lastCount);
        } else {
            s = new ExcitatorySynapse(inputNeuron, targetNeuron, synId, lastCount);
        }

        s.setInactive(CURRENT, true);
        s.setInactive(NEXT, true);

        s.setRecurrent(inputOpt.checkSelfReferencing(targetOpt));

        establishRelations(inputOpt, targetOpt, s);

        s.link();

        if(log.isDebugEnabled()) {
            log.debug("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Link l = new Link(s, inputOpt.getAct(), targetAct);

        targetAct.addLink(Direction.OUTPUT, l);
        inputOpt.getAct().addLink(Direction.INPUT, l);
        targetOpt.link(l, inputOpt);
    }


    public boolean isMature(Config c) {
        Synapse maxSyn = getMaxInputSynapse(CURRENT);
        if(maxSyn == null) {
            return false;
        }

        TSynapse se = (TSynapse) maxSyn;

        return se.getCounts()[1] >= c.getMaturityThreshold();  // Sign.NEG, Sign.POS
    }


    private boolean checkIfSynapseExists(Option inputOpt, Option targetOpt) {
        for(Map.Entry<Link, Option> me: targetOpt.inputOptions.entrySet()) {
            Link l = me.getKey();

            if(l.getInput() == inputOpt.getAct()) {
                return true;
            }
        }
        return false;
    }


    private void establishRelations(Option inputOpt, Option targetOpt, Synapse s) {
        for(Map.Entry<Link, Option> me: targetOpt.inputOptions.entrySet()) {
            Link l = me.getKey();
            for(Map.Entry<Integer, Position> mea: l.getInput().getSlots().entrySet()) {
                for(Map.Entry<Integer, Position> meb: inputOpt.getAct().getSlots().entrySet()) {
                    if(mea.getValue() == meb.getValue() || mea.getValue().getFinalPosition() == meb.getValue().getFinalPosition()) {
                        WeightedRelation rel = new WeightedRelation(
                                new PositionRelation.Equals(meb.getKey(), mea.getKey()),
                                new RelationStatistic(1.0),
                                l.getSynapse().getId(),
                                s.getId()
                        );

                        rel.link(l.getSynapse(), s);
                    }
                }
            }
        }
    }


    private static boolean checkStrength(Link l) {
        return true;
    }



    public void trainLTL(Config config, Option out) {
        Activation act = out.getAct();
        Document doc = act.getDocument();
        double[] pXout = getP();

        double delta = getReliability() * Math.log(pXout[0]);

        for(Input i : getInputs(out)) {
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
                    double covDelta = config.learnRate * Xi * i.getReliability() * i.getP() * out.getState().value * G;

                    double weightDelta = covDelta * (1.0 / getBias());
                    double biasDelta = covDelta * -(si.getWeight() / Math.pow(getBias(), 2.0));

                    si.updateDelta(
                            doc,
                            weightDelta,
                            0.0
                    );
                    updateBiasDelta(biasDelta);
                }
            }
        }

        if(delta == 0.0) {
            return;
        }

        for(Input l : getInputs(out)) {
            double weightDelta = 0.0;
            double biasDelta = 0.0;

            for (XlMode u : XlMode.values()) {
                if(delta < 0.0 && u.getK() == Sign.POS || delta > 0.0 && u.getK() == Sign.NEG) {
                    continue;
                }

                double IGDelta = u.getActDelta(l, out) * delta;

                double d = config.learnRate * out.getP() * l.getP() * l.getReliability() * IGDelta;

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
                    weightDelta,
                    0.0
            );
            updateBiasDelta(biasDelta);

            sl.setInactive(NEXT, sl.getWeight(NEXT) <= 0.0);
        }
    }


    public void computeOutputRelations() {
        clearOutputRelations();

        Synapse maxSyn = getMaxInputSynapse(NEXT);

        if(maxSyn != null) {
            followRelationsAndLink(this, maxSyn, Dir.BEFORE, BEGIN);
            followRelationsAndLink(this, maxSyn, Dir.AFTER, END);
        }
    }


    private static void followRelationsAndLink(INeuron n, Synapse s, Dir dir, int slot) {
        Synapse result = s;
        Synapse oldResult = null;
        while(result != oldResult) {
            oldResult = result;
            result = followRelation(result, dir);
        }
        new PositionRelation.Equals(slot, slot).link(n.getProvider(), result.getId(), OUTPUT);
    }


    private static Synapse followRelation(Synapse s, Dir dir) {
        for(Relation.Key rk : s.getRelations()) {
            Relation rel = rk.getRelation();
            if (rel instanceof WeightedRelation) {
                rel = ((WeightedRelation) rel).keyRelation;
            }

            if (rel instanceof PositionRelation.Equals) {
                PositionRelation.Equals r = (PositionRelation.Equals) rel;

                if ((r.fromSlot == BEGIN && dir == Dir.BEFORE) || (r.fromSlot == END && dir == Dir.AFTER)) {
                    Synapse relSyn = s.getOutput().getSynapseById(rk.getRelatedId());
                    if (relSyn != null && !relSyn.isInactive() && !relSyn.isWeak(NEXT)) {
                        return relSyn;
                    }
                }
            }
        }
        return s;
    }


    public static double actFDelta(Option o) {
        double net = o.getState().net;
        if(net <= 0.0) return 0.0;

        switch(o.getAct().getINeuron().getActivationFunction()) {
            case RECTIFIED_HYPERBOLIC_TANGENT:
                return 1.0 - Math.pow(Math.tanh(net), 2.0);
            case RECTIFIED_LINEAR_UNIT:
                return 1.0;
        }

        return 0.0;
    }


    private enum Dir {
        BEFORE,
        AFTER
    }


    public ExcitatoryNeuron getTargetNeuron(Activation metaAct, Function<Activation, ExcitatoryNeuron> callback) {
        return this;
    }


    public double getTrainingNetValue(Option o) {
        return o.getState().net + trainingBias; //  + getInactiveWeights(o)
    }


    public List<Input> getInputs(Option out) {
        ArrayList<Input> results = new ArrayList<>();
        Set<Synapse> inputSynapses = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        inputSynapses.addAll(out.getAct().getINeuron().getInputSynapses());

        for(Map.Entry<Link, Option> me: out.inputOptions.entrySet()) {
            Input i = new Input(me.getKey(), me.getValue());

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
        Option o;
        ExcitatorySynapse s;

        public Input(Link l, Option o)  {
            this.l = l;
            this.o = o;
            this.s = (ExcitatorySynapse) l.getSynapse();
        }


        public Input(Synapse s) {
            this.s = (ExcitatorySynapse) s;
        }

        public int getId() {
            return s.getId();
        }

        public ExcitatorySynapse getSynapse() {
            return s;
        }

        public TNeuron getNeuron() {
            return (TNeuron) s.getInput().get();
        }

        public String getLabel() {
            return s.getInput().getLabel();
        }

        public double getP() {
            return o != null ? o.getP() : 1.0;
        }

        public double getReliability() {
            return Math.min(((TNeuron)s.getInput().get()).getReliability(), s.getReliability());
        }

        public double[] getPXi() {
            return ((TNeuron) getSynapse().getInput().get()).getP();
        }

        public double getX(Sign s) {
            return s.getX(o != null ? o.getState().value : 0.0);
        }

        public String toString() {
            return "id:" + getId() + " " + getSynapse().getInput().getLabel();
        }
    }
}
