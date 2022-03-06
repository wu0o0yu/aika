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
package network.aika.neuron.disjunctive;

import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.sign.Sign;
import network.aika.utils.Bound;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends DisjunctiveSynapse<InhibitorySynapse, BindingNeuron, InhibitoryNeuron, InhibitoryLink, BindingActivation, InhibitoryActivation> {

    @Override
    public InhibitoryLink createLink(BindingActivation input, InhibitoryActivation output) {
        return new InhibitoryLink(this, input, output);
    }

    @Override
    public boolean checkTemplateLinkingPreConditions(BindingActivation iAct, InhibitoryActivation oAct) {
        if(iAct.getNeuron().isNetworkInput())
            return false;

        return super.checkTemplateLinkingPreConditions(iAct, oAct);
    }

    @Override
    public void setModified() {
        getInput().setModified();
    }
}
