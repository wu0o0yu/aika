package network.aika.training.relation;

import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.relation.Direction;
import network.aika.neuron.relation.Relation;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeightedRelation extends Relation {

    public int fromSynapseId;
    public int toSynapseId;


    public Relation keyRelation;
    public RelationStatistic statistic;


    public WeightedRelation(Relation rel, double weight) {
        this.keyRelation = rel;
        this.statistic = new RelationStatistic(weight);
    }


    public WeightedRelation(Relation rel, RelationStatistic statistic) {
        this.keyRelation = rel;
        this.statistic = statistic;
    }


    public WeightedRelation(Relation rel, RelationStatistic statistic, int fromSynapseId, int toSynapseId) {
        this.keyRelation = rel;
        this.statistic = statistic;
        this.fromSynapseId = fromSynapseId;
        this.toSynapseId = toSynapseId;
    }


    public Relation getKeyRelation() {
        return keyRelation;
    }


    public void setFromSynapseId(int fromSynapseId) {
        this.fromSynapseId = fromSynapseId;
    }


    public void setToSynapseId(int toSynapseId) {
        this.toSynapseId = toSynapseId;
    }


    @Override
    public int getType() {
        return 0;
    }

    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined, Direction dir) {
        return keyRelation.test(act, linkedAct, allowUndefined, dir);
    }


    public Stream<Activation> getActivations(INeuron n, Activation linkedAct, Direction dir) {
        return keyRelation.getActivations(n, linkedAct, dir);
    }


    public boolean isExact() {
        return keyRelation.isExact();
    }


    public double computeWeight(Link l) {
/*        if(dir) {
            return 0;
        }
*/
        Activation oAct = l.getOutput();

        Synapse s = oAct.getSynapseById(fromSynapseId);
        for(Link relLink: oAct.getLinksBySynapse(network.aika.neuron.activation.link.Direction.INPUT, s).collect(Collectors.toList())) {

        }
        return 0;
    }


    public void mapSlots(Map<Integer, Position> slots, Activation act, Direction dir) {
        keyRelation.mapSlots(slots, act, dir);
    }


    public WeightedRelation createTargetRelation(Activation act, Activation relatedAct) {
        // TODO

        return new WeightedRelation(keyRelation, statistic.copy());
    }

    public WeightedRelation copy() {
        return new WeightedRelation(keyRelation, statistic.copy());
    }


    public String toString() {
        return "WEIGHTED-REL(" + keyRelation + ":" + statistic + ")";
    }

    @Override
    public int compareTo(Relation rel, Direction dir) {
        int r = super.compareTo(rel, dir);
        if(r != 0) return r;

        WeightedRelation wr = (WeightedRelation) rel;

        return keyRelation.compareTo(wr.keyRelation, dir);
    }
}
