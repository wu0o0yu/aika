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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Range;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.PatternBindingSignal;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.steps.activation.Propagate;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;


/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought<TextModel> {

    private final StringBuilder content;

    private NavigableMap<Long, PatternBindingSignal> beginPBSIndex = new TreeMap<>();
    private NavigableMap<Long, PatternBindingSignal> endPBSIndex = new TreeMap<>();

    public Document(TextModel model, String content) {
        super(model);
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    public void registerPatternBindingSignalSource(Activation act, PatternBindingSignal pbs) {
        Range r = pbs.getOriginActivation().getRange();

        beginPBSIndex.put(r.getBegin(), pbs);
        endPBSIndex.put(r.getEnd(), pbs);
    }

    public Stream<PatternBindingSignal> getLooselyRelatedBindingSignals(BindingSignal<?> fromBindingSignal, Integer looseLinkingRange) {
        Range r = fromBindingSignal.getOriginActivation().getRange();

        return Stream.concat(
            beginPBSIndex.subMap(r.getEnd(), r.getEnd() + looseLinkingRange).values().stream(),
            endPBSIndex.subMap(r.getBegin() - looseLinkingRange, r.getBegin()).values().stream()
        )
                .map(bs -> bs.getOriginActivation())
                .flatMap(originAct -> originAct.getReverseBindingSignals());
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

    public TokenActivation addToken(String token, int begin, int end) {
        return addToken(model.lookupToken(token), begin, end);
    }

    public TokenActivation addToken(NeuronProvider n, int begin, int end) {
        return addToken((PatternNeuron) n.getNeuron(), begin, end);
    }

    public TokenActivation addToken(PatternNeuron n, int begin, int end) {
        TokenActivation act = new TokenActivation(createActivationId(), begin, end, this, n);

        act.setInputValue(1.0);
        act.init(null, null);
        act.setFired();

        return act;
    }

    public void processTokens(Iterable<String> tokens) {
        int i = 0;
        TokenActivation lastToken = null;
        for(String t: tokens) {
            int j = i + t.length();
            TokenActivation currentToken = addToken(t, i, j);
            process();

            TokenActivation.addRelation(lastToken, currentToken);
            process();

            lastToken = currentToken;
            i = j + 1;
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
