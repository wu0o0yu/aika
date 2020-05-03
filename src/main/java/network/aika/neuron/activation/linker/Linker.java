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

import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;

import static network.aika.neuron.PatternScope.*;
import static network.aika.neuron.activation.linker.PatternType.CURRENT;
import static network.aika.neuron.activation.linker.PatternType.INPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    Deque<Link> queue = new ArrayDeque<>();

    public static LTargetLink patternInputLinkT;
    public static LMatchingLink patternInputLinkI;

    public static LTargetLink sameInputLinkT;
    public static LMatchingLink sameInputLinkI;

    public static LTargetLink relatedInputLinkT;
    public static LMatchingLink relatedInputLinkI;

    public static LTargetLink inhibitoryLinkT;
    public static LMatchingLink inhibitoryLinkI;

    public static LTargetLink posRecLinkT;
    public static LMatchingLink posRecLinkI;

    static {
        // Pattern
        {
            LNode target = new LMatchingNode(CURRENT, PatternNeuron.type, "target");
            LNode inputA = new LMatchingNode(CURRENT, PatternPartNeuron.type, "inputA");
            LNode inputB = new LMatchingNode(CURRENT, PatternPartNeuron.type, "inputB");

            patternInputLinkT = new LTargetLink(inputB, target, SAME_PATTERN, "inputLink", false, false, null);
            patternInputLinkI = new LMatchingLink(inputA, target, SAME_PATTERN, "l2",true);
            new LMatchingLink(inputA, inputB, SAME_PATTERN, "l1", false);
        }

        // Same Input
        {
            LNode target = new LMatchingNode(CURRENT, PatternPartNeuron.type, "target");
            LNode inputPattern = new LMatchingNode(INPUT, PatternNeuron.type, "inputPattern");
            LNode inputRel = new LMatchingNode(INPUT, PatternPartNeuron.type, "inputRel");
            LNode inhib = new LMatchingNode(INPUT, InhibitoryNeuron.type, "inhib");

            sameInputLinkT = new LTargetLink(inputRel, target, INPUT_PATTERN, "sameInputLink", false, false, null);
            sameInputLinkI = new LMatchingLink(inputPattern, target, INPUT_PATTERN, "inputPatternLink", true);
            new LMatchingLink(inputPattern, inputRel, SAME_PATTERN, "l1", false);
            new LMatchingLink(inhib, inputRel, SAME_PATTERN, "l2", false);
            new LMatchingLink(inputPattern, inhib, SAME_PATTERN, "inhibLink", false);
        }

        // Related Input
        {
            LNode target = new LMatchingNode(CURRENT, PatternPartNeuron.type, "target");
            LNode samePatternPP = new LMatchingNode(CURRENT, PatternPartNeuron.type, "samePatternPP");
            LNode inputRel = new LMatchingNode(INPUT, PatternPartNeuron.type, "inputRel");
            LNode relPattern = new LMatchingNode(PatternType.RELATED, PatternNeuron.type, "relPattern");
            LNode inhib = new LMatchingNode(INPUT, InhibitoryNeuron.type, "inhib");

            relatedInputLinkT = new LTargetLink(samePatternPP, target, SAME_PATTERN, "relatedInputLink", false, false, null);
            relatedInputLinkI = new LMatchingLink(inputRel, target, INPUT_PATTERN, "inputRelLink", false);
            new LMatchingLink(relPattern, inputRel, INPUT_PATTERN, "relPatternLink1", false);
            new LMatchingLink(relPattern, samePatternPP, INPUT_PATTERN, "relPatternLink2", false);
            new LMatchingLink(relPattern, inhib, SAME_PATTERN, "inhibLink", false);
            new LMatchingLink(inhib, inputRel, INPUT_PATTERN, "relPatternLink3", true);
        }

        // Inhibitory
        {
            LNode target = new LMatchingNode(CURRENT, PatternPartNeuron.type, "target");
            LNode inhib = new LMatchingNode(null, InhibitoryNeuron.type, "inhib");
            LNode patternpart = new LMatchingNode(CURRENT, PatternPartNeuron.type, "patternpart");
            LNode input = new LMatchingNode(INPUT, PatternNeuron.type, "input");

            inhibitoryLinkT = new LTargetLink(inhib, target, CONFLICTING_PATTERN, "inhibLink", true, true, null);
            inhibitoryLinkI = new LMatchingLink(input, target, INPUT_PATTERN, "l1", true);
            new LMatchingLink(input, patternpart, INPUT_PATTERN, "l2", false);
            new LMatchingLink(patternpart, inhib, SAME_PATTERN, "l3", false);
        }

        // Positive Recurrent Pattern Link
        {
            LNode target = new LMatchingNode(CURRENT, PatternPartNeuron.type, "target");
            LNode pattern = new LMatchingNode(CURRENT, PatternNeuron.type, "pattern");

            posRecLinkT = new LTargetLink(pattern, target, SAME_PATTERN, "posRecLink", true, false, null);
            posRecLinkI = new LMatchingLink(target, pattern, SAME_PATTERN, "patternLink", true);
        }
    }


    public void linkForward(Activation act) {
        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .forEach(l ->
                            queue.add(Link.link(l.getSynapse(), act, l.getOutput()))
                    );
            act.lastRound.unlink();
            act.lastRound = null;
        }

        act.getINeuron().linkForwards(act);

        process();
    }

    public void process() {
        while (!queue.isEmpty()) {
            Link l = queue.pollFirst();
            Activation act = l.getOutput();
            INeuron n = act.getINeuron();

            if(act.isFinal && !l.isSelfRef()) {
                if(l.isConflict()) {
                    act = act.createBranch();
                } else {
                    act = act.createUpdate();
                }
            }

            act.addLink(l);

            n.linkBackwards(l);
        }
    }
}
