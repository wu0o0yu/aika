package network.aika.neuron;

import network.aika.neuron.activation.scopes.Scope;

import java.util.Collections;
import java.util.Set;

public class TemplateNeuronInfo {

    Set<Scope> inputScopes = Collections.emptySet();
    Set<Scope> outputScopes = Collections.emptySet();

    private String label;
    private Set<Neuron<?>> templateGroup;

    private double xCoord;
    private double yCoord;


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getXCoord() {
        return xCoord;
    }

    public void setXCoord(double xCoord) {
        this.xCoord = xCoord;
    }

    public double getYCoord() {
        return yCoord;
    }

    public void setYCoord(double yCoord) {
        this.yCoord = yCoord;
    }

    public Set<Neuron<?>> getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(Set<Neuron<?>> templateGroup) {
        this.templateGroup = templateGroup;
    }

    public Set<Scope> getInputScopes() {
        return inputScopes;
    }

    public Set<Scope> getOutputScopes() {
        return outputScopes;
    }
}
