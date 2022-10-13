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

import network.aika.direction.Direction;
import network.aika.fields.*;
import network.aika.neuron.activation.*;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.mul;

/**
 *
 * @author Lukas Molzberger
 */
public class PositiveFeedbackSynapse extends FeedbackSynapse<
        PositiveFeedbackSynapse,
        PatternNeuron,
        PositiveFeedbackLink,
        PatternActivation
        >
{
    public PositiveFeedbackLink createLink(PatternActivation input, BindingActivation output) {
        return new PositiveFeedbackLink(this, input, output);
    }

    public void initDummyLink(BindingActivation oAct) {
        Multiplication dummyWeight = mul(
                oAct,
                 "pos-dummy-weight-" + getInput().getId(),
                 oAct.getIsOpen(),
                 getWeight()
         );

        LinkSlot ls = oAct.lookupLinkSlot(this, true);
        connect(dummyWeight, -1, ls);
    }

    @Override
    public boolean propagateCheck(PatternActivation iAct) {
        return false;
    }

    @Override
    public FieldOutput getLinkingEvent(Activation act, Direction dir) {
        return null;
    }

    @Override
    protected boolean checkCausal(PatternActivation iAct, BindingActivation oAct) {
        return true;
    }

    @Override
    public boolean isPropagate() {
        return false;
    }
}
