package network.aika.training.excitatory;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Utils;
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
    public static boolean DEBUG = true;
    public static boolean DEBUG1 = true;
    public static boolean DEBUG2 = true;

    public static int MATURITY_THRESHOLD = 10;

    public InhibitoryNeuron inhibitoryNeuron;

    public Map<MetaNeuron, MetaNeuron.MappingLink> metaNeurons = new TreeMap<>();

    enum WeightBias {
        WEIGHT,
        BIAS
    }

    enum XlMode {
        WEIGHT_KPOS_UPOS((l, out) -> l.getX(Sign.POS) * actFDelta(out), Sign.POS, WeightBias.WEIGHT),
        WEIGHT_KPOS_UNEG((l, out) -> -l.getX(Sign.NEG) * actFDelta(out), Sign.POS, WeightBias.WEIGHT),
        BIAS_KPOS_UPOS((l, out) -> 0.0, Sign.POS, WeightBias.BIAS),
        BIAS_KPOS_UNEG((l, out) -> l.getX(Sign.NEG) * actFDelta(out), Sign.POS, WeightBias.BIAS),
        WEIGHT_KNEG_UPOS((l, out) -> 0.0, Sign.NEG, WeightBias.WEIGHT),
        WEIGHT_KNEG_UNEG((l, out) -> -l.getX(Sign.NEG) * actFDelta(out), Sign.NEG, WeightBias.WEIGHT),
        BIAS_KNEG_UPOS((l, out) -> l.getX(Sign.POS) * actFDelta(out), Sign.NEG, WeightBias.BIAS),
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


    public Option init(Option inputOpt) {
        Activation iAct = inputOpt.getAct();
        Document doc = iAct.getDocument();
        if(DEBUG) {
            System.out.println("Created Neuron: " + getId() + ":" +getLabel());
        }

        setBias(2.0);


        int actBegin = iAct.lookupSlot(BEGIN).getFinalPosition();
        lastCount += actBegin;

        ExcitatorySynapse s = new ExcitatorySynapse(iAct.getNeuron(), getProvider(), getProvider().getNewSynapseId(), actBegin);

        s.updateDelta(doc, 2.0,  0.0);
        s.setInactive(CURRENT, false);
        s.setInactive(NEXT, false);

        s.link();

        if(DEBUG) {
            System.out.println("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        commit(Collections.singletonList(s));

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
        targetOpt.p = inputOpt.p;
//        targetOpt.state = targetAct.computeValueAndWeight(0);
        targetOpt.setState(new State(value, value, net, inputOpt.getState().fired + 1, 0.0));

        return targetOpt;
    }


    int debugCounter = 0;

    public void train(Activation act, TDocument.Config config, TDocument.DebugDocument ddoc) {
        DebugAct dact = null;
        if (DEBUG1) {
            dact = new DebugAct(act);
            ddoc.acts.put(act, dact);
        }

        if(DEBUG) {
            System.out.println("Train Excitatory: " + act.toString() + "  DBG:" + debugCounter);
        }
        debugCounter++;

        for (Option out : act.getOptions()) {
            trainSynapse(config, out, dact);

            if (DEBUG) {
                dumpRelations();
            }

        }

        if(DEBUG || DEBUG2) {
            System.out.println();
        }
    }


    public void generateSynapses(Activation act) {
        for (Option o : act.getOptions()) {
            collectCandidateSynapses(o);
        }
    }

    private void collectCandidateSynapses(Option targetOpt) {
        if(DEBUG) {
            System.out.println("Created Synapses for Neuron: " + targetOpt.getAct().getINeuron().getId() + ":" + targetOpt.getAct().getINeuron().getLabel());
        }

        TreeMap<Link, Option> tmp = new TreeMap<>(INPUT_COMP);
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

                            createCandidateSynapse(inputOpt, targetOpt);
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
        return inputAct.getOptions().stream().max(Comparator.comparingDouble(o -> o.p)).orElse(null);
    }


    private void createCandidateSynapse(Option inputOpt, Option targetOpt) {
        Activation targetAct = targetOpt.getAct();
        Activation iAct = inputOpt.getAct();

        if(checkIfSynapseExists(inputOpt, targetOpt)) {
            return;
        }

        if(!((TNeuron) inputOpt.getAct().getINeuron()).isMature()) {
            return;
        }

        int synId = targetAct.getNeuron().getNewSynapseId();
        int lastCount = iAct.lookupSlot(BEGIN).getFinalPosition();

        ExcitatorySynapse s;
        if(inputOpt.getAct().getINeuron() instanceof InhibitoryNeuron && checkSelfReferencing(targetOpt, inputOpt)) {
            s = new NegExcitatorySynapse(iAct.getNeuron(), targetAct.getNeuron(), synId, lastCount);
        } else {
            s = new ExcitatorySynapse(iAct.getNeuron(), targetAct.getNeuron(), synId, lastCount);
        }

        s.setInactive(CURRENT, true);
        s.setInactive(NEXT, true);
        s.setRecurrent(inputOpt.getState().fired > targetOpt.getState().fired);

        establishRelations(inputOpt, targetOpt, s);

        s.link();

        if(DEBUG) {
            System.out.println("    Created Synapse: " + s.getInput().getId() + ":" + s.getInput().getLabel() + " -> " + s.getOutput().getId() + ":" + s.getOutput().getLabel());
        }

        Link l = new Link(s, inputOpt.getAct(), targetAct);

        targetAct.addLink(Direction.OUTPUT, l);
        inputOpt.getAct().addLink(Direction.INPUT, l);
        targetOpt.link(l, inputOpt);
    }


    public boolean isMature() {
        Synapse maxSyn = getMaxInputSynapse(CURRENT);
        if(maxSyn == null) {
            return false;
        }

        TSynapse se = (TSynapse) maxSyn;

        return se.getCounts()[1] >= MATURITY_THRESHOLD;  // Sign.NEG, Sign.POS
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

                        Relation.addRelation(s.getRelations(), l.getSynapse().getId(), s.getId(), null, rel);
                        Relation.addRelation(l.getSynapse().getRelations(), s.getId(), l.getSynapse().getId(), null, rel.invert());
                    }
                }
            }
        }
    }


    private static boolean checkStrength(Link l) {
        return true;
    }


    private void dumpRelations() {
        for(Synapse s: getInputSynapses()) {
            for(Map.Entry<Integer, Relation> me: s.getRelations().entrySet()) {
                if(s.getId() <= me.getKey() || me.getKey() == OUTPUT) {
                    System.out.println("   Relation: From:" + s.getId() + " To:" + (me.getKey() == OUTPUT ? "OUTPUT" : me.getKey()) + " Rel:" + me.getValue());
                }
            }
        }
    }


    public void trainSynapse(TDocument.Config config, Option out, DebugAct dact) {
        Activation act = out.getAct();
        Document doc = act.getDocument();
        double[] pXout = getP();

        double delta = getReliability() * Math.log(pXout[0]);

        for(Input i : getInputs(out)) {
            TSynapse si = i.getSynapse();
            double[] pXiXout = si.getPXiXout();
            // PXi und PXout aus den beiden unterschiedlichen Quellen müssen annähernd gleich sein.
            double[] pXi = i.getPXi();

            double diff = Math.abs(Math.log(i.getPXi()[0]) - Math.log(pXiXout[0] + pXiXout[2]));
/*        if(diff > 0.001) {
            System.out.println("Diff - " + i.getLabel());
            System.out.println("  Diff:" + diff + "  pXin:" + pXi[0] + "  pXis:" + (pXiXout[0] + pXiXout[2]) + "  nRel:" + i.getNeuron().getReliability() + "  sRel:" + i.getSynapse().getReliability());
            System.out.println("  Nn:" + i.getNeuron().N + "  Ns:" + i.getSynapse().N);
        }
*/

            double covi = i.getSynapse().getCoverage();
            if (covi == 0.0) {
                continue;
            }

            System.out.print("  i:" + i.getLabel() + " covi:" + covi + " iRel:" + i.getReliability() + " p:" + i.getP());

            for(Sign k : Sign.values()) {
                int sii = k.ordinal();
                double Xi = i.getX(k);

                if(pXiXout[sii] == 0.0) {
                    continue;
                }
                double G = Math.log(pXiXout[sii]) - (Math.log(pXi[sii]) + Math.log(pXout[0]));

                double d = Xi * i.getReliability() * i.getP() * covi * G;
                delta += d;

                System.out.print("  " + k.name() + ":(d:" + d + " Xi:" + Xi + " G:" + G + ")");

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
            System.out.println();
        }

        if(delta == 0.0) {
            return;
        }

        if (DEBUG) {
            System.out.println("  delta:" + delta);
            System.out.println();
        }


        for(Input l : getInputs(out)) {
            double weightDelta = 0.0;
            double biasDelta = 0.0;

            LDebugSynapse dsyn = getlDebugSynapse(dact, l.getSynapse());

            for (XlMode u : XlMode.values()) {
                if(delta < 0.0 && u.getK() == Sign.POS || delta > 0.0 && u.getK() == Sign.NEG) {
                    continue;
                }

                double IGDelta = u.getActDelta(l, out) * delta;

                double d = config.learnRate * out.p * l.getP() * l.getReliability() * IGDelta;

                if (d == 0.0) {
                    continue;
                }

                if (u.getWB() == WeightBias.WEIGHT) {
                    weightDelta += d;
                    biasDelta += d;
                } else if (u.getWB() == WeightBias.BIAS) {
                    biasDelta += d;
                }

                if (DEBUG) {
                    System.out.println("    l:" + l.getLabel() + " u:" + u.name() + (u.getWB() == WeightBias.WEIGHT ? " W:" + l.getSynapse().getWeight() : " B:" + out.getAct().getINeuron().getBias()) + " d:" + d);
                }

                if (DEBUG1) {
                    XlModeParameters xlParams = dsyn.lookup(u);
                    xlParams.lRel = l.getReliability();
                    xlParams.pOut = out.p;
                    xlParams.pl = l.getP();
                    xlParams.actDelta = u.getActDelta(l, out);
                    xlParams.IGDelta = IGDelta;
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
        for(Map.Entry<Integer, Relation> me: s.getRelations().entrySet()) {
            Relation rel = me.getValue();

            if(rel instanceof PositionRelation.Equals) {
                PositionRelation.Equals r = (PositionRelation.Equals) rel;

                if((r.fromSlot == BEGIN && dir == Dir.BEFORE) || (r.fromSlot == END && dir == Dir.AFTER)) {
                    Synapse relSyn = s.getOutput().getSynapseById(me.getKey());
                    if(relSyn != null && !relSyn.isInactive() && !relSyn.isWeak(NEXT)) {
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


    private LDebugSynapse getlDebugSynapse(DebugAct dact, Synapse sl) {
        LDebugSynapse dsyn = null;
        if (DEBUG1) {
            dsyn = new LDebugSynapse();

            dact.lSynapses.put(sl, dsyn);
        }
        return dsyn;
    }


    private IDebugSynapse getiDebugSynapse(DebugAct dact, Input i) {
        IDebugSynapse dsyn = null;
        if(DEBUG1) {
            dsyn = new IDebugSynapse();

            dact.getIDebugSynapses().put(i.getSynapse(), dsyn);
        }
        return dsyn;
    }


    public class DebugAct {
        Map<Synapse, IDebugSynapse> iSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
        Map<Synapse, LDebugSynapse> lSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);

        INeuron n;

        public Map<Synapse, IDebugSynapse> getIDebugSynapses() {
            return iSynapses;
        }

        public DebugAct(Activation act) {
            n = act.getINeuron();
        }
    }


    public class LDebugSynapse {
        public double N;
        public double pl;


        public TreeMap<XlMode, XlModeParameters> xlModeParameters = new TreeMap<>();

        public XlModeParameters lookup(XlMode m) {
            XlModeParameters p = xlModeParameters.get(m);
            if(p == null) {
                p = new XlModeParameters();
                xlModeParameters.put(m, p);
            }
            return p;
        }

        public String toString() {
            return "  pl:" + Utils.round(pl) +
                    "  N:" + Utils.round(N);
        }
    }


    public static class IDebugSynapse {

    }


    public static class XlModeParameters {

        public double lRel;
        public double pOut;
        public double pl;
        public double actDelta;
        public double IGDelta;
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
            return o != null ? o.p : 1.0;
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
