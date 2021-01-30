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

import network.aika.neuron.activation.Activation;
import network.aika.neuron.phase.Phase;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.phase.link.LinkPhase.*;

/**
 *
 * @author Lukas Molzberger
 */
public interface ActivationPhase extends Phase<Activation> {
    ActivationPhase INDUCTION = new Induction();
    LinkAndPropagate LINK_AND_PROPAGATE = new LinkAndPropagate();
    ActivationPhase PREPARE_POSITIVE_FEEDBACK_LOOP_UPDATE = new PreparePositiveFeedbackLoopUpdate();
    ActivationPhase POSITIVE_FEEDBACK_LOOP_UPDATE = new PositiveFeedbackLoopUpdate();
    ActivationPhase SOFTMAX = new DetermineBranchProbability();
    ActivationPhase COUNTING = new Counting();
    ActivationPhase ENTROPY_GRADIENT = new EntropyGradient();
    ActivationPhase PROPAGATE_GRADIENTS = new PropagateGradients();
    ActivationPhase UPDATE_SYNAPSE_INPUT_LINKS = new UpdateSynapseInputLinks();
    Template TEMPLATE_INPUT = new Template(TEMPLATE, INPUT);
    Template TEMPLATE_OUTPUT = new Template(TEMPLATE_INPUT, OUTPUT);

    void process(Activation act);

}
