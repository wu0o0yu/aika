package network.aika.elements.links;

import network.aika.elements.activations.*;
import network.aika.elements.synapses.CategoryInputSynapse;
import network.aika.elements.synapses.CategorySynapse;

public abstract class CategoryInputLink extends DisjunctiveLink<CategoryInputSynapse, CategoryActivation, Activation>  {

    public CategoryInputLink(CategoryInputSynapse s, CategoryActivation input, Activation output) {
        super(s, input, output);
    }

    @Override
    public void instantiateTemplate(CategoryActivation iAct, Activation oAct) {
        if(iAct == null || oAct == null)
            return;

        Link l = iAct.getInputLink(oAct.getNeuron());

        if(l != null)
            return;

        CategorySynapse s = createCategorySynapse();
        s.initFromTemplate(oAct.getNeuron(), iAct.getNeuron(), synapse);

        s.createLinkFromTemplate(oAct, iAct, this);
    }

    protected abstract CategorySynapse createCategorySynapse();
}
