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
package network.aika.neuron.phase.activation;

import network.aika.Config;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.link.LinkPhase;

/**
 *
 * @author Lukas Molzberger
 */
public interface ActivationPhase extends Phase<Activation> {
    ActivationPhase INITIAL_LINKING = new Linking();
    ActivationPhase PREPARE_FINAL_LINKING = new PrepareFinalLinking();
    ActivationPhase FINAL_LINKING = new FinalLinking();
    ActivationPhase SOFTMAX = new Softmax();
    ActivationPhase COUNTING = new Counting();
    ActivationPhase SELF_GRADIENT = new SelfGradient();
    ActivationPhase GRADIENTS = new PropagateGradients();
    ActivationPhase UPDATE_SYNAPSE_INPUT_LINKS = new UpdateSynapseInputLinks();
    ActivationPhase TEMPLATE = new Template();
    ActivationPhase INDUCTION = new Induction();
    ActivationPhase FINAL = new Final();

    void process(Activation act);

    boolean isFinal();

    void tryToLink(Activation act, Visitor v);

    void propagate(Activation act, Visitor v);

    ActivationPhase[] getNextActivationPhases(Config c);

    LinkPhase[] getNextLinkPhases(Config c);

    static boolean isFinal(ActivationPhase ap) {
        return ap != null && ap.isFinal();
    }

    static ActivationPhase[] getInitialPhases(Config c) {
        return c.isEnableTraining() ?
                new ActivationPhase[]{
                        PREPARE_FINAL_LINKING,
                        SOFTMAX,
                        COUNTING,
                        SELF_GRADIENT,
                        GRADIENTS,
                        UPDATE_SYNAPSE_INPUT_LINKS,
                        TEMPLATE,
                        FINAL
                } :
                new ActivationPhase[] {
                        PREPARE_FINAL_LINKING,
                        SOFTMAX,
                        COUNTING,
                        FINAL
                };
    }
}
