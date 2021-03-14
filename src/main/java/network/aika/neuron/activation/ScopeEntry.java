package network.aika.neuron.activation;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.Scope.PP_INPUT;

public class ScopeEntry implements Comparable<ScopeEntry> {

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
        int r = Integer.compare(sourceId, se.sourceId);
        if(r != 0) return r;
        return scope.compareTo(se.scope);
    }
}
