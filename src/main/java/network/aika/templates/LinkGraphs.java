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
package network.aika.templates;

import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.*;

import static network.aika.neuron.PatternScope.*;

/**
 *
 * @author Lukas Molzberger
 */
public class LinkGraphs {

    public static LLink patternInputLinkT;
    public static LLink patternInputLinkI;

    public static LLink sameInputLinkT;
    public static LLink sameInputLinkI;

    public static LLink relatedInputLinkT;
    public static LLink relatedInputLinkI;

    public static LLink inhibitoryLinkT;
    public static LLink inhibitoryLinkI;

    public static LLink posRecLinkT;
    public static LLink posRecLinkI;

    public static LLink propagateT;

    public static LLink inducePatternPart;

    public static LLink inducePPInhibitoryNeuron;

    public static LLink inducePPInhibInputSynapse;

    public static LLink induceNegativePPInputSynapse;

    static {
        // Pattern
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("CURRENT-target");

            LNode inputA = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-inputA");

            LNode inputB = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-inputB");


            patternInputLinkT = new LTargetLink()
                    .setRecurrent(false)
                    .setNegative(false)
                    .setInput(inputB)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("inputLink");

            patternInputLinkI = new LMatchingLink()
                    .setInput(inputA)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("l2")
                    .setDirection(true);


            new LMatchingLink()
                    .setInput(inputA)
                    .setOutput(inputB)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("l1")
                    .setDirection(false);
        }

        // Same Input
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode inputPattern = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("INPUT-inputPattern");

            LNode inputRel = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("INPUT-inputRel");

            LNode inhib = new LMatchingNode()
                    .setNeuronClass(InhibitoryNeuron.class)
                    .setLabel("INPUT-inhib");


            sameInputLinkT = new LTargetLink()
                    .setRecurrent(false)
                    .setNegative(false)
                    .setInput(inputRel)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("sameInputLink");

            sameInputLinkI = new LMatchingLink()
                    .setInput(inputPattern)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("inputPatternLink")
                    .setDirection(true);

