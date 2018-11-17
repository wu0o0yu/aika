package network.aika.neuron.relation;

import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.util.*;
import java.util.stream.Stream;


public abstract class AncestorRelation extends Relation {

    public static AncestorRelation COMMON_ANCESTOR = new CommonAncestor();
    public static AncestorRelation IS_DESCENDANT_OF = new IsDescendantOf();
    public static AncestorRelation IS_ANCESTOR_OF = new IsAncestorOf();
    public static AncestorRelation NOT_DESCENDANT_OF = new NotDescendantOf();
    public static AncestorRelation NOT_ANCESTOR_OF = new NotAncestorOf();


    AncestorRelation() {}


    @Override
    public void registerRequiredSlots(Neuron input) {
    }


    private static void collectCommonAncestor(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;

        collectContains(results, n, linkedAct, v);

        linkedAct.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> collectCommonAncestor(results, n, l.input, v));
    }


    private static void collectContains(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        linkedAct.getOutputLinks(false)
                .filter(l ->l.synapse.identity)
                .forEach(l -> collectContains(results, n, l.output, v));
    }


    private static void collectContainedIn(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        linkedAct.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> collectContainedIn(results, n, l.input, v));
    }


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act) {
    }


    @Override
    public void linksOutputs(Set<Integer> results) {
    }


    private static boolean contains(Activation actA, Activation actB, long v) {
        if(actA.visited == v) return false;
        actA.visited = v;

        if(actA == actB) return true;

        return actA.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .anyMatch(l -> contains(l.input, actB, v));
    }


    private static boolean hasCommonAncestor(Activation act, Activation linkedAct) {
        long v = act.doc.visitedCounter++;
        markAncestors(linkedAct, v);
        return hasCommonAncestor(act, v, act.doc.visitedCounter++);
    }


    private static void markAncestors(Activation act, long v) {
        if(act.visited == v) return;
        act.visited = v;

        act.markedAncDesc = v;

        act.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> markAncestors(l.input, v));
    }


    private static void markDescendants(Activation act, long v) {
        if(act.visited == v) return;
        act.visited = v;

        act.markedAncDesc = v;

        act.getOutputLinks(false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> markDescendants(l.input, v));
    }


    private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
        if(act.visited == v2) return false;
        act.visited = v2;

        if(act.markedAncDesc == v1) return true;

        return act.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .anyMatch(l -> hasCommonAncestor(l.input, v1, v2));
    }


    @Override
    public boolean isExact() {
        return false;
    }


    public static class CommonAncestor extends AncestorRelation {
        public static int TYPE = 50;

        static {
            registerRelation(TYPE, () -> COMMON_ANCESTOR);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return COMMON_ANCESTOR;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return hasCommonAncestor(act, linkedAct);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            List<Activation> results = new ArrayList<>();
            collectCommonAncestor(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            return results.stream();
        }
    }


    public static class IsDescendantOf extends AncestorRelation {
        public static int TYPE = 51;

        static {
            registerRelation(TYPE, () -> IS_DESCENDANT_OF);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return IS_ANCESTOR_OF;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return contains(act, linkedAct, act.doc.visitedCounter++);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            List<Activation> results = new ArrayList<>();
            collectContains(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            return results.stream();
        }
    }


    public static class IsAncestorOf extends AncestorRelation {
        public static int TYPE = 52;

        static {
            registerRelation(TYPE, () -> IS_ANCESTOR_OF);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return IS_DESCENDANT_OF;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return contains(linkedAct, act, act.doc.visitedCounter++);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            List<Activation> results = new ArrayList<>();
            collectContainedIn(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            return results.stream();
        }
    }


    public static class NotDescendantOf extends AncestorRelation {
        public static int TYPE = 53;

        static {
            registerRelation(TYPE, () -> NOT_DESCENDANT_OF);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return NOT_ANCESTOR_OF;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return !contains(act, linkedAct, act.doc.visitedCounter++);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            long v = linkedAct.doc.visitedCounter++;
            markDescendants(linkedAct, v);

            INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);
            return th.getActivations()
                    .filter(act -> act.markedAncDesc != v);
        }
    }


    public static class NotAncestorOf extends AncestorRelation {
        public static int TYPE = 54;

        static {
            registerRelation(TYPE, () -> NOT_ANCESTOR_OF);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return NOT_DESCENDANT_OF;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return !contains(linkedAct, act, act.doc.visitedCounter++);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            long v = linkedAct.doc.visitedCounter++;
            markAncestors(linkedAct, v);

            INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);
            return th.getActivations()
                    .filter(act -> act.markedAncDesc != v);
        }
    }
}
