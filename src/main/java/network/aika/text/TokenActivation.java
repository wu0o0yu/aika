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
package network.aika.text;

import network.aika.Thought;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.excitatory.PatternNeuron;


/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private int rangeBegin;
    private int rangeEnd;
    private TokenActivation previousToken;
    private TokenActivation nextToken;


    public TokenActivation(int id, int begin, int end, Document doc, PatternNeuron patternNeuron) {
        super(id, doc, patternNeuron);
        rangeBegin = begin;
        rangeEnd = end;
    }

    public static void addRelation(TokenActivation prev, TokenActivation next) {
        if(prev == null || next == null)
            return;

        prev.nextToken = next;
        next.previousToken = prev;
    }

    public TokenActivation getPreviousToken() {
        return previousToken;
    }

    public TokenActivation getNextToken() {
        return nextToken;
    }
}
