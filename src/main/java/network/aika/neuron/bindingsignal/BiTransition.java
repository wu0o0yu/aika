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
package network.aika.neuron.bindingsignal;


import network.aika.direction.Direction;
import network.aika.neuron.Synapse;


/**
 * @author Lukas Molzberger
 */
public class BiTransition extends Transition {

    private BiTransition relatedTransition;


    protected BiTransition(State input, State output) {
        super(input, output);
    }

    public static BiTransition biTransition(State input, State output) {
        return new BiTransition(input, output);
    }

    public BiTransitionListener createListener(Synapse ts, BindingSignal bs, Direction dir) {
        return new BiTransitionListener(this, bs, dir, ts);
    }

    public static void link(BiTransition t1, BiTransition t2) {
        t1.relatedTransition = t2;
        t2.relatedTransition = t1;
    }

    public Transition getRelatedTransition() {
        return relatedTransition;
    }
}
