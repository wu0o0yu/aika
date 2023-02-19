package network.aika.elements.links;

import network.aika.elements.activations.*;
import network.aika.elements.synapses.CategoryInputSynapse;
import network.aika.elements.synapses.CategorySynapse;

public class CategoryInputLink extends DisjunctiveLink<CategoryInputSynapse, CategoryActivation, Activation>  {


    @Override
    public void instantiateTemplate(CategoryActivation iAct, Activation oAct) {
        if(iAct == null || oAct == null)
            return;

        Link l = iAct.getInputLink(oAct.getNeuron());

        if(l != null)
            return;

        CategorySynapse s = new CategorySynapse();
        s.initFromTemplate(oAct.getNeuron(), iAct.getNeuron(), synapse);

        s.createLinkFromTemplate(oAct, iAct, this);
    }

    @Override
    public void addInputLinkingStep() {
        super.addInputLinkingStep();

        input.getInputLinks()
                .map(l -> l.getInput())
                .forEach(act ->
                        output.linkTemplateAndInstance(act)
                );
    }

/*

PatternCategoryInputLink:

    @Override
    protected void connectGradientFields() {
        initGradient();

        super.connectGradientFields();
    }

    @Override
    public void patternVisit(Visitor v) {
    }
 */

    /*
    BindingCategoryInputLink:

     */
}
