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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Range;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.linking.Visitor;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    protected Range range;

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    public Range getRange() {
        return range;
    }

    public void trackBindingSignal(Visitor v, Predicate<Activation> p) {
        if(visited == v.getV())
            return;

        super.followBindingSignal(v, p);
        super.followBindingSignal(v.next(Direction.OUTPUT), p);

        p.test(this);

        visited = v.getV();
    }
}
