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

import network.aika.Model;
import network.aika.Thought;
import network.aika.elements.activations.Timestamp;
import network.aika.elements.neurons.Range;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.neurons.TokenNeuron;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.elements.activations.Timestamp.MIN;
import static network.aika.elements.activations.Timestamp.NOT_SET;


/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    private NavigableMap<PositionKey, TokenActivation> tokenPosIndex = new TreeMap<>(Comparator
            .<PositionKey>comparingInt(pk -> pk.getTokenPosition())
            .thenComparingInt(pk -> pk.getActId())
    );

    private NavigableMap<RangeKey, TokenActivation> rangeIndex = new TreeMap<>(Comparator
            .<RangeKey>comparingLong(rk -> rk.getRange().getBegin())
            .thenComparingLong(rk -> -rk.getRange().getEnd())
            .thenComparingInt(rk -> rk.getActId())
    );


    public Document(Model model, String content) {
        super(model);
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    public void registerTokenActivation(TokenActivation tokenAct) {
        if(tokenAct.getTokenPos() != null)
            tokenPosIndex.put(new PositionKey(tokenAct), tokenAct);

        if(tokenAct.getRange() != null)
            rangeIndex.put(new RangeKey(tokenAct), tokenAct);
    }

    public Stream<TokenActivation> getRelatedTokensByTokenPosition(TokenActivation fromTokenAct, int relFrom, int relTo) {
        return tokenPosIndex.subMap(
                new PositionKey(fromTokenAct.getTokenPos() + relFrom, Integer.MIN_VALUE),
                new PositionKey(fromTokenAct.getTokenPos() + relTo, Integer.MAX_VALUE)
        ).values().stream();
    }

    public Stream<TokenActivation> getRelatedTokensByCharPosition(Range fromRange, Range toRange) {
        return rangeIndex.subMap(
                new RangeKey(fromRange, Integer.MIN_VALUE),
                new RangeKey(toRange, Integer.MAX_VALUE)
        ).values().stream();
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

    public TokenActivation addToken(TokenNeuron n, Integer pos, int begin, int end) {
        return new TokenActivation(createActivationId(), pos, begin, end, this, n);
    }

    @Override
    public Timestamp getCreated() {
        return MIN;
    }

    @Override
    public Timestamp getFired() {
        return NOT_SET;
    }

    @Override
    public Thought getThought() {
        return this;
    }

    public String docToString() {
        StringBuilder sb = new StringBuilder(content);
        sb.append("\n");
        sb.append(super.activationsToString());
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" Content: ");
        sb.append(
                content.substring(0, Math.min(content.length(), 100))
                        .replaceAll("[\\n\\r\\s]+", " ")
        );
        return sb.toString();
    }
}
