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
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.BindingSignal;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.steps.activation.AddInputActivation;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    public Document(String content) {
        super();
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    public void registerActivation(Activation act) {
        super.registerActivation(act);
        TextModel model = (TextModel) act.getModel();

        Neuron n = act.getNeuron();
        if(n == model.getPrevTokenInhib()) {
            addRelationBindingSignal(act, INPUT);
        } else if(n == model.getNextTokenInhib()) {
            addRelationBindingSignal(act, OUTPUT);
        }
    }

    private void addRelationBindingSignal(Activation<?> act, Direction dir) {
        BindingSignal currentBS = act.getPatternBindingSignals().values().stream()
                .filter(bs -> bs.getScope() == 0)
                .findFirst()
                .orElse(null);

        TokenActivation currentToken = (TokenActivation) currentBS.getBindingSignalAct();
        TokenActivation relToken = dir == INPUT ?
                currentToken.getPreviousToken() :
                currentToken.getNextToken();

        act.addBindingSignal(
                new BindingSignal(
                        null,
                        relToken,
                        act,
                        (byte)1,
                        (byte)1
                )
        );
    }

    public void append(String txt) {
        content.append(txt);
    }

    public char charAt(int i) {
        return content.charAt(i);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    public String getTextSegment(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }

    public String getText(Activation<?> act) {
        return getTextSegment(act.getStatBegin(), act.getStatEnd());
    }

    public PatternActivation addToken(TextModel m, String token, int begin, int end) {
        return addToken(m.lookupToken(token), begin, end);
    }

    public PatternActivation addToken(PatternNeuron n, int begin, int end) {
        TokenActivation act = new TokenActivation(createActivationId(), begin, end, this, n);

        act.setInputValue(1.0);

        AddInputActivation.add(act);
        return act;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(content);
        sb.append("\n");
        sb.append(super.toString());
        return sb.toString();
    }
}
