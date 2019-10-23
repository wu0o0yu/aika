package network.aika.neuron.relation;

import network.aika.neuron.activation.Activation;

import java.util.Collection;

public interface RelationEndpoint {

    Integer getRelationEndpointId();

    void addRelation(RelationEndpoint relEndpoint, Relation rel, Direction dir);

    void removeRelation(RelationEndpoint relEndpoint, Relation rel, Direction dir);

    Collection<Relation.Key> getRelationById(Integer id);

    Relation.Key getRelation(Relation.Key rk);

    Collection<Relation.Key> getRelations();

    Collection<Relation.Key> getOutputRelations();

    Collection<Activation> getActivations(Activation outputAct);
}
