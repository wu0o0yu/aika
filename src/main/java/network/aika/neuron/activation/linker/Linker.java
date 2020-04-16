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
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;

import static network.aika.neuron.OutputKey.OUTPUT_COMP;
import static network.aika.neuron.PatternScope.INPUT_PATTERN;
import static network.aika.neuron.PatternScope.SAME_PATTERN;

/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    public interface CollectResults {
        void collect(Activation act, Synapse s);
    }

    public static LTargetLink patternInputLinkT;
    public static LMatchingLink patternInputLinkI;

    public static LTargetLink sameInputLinkT;
    public static LMatchingLink sameInputLinkI;

    public static LTargetLink relatedInputLinkT;
    public static LMatchingLink relatedInputLinkI;

    static {
        // Pattern
        {
            LNode target = new LNode(PatternType.CURRENT, PatternNeuron.type, "target");
            LNode inputA = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "inputA");
            LNode inputB = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "inputB");

            patternInputLinkT = new LTargetLink(inputB, target, SAME_PATTERN, "inputLink");
            patternInputLinkI = new LMatchingLink(inputA, target, SAME_PATTERN, "l2",true);
            LLink l1 = new LMatchingLink(inputA, inputB, SAME_PATTERN, "l1", false);

            target.setLinks(patternInputLinkT, patternInputLinkI);
            inputA.setLinks(l1, patternInputLinkI);
            inputB.setLinks(l1, patternInputLinkT);
        }

        // Same Input
        {
            LNode target = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "target");
            LNode inputPattern = new LNode(PatternType.INPUT, PatternNeuron.type, "inputPattern");
            LNode inputRel = new LNode(PatternType.INPUT, PatternPartNeuron.type, "inputRel");
            LNode inhib = new LNode(PatternType.INPUT, InhibitoryNeuron.type, "inhib");

            sameInputLinkT = new LTargetLink(inputRel, target, INPUT_PATTERN, "sameInputLink");
            sameInputLinkI = new LMatchingLink(inputPattern, target, INPUT_PATTERN, "inputPatternLink", true);
            LLink l1 = new LMatchingLink(inputPattern, inputRel, SAME_PATTERN, "l1", false);
            LLink l2 = new LMatchingLink(inhib, inputRel, SAME_PATTERN, "l2", false);
            LLink inhibLink = new LMatchingLink(inputPattern, inhib, SAME_PATTERN, "inhibLink", false);

            target.setLinks(sameInputLinkT, sameInputLinkI);
            inputPattern.setLinks(l1, inhibLink, sameInputLinkI);
            inputRel.setLinks(sameInputLinkT, l1, l2);
            inhib.setLinks(inhibLink, l2);
        }

        // Related Input
        {
            LNode target = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "target");
            LNode samePatternPP = new LNode(PatternType.CURRENT, PatternPartNeuron.type, "samePatternPP");
            LNode inputRel = new LNode(PatternType.INPUT, PatternPartNeuron.type, "inputRel");
            LNode relPattern = new LNode(PatternType.RELATED, PatternNeuron.type, "relPattern");
            LNode inhib = new LNode(PatternType.INPUT, InhibitoryNeuron.type, "inhib");

            relatedInputLinkT = new LTargetLink(samePatternPP, target, SAME_PATTERN, "relatedInputLink");
            relatedInputLinkI = new LMatchingLink(inputRel, target, INPUT_PATTERN, "inputRelLink", false);
            LLink relPatternLink1 = new LMatchingLink(relPattern, inputRel, INPUT_PATTERN, "relPatternLink1", false);
            LLink relPatternLink2 = new LMatchingLink(relPattern, samePatternPP, INPUT_PATTERN, "relPatternLink2", false);
            LLink inhibLink = new LMatchingLink(relPattern, inhib, SAME_PATTERN, "inhibLink", false);
            LLink relPatternLink3 = new LMatchingLink(inhib, inputRel, INPUT_PATTERN, "relPatternLink3", true);

            target.setLinks(relatedInputLinkT, relatedInputLinkI);
            samePatternPP.setLinks(relatedInputLinkT, relPatternLink2);
            inputRel.setLinks(relatedInputLinkI, relPatternLink1, relPatternLink3);
            relPattern.setLinks(relPatternLink1, relPatternLink2, inhibLink);
            inhib.setLinks(inhibLink, relPatternLink3);
        }
    }

    public void linkForward(Activation act, boolean processMode) {
        Deque<Link> queue = new ArrayDeque<>();
        Document doc = act.getDocument();
        TreeSet<Synapse> propagationTargets = new TreeSet(OUTPUT_COMP);
        propagationTargets.addAll(act.getINeuron().getPropagationTargets());

        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .forEach(l -> {
                        addLinkToQueue(queue, l.getSynapse(), act, l.getOutput());
                        propagationTargets.remove(l.getSynapse());
                    });
            act.lastRound.unlink();
            act.lastRound = null;
        }

        act.getINeuron().collectLinkingCandidatesForwards(act, (cAct, s) -> {
            addLinkToQueue(queue, s, act, cAct);
            propagationTargets.remove(s);
        });

        propagationTargets
                .forEach(s ->
                        addLinkToQueue(queue, s, act, new Activation(doc, s.getOutput(), false, null, 0))
                );

        process(queue, processMode);
    }

    private void process(Deque<Link> queue, boolean processMode) {
        while (!queue.isEmpty()) {
            Link l = queue.pollFirst();
            Activation act = l.getOutput();
            INeuron n = act.getINeuron();

            if(act.isFinal && !l.isSelfRef()) {
                act = act.cloneAct(act.isInitialRound() && l.isConflict());
            }

            act.addLink(l, processMode);

            n.collectLinkingCandidatesBackwards(l,
                    (cAct, s) -> addLinkToQueue(queue, s, cAct, null)
            );
        }
    }

    private static void addLinkToQueue(Deque<Link> queue, Synapse s, Activation iAct, Activation oAct) {
        queue.add(new Link(s, iAct, oAct));
    }
}
