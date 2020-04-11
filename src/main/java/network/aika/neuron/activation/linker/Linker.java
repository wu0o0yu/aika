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
package network.aika.neuron.activation.linker;

import network.aika.Document;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;

import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;
import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;


/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    public static LTargetLink inputLink;
    public static LTargetLink sameInputLink;
    public static LTargetLink relatedInputLink;

    static {
        // Pattern
        {
            LNode target = new LNode(PatternType.CURRENT, PatternNeuron.type, "target");
            LNode inputA = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "inputA");
            LNode inputB = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "inputB");

            inputLink = new LTargetLink(inputB, target, SAME_PATTERN, "inputLink");
            LLink l1 = new LMatchingLink(inputA, inputB, SAME_PATTERN, "l1");
            LLink l2 = new LMatchingLink(inputA, target, SAME_PATTERN, "l2");

            target.setLinks(inputLink, l2);
            inputA.setLinks(l1, l2);
            inputB.setLinks(l1, inputLink);
        }

        // Same Input
        {
            LNode target = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "target");
            LNode inputPattern = new LNode(PatternType.INPUT, PatternNeuron.type, "inputPattern");
            LNode inputRel = new LNode(PatternType.INPUT, PatternPartNeuron.type, "inputRel");
            LNode inhib = new LNode(PatternType.INPUT, InhibitoryNeuron.type, "inhib");

            sameInputLink = new LTargetLink(inputRel, target, INPUT_PATTERN, "sameInputLink");
            LLink l1 = new LMatchingLink(inputPattern, inputRel, SAME_PATTERN, "l1");
            LLink l2 = new LMatchingLink(inhib, inputRel, SAME_PATTERN, "l2");
            LLink inhibLink = new LMatchingLink(inputPattern, inhib, SAME_PATTERN, "inhibLink");
            LLink inputPatternLink = new LMatchingLink(inputPattern, target, INPUT_PATTERN, "inputPatternLink");

            target.setLinks(sameInputLink, inputPatternLink);
            inputPattern.setLinks(l1, inhibLink, inputPatternLink);
            inputRel.setLinks(sameInputLink, l1, l2);
            inhib.setLinks(inhibLink, l2);
        }

        // Related Input
        {
            LNode target = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "target");
            LNode samePatternPP = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "samePatternPP");
            LNode inputRel = new LNode(PatternType.INPUT, PatternPartNeuron.type, "inputRel");
            LNode relPattern = new LNode(PatternType.RELATED, PatternNeuron.type, "relPattern");
            LNode inhib = new LNode(PatternType.INPUT, InhibitoryNeuron.type, "inhib");

            relatedInputLink = new LTargetLink(samePatternPP, target, SAME_PATTERN, "relatedInputLink");
            LLink inputRelLink = new LMatchingLink(inputRel, target, INPUT_PATTERN, "inputRelLink");
            LLink relPatternLink1 = new LMatchingLink(relPattern, inputRel, INPUT_PATTERN, "relPatternLink1");
            LLink relPatternLink2 = new LMatchingLink(relPattern, samePatternPP, INPUT_PATTERN, "relPatternLink2");
            LLink inhibLink = new LMatchingLink(relPattern, inhib, SAME_PATTERN, "inhibLink");
            LLink relPatternLink3 = new LMatchingLink(inhib, inputRel, INPUT_PATTERN, "relPatternLink3");

            target.setLinks(relatedInputLink, inputRelLink);
            samePatternPP.setLinks(relatedInputLink, relPatternLink2);
            inputRel.setLinks(inputRelLink, relPatternLink1, relPatternLink3);
            relPattern.setLinks(relPatternLink1, relPatternLink2, inhibLink);
            inhib.setLinks(inhibLink, relPatternLink3);
        }
    }

    public interface CollectResults {
        void collect(Activation act, Synapse s);
    }

    public void linkForward(Activation act, boolean processMode) {
        ArrayDeque<Entry> queue = new ArrayDeque<>();
        Document doc = act.getDocument();
        TreeSet<Neuron> propagationTargets = new TreeSet(act.getINeuron().getPropagationTargets());

        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .forEach(l -> {
                        new Entry(l.getOutput())
                                .addCandidate(l.getSynapse(), act)
                                .addToQueue(queue);
                        propagationTargets.remove(l.getOutput().getNeuron());
                    });
            act.lastRound.unlink();
            act.lastRound = null;
        }

        act.getINeuron().collectLinkingCandidates(act, INPUT, (cAct, s) -> {
            new Entry(cAct)
                    .addCandidate(s, act)
                    .addToQueue(queue);
            propagationTargets.remove(cAct.getNeuron());
        });

        propagationTargets
                .stream()
                .map(n -> n.get().getProvider())
                .map(n -> act.getNeuron().getOutputSynapse(n))
                .forEach(s ->
                        new Entry(new Activation(doc, s.getOutput(), false, null, 0))
                                .addCandidate(s, act)
                                .addToQueue(queue)
                );

        process(queue, processMode);
    }

    private static class Entry {
        Activation act;
        NavigableSet<Link> candidates = new TreeSet<>(Comparator.
                <Link, Fired>comparing(l -> l.getInput().getFired())
                .thenComparing(l -> l.getSynapse().getInput())
        );

        private Entry() {
        }

        public Entry(Activation act) {
            this.act = act;
       }

        public Entry addCandidate(Synapse s, Activation input) {
            candidates.add(new Link(s, input, null));
            return this;
        }

        public Entry cloneEntry(boolean branch) {
            Entry ce = new Entry();
            ce.act = act.cloneAct(branch);
            candidates.forEach(l -> ce.addCandidate(l.getSynapse(), l.getInput()));
            return ce;
        }

        public void addToQueue(ArrayDeque<Entry> queue) {
            if (!candidates.isEmpty()) {
                queue.addLast(this);
            }
        }

        public String toString() {
            return "act:" + act;
        }
    }

    private void process(ArrayDeque<Entry> queue, boolean processMode) {
        while (!queue.isEmpty()) {
            Entry e = queue.pollFirst();
            Link l = e.candidates.pollFirst();

            if(e.act.isFinal && !l.isSelfRef()) {
                e.addToQueue(queue);
                e = e.cloneEntry(e.act.isInitialRound() && l.isConflict());
            }

            e.act.addLink(l, processMode);
            findLinkingCandidates(e, l);
            e.addToQueue(queue);
        }
    }

    public void findLinkingCandidates(Entry e, Link l) {
 //       if(((e.act.getINeuron() instanceof InhibitoryNeuron) || l.isConflict())) return;

        l.getInput().getINeuron().collectLinkingCandidates(l.getInput(), OUTPUT, (act, s) -> {
            Synapse is = e.act.getNeuron().getInputSynapse(act.getNeuron());
            if (is == null || l.getSynapse() == is) return;

            if (act.getOutputLinks(is).noneMatch(la -> la.getOutput() == e.act)) {
                e.addCandidate(is, act);
            }
        });
    }
}
