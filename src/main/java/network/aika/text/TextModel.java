package network.aika.text;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

public class TextModel extends Model {

    public InhibitoryNeuron prevTokenInhib;
    public InhibitoryNeuron nextTokenInhib;


    @Override
    public void linkInputRelations(PatternPartSynapse s, Activation originAct) {
        if(s.getInput() != nextTokenInhib) {
            return;
        }

        Document doc = (Document) originAct.getThought();
        Cursor c = doc.getCursor();

        if(c.previousNextTokenAct != null) {
            Link l = new Link(s, c.previousNextTokenAct, originAct);
            doc.add(l);
        }
    }

    public void initToken(String label) {
        PatternNeuron in = new PatternNeuron(this, label, true);
        PatternPartNeuron inRelPW = new PatternPartNeuron(this, label + "Rel Prev. Word", true);
        PatternPartNeuron inRelNW = new PatternPartNeuron(this, label + "Rel Next Word", true);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelPW);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(nextTokenInhib, inRelPW);

                s.link();
                s.update(10.0);
            }
            inRelPW.setBias(4.0);
            inRelPW.commit();
        }
        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelNW);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(prevTokenInhib, inRelNW);

                s.link();
                s.update(10.0);
            }
            inRelNW.setBias(4.0);
            inRelNW.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, prevTokenInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, nextTokenInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }
    }

    public InhibitoryNeuron getPrevTokenInhib() {
        return prevTokenInhib;
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return nextTokenInhib;
    }
}
