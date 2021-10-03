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
package network.aika.neuron.excitatory;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.activation.direction.Direction;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternSynapse extends ExcitatorySynapse<BindingNeuron, PatternNeuron, PatternActivation> {

    @Override
    public void updateReference(Link l) {
        Reference or = l.getOutput().getReference();
        Reference ir = l.getInput().getReference();

        l.getOutput().propagateReference(
                or == null ?
                        ir :
                        or.add(ir)
        );
    }

    @Override
    public boolean checkTemplatePropagate(Activation act) {
        return true;
    }

    @Override
    public boolean checkCausality(Activation<?> iAct, Activation<?> oAct) {
        return true;
    }
}
