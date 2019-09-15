package network.aika.neuron.relation;

import java.util.Collection;

public interface RelationEndpoint {

    void addRelation(Integer synId, Relation rel, Direction dir);

    void removeRelation(Integer synId, Relation rel, Direction dir);

    Collection<Relation.Key> getRelationById(Integer id);

    Relation.Key getRelation(Relation.Key rk);

    Collection<Relation.Key> getRelations();

    Collection<Relation.Key> getOutputRelationsTmp();
}
