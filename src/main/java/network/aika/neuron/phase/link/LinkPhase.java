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
package network.aika.neuron.phase.link;

import network.aika.Config;
import network.aika.neuron.activation.Link;
import network.aika.neuron.phase.Phase;

/**
 *
 * @author Lukas Molzberger
 */
public interface LinkPhase extends Phase<Link> {

    int INDUCTION_RANK = 1;
    int LINKING_RANK = 2;
    int COUNTING_RANK = 8;
    int SHADOW_FACTOR_RANK = 9;
    int SELF_GRADIENT_RANK = 11;
    int PROPAGATE_GRADIENT_RANK = 12;
    int UPDATE_WEIGHTS_RANK = 14;
    int TEMPLATE_RANK = 16;

/*
    static LinkPhase[] getInitialPhases(Config c) {
        return c.isEnableTraining() ?
                new LinkPhase[] {
                        LINKING,
                        COUNTING,
                        SHADOW_FACTOR,
                        SELF_GRADIENT,
                        UPDATE_WEIGHTS,
                        TEMPLATE
                } :
                new LinkPhase[] {
                        COUNTING
                };
    }
 */
}
