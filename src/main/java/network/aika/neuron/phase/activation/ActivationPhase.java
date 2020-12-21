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
import network.aika.neuron.phase.Phase;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public interface ActivationPhase extends Phase<Activation> {
    ActivationPhase INDUCTION = new Induction(0);
    Linking INITIAL_LINKING = new Linking(3);
    ActivationPhase PREPARE_FINAL_LINKING = new PrepareFinalLinking(4);
    ActivationPhase FINAL_LINKING = new FinalLinking(5);
    ActivationPhase SOFTMAX = new Softmax(6);
    ActivationPhase COUNTING = new Counting(7);
    ActivationPhase SELF_GRADIENT = new SelfGradient(10);
    ActivationPhase PROPAGATE_GRADIENT = new PropagateGradients(13);
    ActivationPhase UPDATE_SYNAPSE_INPUT_LINKS = new UpdateSynapseInputLinks(15);
    Template TEMPLATE_INPUT = new Template(17, INPUT);
    Template TEMPLATE_OUTPUT = new Template(18, OUTPUT);
    ActivationPhase FINAL = new Final(19);

    void process(Activation act);

    boolean isFinal();

    static boolean isFinal(ActivationPhase ap) {
        return ap != null && ap.isFinal();
    }

    static ActivationPhase[] getInitialPhases(Config c) {
        return c.isEnableTraining() ?
                new ActivationPhase[]{
                        INITIAL_LINKING,
                        PREPARE_FINAL_LINKING,
                        SOFTMAX,
                        COUNTING,
                        SELF_GRADIENT,
                        PROPAGATE_GRADIENT,
                        UPDATE_SYNAPSE_INPUT_LINKS,
                        TEMPLATE_INPUT,
                        TEMPLATE_OUTPUT,
                        FINAL
                } :
                new ActivationPhase[] {
                        INITIAL_LINKING,
                        PREPARE_FINAL_LINKING,
                        SOFTMAX,
                        COUNTING,
                        FINAL
                };
    }
}