            new LMatchingLink()
                    .setInput(inputPattern)
                    .setOutput(inputRel)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("l1")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(inhib)
                    .setOutput(inputRel)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("l2")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(inputPattern)
                    .setOutput(inhib)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("inhibLink")
                    .setDirection(false);

        }

        // Related Input
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode samePatternPP = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-samePatternPP");

            LNode inputRel = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("INPUT-inputRel");

            LNode relPattern = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("RELATED-relPattern");

            LNode inhib = new LMatchingNode()
                    .setNeuronClass(InhibitoryNeuron.class)
                    .setLabel("INPUT-inhib");

            relatedInputLinkT = new LTargetLink()
                    .setRecurrent(false)
                    .setNegative(false)
                    .setInput(samePatternPP)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("relatedInputLink");

            relatedInputLinkI = new LMatchingLink()
                    .setInput(inputRel)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("inputRelLink")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(relPattern)
                    .setOutput(inputRel)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("relPatternLink1")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(relPattern)
                    .setOutput(samePatternPP)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("relPatternLink2")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(relPattern)
                    .setOutput(inhib)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("inhibLink")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(inhib)
                    .setOutput(inputRel)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("relPatternLink3")
                    .setDirection(true);
        }

        // Inhibitory
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode inhib = new LMatchingNode()
                    .setNeuronClass(InhibitoryNeuron.class)
                    .setLabel("inhib");

            LNode patternpart = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-patternpart");

            LNode input = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("INPUT-input");


            inhibitoryLinkT = new LTargetLink()
                    .setRecurrent(true)
                    .setNegative(true)
                    .setInput(inhib)
                    .setOutput(target)
                    .setPatternScope(CONFLICTING_PATTERN)
                    .setLabel("inhibLink");

            inhibitoryLinkI = new LMatchingLink()
                    .setInput(input)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("l1")
                    .setDirection(true);

            new LMatchingLink()
                    .setInput(input)
                    .setOutput(patternpart)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("l2")
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(patternpart)
                    .setOutput(inhib)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("l3")
                    .setDirection(false);
        }

        // Positive Recurrent Pattern Link
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode pattern = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("CURRENT-pattern");


            posRecLinkT = new LTargetLink()
                    .setRecurrent(true)
                    .setNegative(false)
                    .setInput(pattern)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("posRecLink");

            posRecLinkI = new LMatchingLink()
                    .setInput(target)
                    .setOutput(pattern)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("patternLink")
                    .setDirection(true);

        }

        // Propagate
        {
            LNode target = new LTargetNode()
                    .setLabel("target");

            LNode input = new LMatchingNode()
                    .setLabel("input");


            propagateT = new LTargetLink()
                    .setPropagate(true)
                    .setNegative(false)
                    .setInput(input)
                    .setOutput(target)
                    .setLabel("propagateLink");
        }

        // Induce Pattern Part
        {
            LNode target = new LTargetNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode input = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("INPUT-input");


            inducePatternPart = new LTargetLink()
                    .setPropagate(true)
                    .setNegative(false)
                    .setRecurrent(false)
                    .setInput(input)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("inducePatternPart")
                    .setSynapseClass(PatternPartSynapse.class);
        }

        // Induce Inhibitory Neuron
        {
            LNode target = new LTargetNode()
                    .setNeuronClass(PatternPartInhibitoryNeuron.class)
                    .setLabel("CURRENT-target");

            LNode input = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("INPUT-input");


            inducePPInhibitoryNeuron = new LTargetLink()
                    .setPropagate(true)
                    .setNegative(false)
                    .setRecurrent(false)
                    .setInitialWeight(1.0)
                    .setInput(input)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("induceInhibitoryNeuron")
                    .setSynapseClass(PrimaryInhibitorySynapse.class);

        }

        // Induce Input PP-Inhibitory Synapse
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartInhibitoryNeuron.class)
                    .setLabel("CURRENT-target");

            LNode input = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-input");

            LNode inputPattern = new LMatchingNode()
                    .setNeuronClass(PatternNeuron.class)
                    .setLabel("INPUT-input-pattern");


            inducePPInhibInputSynapse = new LTargetLink()
                    .setPropagate(true)
                    .setNegative(false)
                    .setRecurrent(false)
                    .setInput(input)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("induceInhibitorySynapse")
                    .setSynapseClass(InhibitorySynapse.class);

            new LMatchingLink()
                    .setInput(inputPattern)
                    .setOutput(input)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("pp-input-syn")
                    .setSynapseClass(PatternPartSynapse.class)
                    .setDirection(false);

            new LMatchingLink()
                    .setInput(inputPattern)
                    .setOutput(target)
                    .setPatternScope(INPUT_PATTERN)
                    .setLabel("primary-inhib-syn")
                    .setSynapseClass(PrimaryInhibitorySynapse.class)
                    .setDirection(true);
        }

        // Induce negative PP-Input Synapse
        {
            LNode target = new LMatchingNode()
                    .setNeuronClass(PatternPartNeuron.class)
                    .setLabel("CURRENT-target");

            LNode input = new LMatchingNode()
                    .setNeuronClass(PatternPartInhibitoryNeuron.class)
                    .setLabel("CURRENT-input");


            induceNegativePPInputSynapse = new LTargetLink()
                    .setNegative(true)
                    .setRecurrent(true)
                    .setInput(input)
                    .setOutput(target)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("induce pp-input-syn")
                    .setSynapseClass(PatternPartSynapse.class);

            new LMatchingLink()
                    .setInput(target)
                    .setOutput(input)
                    .setPatternScope(SAME_PATTERN)
                    .setLabel("pp-inhib-syn")
                    .setSynapseClass(InhibitorySynapse.class)
                    .setDirection(false);
        }
    }
}
