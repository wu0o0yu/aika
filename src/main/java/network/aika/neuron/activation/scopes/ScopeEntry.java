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
package network.aika.neuron.activation.scopes;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Lukas Molzberger
 */
public class ScopeEntry implements Comparable<ScopeEntry> {

    public static final Comparator<ScopeEntry> COMPARE = Comparator.
            <ScopeEntry>comparingInt(se -> se.sourceId)
            .thenComparing(se -> se.scope.getClass().getSimpleName());

    private int sourceId;
    private Scope scope;

    public ScopeEntry(int sourceId, Scope scope) {
        this.sourceId = sourceId;
        this.scope = scope;
    }

    public int getSourceId() {
        return sourceId;
    }

    public Scope getScope() {
        return scope;
    }

    public ScopeEntry next(Scope s) {
        return new ScopeEntry(sourceId, s);
    }

    public Set<ScopeEntry> nextSet(Scope... scopes) {
        Set<ScopeEntry> results = new TreeSet<>();
        for(Scope s: scopes) {
            results.add(next(s));
        }
        return results;
    }

    @Override
    public int compareTo(ScopeEntry se) {
        return COMPARE.compare(this, se);
    }

    public String toString() {
        return sourceId + "-" + scope;
    }
}
