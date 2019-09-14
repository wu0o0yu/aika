package network.aika.neuron.relation;

import java.util.NavigableSet;

public interface RelationEndpoint {


    NavigableSet<Relation.Key> getRelations();


    NavigableSet<Relation.Key> getOutputRelationsTmp();



}
