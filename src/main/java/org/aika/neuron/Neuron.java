package org.aika.neuron;


import org.aika.Model;
import org.aika.corpus.Range;
import org.aika.lattice.Node;
import org.aika.lattice.OrNode;

import java.util.Set;

public class Neuron extends AbstractNeuron<Neuron> {

    public Neuron() {
    }

    public Neuron(String label) {
        super(label);
    }

    public Neuron(String label, boolean isBlocked, boolean noTraining) {
        super(label, isBlocked, noTraining);
    }

    public static Neuron init(Model m, int threadId, Neuron np, double bias, double negDirSum, double negRecSum, double posRecSum, Set<Synapse> inputs) {
        Neuron n = np;
        n.m = m;
        n.m.stat.neurons++;
        n.bias = bias;
        n.negDirSum = negDirSum;
        n.negRecSum = negRecSum;
        n.posRecSum = posRecSum;

        n.lock.acquireWriteLock(threadId);
        OrNode node = new OrNode(m);
        n.node = node.provider;
        node.neuron = n.provider;

        n.lock.releaseWriteLock();

        double sum = 0.0;
        for(Synapse s: inputs) {
            assert !s.key.startRangeOutput || s.key.startRangeMatch == Range.Operator.EQUALS || s.key.startRangeMatch == Range.Operator.FIRST;
            assert !s.key.endRangeOutput || s.key.endRangeMatch == Range.Operator.EQUALS || s.key.endRangeMatch == Range.Operator.FIRST;

            s.output = n.provider;
            s.link(threadId);

            if(s.maxLowerWeightsSum == Double.MAX_VALUE) {
                s.maxLowerWeightsSum = sum;
            }

            sum += s.w;
        }

        if(!Node.adjust(m, threadId, n, -1)) return null;

        n.publish(threadId);

        return np;
    }
}
