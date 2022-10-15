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
package network.aika.neuron.conjunctive;

import network.aika.neuron.activation.*;
import network.aika.neuron.disjunctive.InhibitoryNeuron;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.mul;


/**
 *
 * @author Lukas Molzberger
 */
public class NegativeFeedbackSynapse extends FeedbackSynapse<
        NegativeFeedbackSynapse,
        InhibitoryNeuron,
        NegativeFeedbackLink,
        InhibitoryActivation
        >
{
    public NegativeFeedbackSynapse() {
        super(Scope.INPUT);
    }

    @Override
    public NegativeFeedbackLink createLink(InhibitoryActivation input, BindingActivation output) {
        return new NegativeFeedbackLink(this, input, output);
    }

    protected boolean getDummyLinkUB() {
        return false;
    }

    @Override
    public boolean propagateCheck(InhibitoryActivation iAct) {
        return true;
    }
/*
    @Override
    public boolean linkCheck(InhibitoryActivation iBS, BindingActivation oBS) {
        return iBS.isSelfRef(oBS);
    }
*/

    @Override
    public void setWeight(double w) {
        weight.receiveUpdate(w);
    }

    @Override
    protected boolean checkCausal(InhibitoryActivation iAct, BindingActivation oAct) {
        return true;
    }

    @Override
    public boolean isPropagate() {
        return false;
    }
}
