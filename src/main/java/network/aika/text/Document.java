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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Range;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.PatternNeuron;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static network.aika.steps.Phase.PROCESSING;


/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought<TextModel> {

    private final StringBuilder content;

    private NavigableMap<Integer, BindingSignal> tokenPosIndex = new TreeMap<>();

    public Document(TextModel model, String content) {
        super(model);
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    @Override
    public void registerBindingSignalSource(Activation act, BindingSignal bs) {
        if(bs.getOriginActivation() instanceof TokenActivation) {
            TokenActivation tokenAct = (TokenActivation) bs.getOriginActivation();
            tokenPosIndex.put(tokenAct.getPosition(), bs);
        }
    }

    public Stream<TokenActivation> getRelatedTokens(BindingSignal fromBindingSignal, Integer distance, Neuron toNeuron) {
        if(!(fromBindingSignal.getOriginActivation() instanceof TokenActivation))
            return Stream.empty();

        TokenActivation tAct = (TokenActivation) fromBindingSignal.getOriginActivation();
        BindingSignal relBS = tokenPosIndex.get(tAct.getPosition() + distance);
        if(relBS == null)
            return Stream.empty();

        return Stream.of((TokenActivation) relBS.getOriginActivation());
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

    public String getTextSegment(Range range) {
        if(range == null)
            return "";

        Range r = range.limit(new Range(0, length()));
        return content.substring((int) r.getBegin(), (int) r.getEnd());
    }

    public static String getText(Activation<?> act) {
        return ((Document)act.getThought()).getTextSegment(act.getRange());
    }

    public TokenActivation addToken(String token, int pos, int begin, int end) {
        return addToken(model.lookupToken(token), pos, begin, end);
    }

    public TokenActivation addToken(PatternNeuron n, int pos, int begin, int end) {
        TokenActivation act = new TokenActivation(createActivationId(), pos, begin, end, this, n);

        act.init(null, null);

        return act;
    }

    public void processTokens(Iterable<String> tokens) {
        int i = 0;
        int pos = 0;

        List<TokenActivation> tokenActs = new ArrayList<>();
        for(String t: tokens) {
            int j = i + t.length();
            tokenActs.add(addToken(t, pos++, i, j));

            i = j + 1;
        }

        for(TokenActivation tAct: tokenActs) {
            tAct.setNet(10.0);
            process(PROCESSING);
        }

        updateModel();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(content);
        sb.append("\n");
        sb.append(super.toString());
        return sb.toString();
    }
}
