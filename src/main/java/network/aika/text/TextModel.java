package network.aika.text;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

public class TextModel extends Model {

    private InhibitoryNeuron prevTokenInhib;
    private InhibitoryNeuron nextTokenInhib;


    @Override
    public void linkInputRelations(PatternPartSynapse s, Activation originAct) {

    }

    private Neuron[] initInput(Model m, InhibitoryNeuron prevWordInhib, InhibitoryNeuron nextWordInhib, String label) {
        PatternNeuron in = new PatternNeuron(m, label, true);
        PatternPartNeuron inRelPW = new PatternPartNeuron(m, label + "Rel Prev. Word", true);
        PatternPartNeuron inRelNW = new PatternPartNeuron(m, label + "Rel Next Word", true);

        {
            {
                PatternPartSynapse s = new PatternPartSynapse(in, inRelPW);
                s.setPropagate(true);

                s.link();
                s.update(10.0);
            }

            {
                PatternPartSynapse s = new PatternPartSynapse(nextWordInhib, inRelPW);

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
                PatternPartSynapse s = new PatternPartSynapse(prevWordInhib, inRelNW);

                s.link();
                s.update(10.0);
            }
            inRelNW.setBias(4.0);
            inRelNW.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelPW, prevWordInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }

        {
            InhibitorySynapse s = new InhibitorySynapse(inRelNW, nextWordInhib);

            s.link();
            s.update(1.0);
            s.commit();
        }

        return new Neuron[] {in, inRelPW, inRelNW};
    }


    public InhibitoryNeuron getPrevTokenInhib() {
        return prevTokenInhib;
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return nextTokenInhib;
    }

    public PatternNeuron lookupTokenNeuron(String tokenLabel) {
        return null;
    }
}
