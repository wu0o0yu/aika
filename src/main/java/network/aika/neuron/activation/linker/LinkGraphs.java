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

import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import static network.aika.neuron.PatternScope.*;

/**
 *
 * @author Lukas Molzberger
 */
public class LinkGraphs {

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

    public static LTargetLink propagateT;

    public static LTargetLink inducePatternPart;

    static {
        // Pattern
        {
            LNode target = new LMatchingNode(PatternNeuron.class, "CURRENT-target");
            LNode inputA = new LMatchingNode(PatternPartNeuron.class, "CURRENT-inputA");
            LNode inputB = new LMatchingNode(PatternPartNeuron.class, "CURRENT-inputB");

            patternInputLinkT = new LTargetLink(inputB, target, SAME_PATTERN, null, "inputLink", false, false, null);
            patternInputLinkI = new LMatchingLink(inputA, target, SAME_PATTERN, null, "l2",true);
            new LMatchingLink(inputA, inputB, SAME_PATTERN, null, "l1", false);
        }

        // Same Input
        {
            LNode target = new LMatchingNode(PatternPartNeuron.class, "CURRENT-target");
            LNode inputPattern = new LMatchingNode(PatternNeuron.class, "INPUT-inputPattern");
            LNode inputRel = new LMatchingNode(PatternPartNeuron.class, "INPUT-inputRel");
            LNode inhib = new LMatchingNode(InhibitoryNeuron.class, "INPUT-inhib");

            sameInputLinkT = new LTargetLink(inputRel, target, INPUT_PATTERN, null, "sameInputLink", false, false, null);
            sameInputLinkI = new LMatchingLink(inputPattern, target, INPUT_PATTERN, null, "inputPatternLink", true);
            new LMatchingLink(inputPattern, inputRel, SAME_PATTERN, null, "l1", false);
            new LMatchingLink(inhib, inputRel, SAME_PATTERN, null, "l2", false);
            new LMatchingLink(inputPattern, inhib, SAME_PATTERN, null, "inhibLink", false);
        }

        // Related Input
        {
            LNode target = new LMatchingNode(PatternPartNeuron.class, "CURRENT-target");
            LNode samePatternPP = new LMatchingNode(PatternPartNeuron.class, "CURRENT-samePatternPP");
            LNode inputRel = new LMatchingNode(PatternPartNeuron.class, "INPUT-inputRel");
            LNode relPattern = new LMatchingNode(PatternNeuron.class, "RELATED-relPattern");
            LNode inhib = new LMatchingNode(InhibitoryNeuron.class, "INPUT-inhib");

            relatedInputLinkT = new LTargetLink(samePatternPP, target, SAME_PATTERN, null, "relatedInputLink", false, false, null);
            relatedInputLinkI = new LMatchingLink(inputRel, target, INPUT_PATTERN, null, "inputRelLink", false);
            new LMatchingLink(relPattern, inputRel, INPUT_PATTERN, null, "relPatternLink1", false);
            new LMatchingLink(relPattern, samePatternPP, INPUT_PATTERN, null, "relPatternLink2", false);
            new LMatchingLink(relPattern, inhib, SAME_PATTERN, null, "inhibLink", false);
            new LMatchingLink(inhib, inputRel, INPUT_PATTERN, null, "relPatternLink3", true);
        }

        // Inhibitory
        {
            LNode target = new LMatchingNode(PatternPartNeuron.class, "CURRENT-target");
            LNode inhib = new LMatchingNode(InhibitoryNeuron.class, "inhib");
            LNode patternpart = new LMatchingNode(PatternPartNeuron.class, "CURRENT-patternpart");
            LNode input = new LMatchingNode(PatternNeuron.class, "INPUT-input");

            inhibitoryLinkT = new LTargetLink(inhib, target, CONFLICTING_PATTERN, null, "inhibLink", true, true, null);
            inhibitoryLinkI = new LMatchingLink(input, target, INPUT_PATTERN, null, "l1", true);
            new LMatchingLink(input, patternpart, INPUT_PATTERN, null, "l2", false);
            new LMatchingLink(patternpart, inhib, SAME_PATTERN, null, "l3", false);
        }

        // Positive Recurrent Pattern Link
        {
            LNode target = new LMatchingNode(PatternPartNeuron.class, "CURRENT-target");
            LNode pattern = new LMatchingNode(PatternNeuron.class, "CURRENT-pattern");

            posRecLinkT = new LTargetLink(pattern, target, SAME_PATTERN, null, "posRecLink", true, false, null);
            posRecLinkI = new LMatchingLink(target, pattern, SAME_PATTERN, null, "patternLink", true);
        }

        // Propagate
        {
            LNode target = new LTargetNode(null, "target");
            LNode input = new LMatchingNode(null, "input");

            propagateT = new LTargetLink(input, target, null, null, "propagateLink", null, false, true);
        }

        // Induce Pattern Part
        {
            LNode target = new LTargetNode(PatternPartNeuron.class, "CURRENT-target");
            LNode input = new LMatchingNode(PatternNeuron.class, "INPUT-input");

            inducePatternPart = new LTargetLink(input, target, INPUT_PATTERN, PatternPartSynapse.class, "inducePatternPart", false, false, true);
        }
    }
}
