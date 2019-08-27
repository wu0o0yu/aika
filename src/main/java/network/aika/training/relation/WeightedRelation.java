package network.aika.training.relation;

import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.relation.Relation;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WeightedRelation extends Relation {

    public int fromSynapseId;
    public int toSynapseId;


    public Relation rel;
    public RelationStatistic statistic;


    public WeightedRelation(Relation rel, double weight) {
        this.rel = rel;
        this.statistic = new RelationStatistic(weight);
    }


    public WeightedRelation(Relation rel, RelationStatistic statistic) {
        this.rel = rel;
        this.statistic = statistic;
    }


    public WeightedRelation(Relation rel, RelationStatistic statistic, int fromSynapseId, int toSynapseId) {
        this.rel = rel;
        this.statistic = statistic;
        this.fromSynapseId = fromSynapseId;
        this.toSynapseId = toSynapseId;
    }


    public Relation getKeyRelation() {
        return rel;
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

    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined) {
        return rel.test(act, linkedAct, allowUndefined);
    }

    @Override
    public Relation invert() {
        return new WeightedRelation(rel.invert(), statistic, toSynapseId, fromSynapseId);
    }


    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        return rel.getActivations(n, linkedAct);
    }


    public boolean isExact() {
        return rel.isExact();
    }


    public double computeWeight(Link l) {
/*        if(dir) {
            return 0;
        }
*/
        Activation oAct = l.getOutput();

        Synapse s = oAct.getSynapseById(fromSynapseId);
        for(Link relLink: oAct.getLinksBySynapse(Direction.INPUT, s).collect(Collectors.toList())) {

        }
        return 0;
    }


    public void mapSlots(Map<Integer, Position> slots, Activation act) {
        rel.mapSlots(slots, act);
    }


    public WeightedRelation createTargetRelation(Activation act, Activation relatedAct) {
        // TODO

        return new WeightedRelation(rel, statistic.copy());
    }

    public WeightedRelation copy() {
        return new WeightedRelation(rel, statistic.copy());
    }
}
