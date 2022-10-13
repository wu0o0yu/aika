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
import network.aika.neuron.activation.*;

/**
 *
 * @author Lukas Molzberger
 */
public class RelatedInputSynapse extends BindingNeuronSynapse<
        RelatedInputSynapse,
        LatentRelationNeuron,
        RelatedInputLink,
        BindingActivation
        >
{
    @Override
    public RelatedInputLink createLink(BindingActivation input, BindingActivation output) {
        return new RelatedInputLink(this, input, output);
    }

    @Override
    public void linkAndPropagateIn(Activation fromBS) {
 // OP-3
 //     latentBackwardsPropagation(fromBS);
        super.linkAndPropagateIn(fromBS);
    }

    /*
    private void latentBackwardsPropagation(Activation fromBS) {
        Activation relatedInputBS = fromBSs[0];
        Activation relatedSameBS = fromBSs[1];

        LatentRelationActivation latentRelAct = getInput().createOrLookupLatentActivation(
                relatedInputBS.getOriginActivation(),
                INPUT_TRANSITION.getInput().getState(),
                relatedSameBS.getOriginActivation(),
                SAME_TRANSITION.getInput().getState()
        );

        Link l = createLink(latentRelAct, (BindingActivation) relatedSameBS);

        INPUT_TRANSITION.propagate(INPUT_TRANSITION.getOutput(), relatedInputBS, l, latentRelAct);
        SAME_TRANSITION.propagate(SAME_TRANSITION.getOutput(), relatedSameBS, l, latentRelAct);
    }*/
}
