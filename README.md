# About the AIKA Neural Network
AIKA (**A**rtificial **I**ntelligence for **K**nowledge **A**cquisition) is a new type of artificial neural network designed to more closely mimic the behavior of a biological brain and to bridge the gap to classical AI. A key design decision in the AIKA network is to conceptually separate the activations from their neurons, meaning that there are two separate graphs. One graph consisting of neurons and synapses representing the knowledge the network has already acquired and another graph consisting of activations and links describing the information the network was able to infer about a concrete input data set. There is a one-to-many relation between the neurons and the activations. For example, there might be a neuron representing a word or a specific meaning of a word, but there might be several activations of this neuron, each representing an occurrence of this word within the input data set. A consequence of this decision is that we must give up on the idea of a fixed layered topology for the network, since the sequence in which the activations are fired depends on the input data set. Within the activation network, each activation is grounded within the input data set, even if there are several activations in between. This means links between activations server multiple purposes:
- They propagate the activation value.
- They propagate the binding-signal, that is used for the linking process.
- They establish an approximate causal relation through the fired timestamps of their input and output activations.
- They allow the training gradient to be propagated backwards.
- Negative feedback links create mutually exclusive branches within the activations network.
- Positive feedback links allow the binding neurons of a pattern neuron ensemble to support each other, by feeding the activation value of the patten neuron back to its input binding-neurons.

The AIKA network uses four different types of neurons:
- Pattern-Neurons (PN)
- Binding-Neurons (BN)
- Inhibitory-Neurons (IN)
- Category-Neurons (CN)

The Pattern-Neurons and the Binding-Neurons are both conjunctive in nature while the Inhibitory-Neurons and the Category-Neurons are disjunctive. The Binding-Neurons are kind of the glue code of the whole network. On the one hand, they bind the input-features of a pattern to the pattern-neuron and on the other hand receive negative feedback synapses from the inhibitory neurons which allow them to either be suppressed by an opposing pattern or allow themselves suppress another conflicting pattern. Similar to the neuron types there are also several different types of synapses, depending on wich types of neurons they connect. For example, the input synapses of an inhibitory neuron are always linked to Binding-Neurons, while the input synapses of Category-Neurons are always linked to pattern-neurons.

The following types of synapses exist within the AIKA network:

- PrimaryInputBNSynapse ((PN|CN) -> BN)
- RelatedInputBNSynapse (BN -> BN)
- SamePatternBNSynapse (BN -> BN)
- PositiveFeedbackSynapse (PN -> BN)
- NegativeFeedbackSynapse (IN -> BN)
- PatternSynapse (BN -> PN)
- CategorySynapse (PN -> PN)
- InhibitorySynapse (BN -> IN)

Depending on their source activation two types of binding-signals can be distinguished: The pattern-binding-signal and the branch-binding-signal. The pattern-binding-signal originates at a pattern-activation and is used to bind the input-features of a pattern to the pattern itself. The branch-binding-signal originates at a binding-activation and is used to distinguish the mutually exclusive branches from each other. The pattern-binding-signal is also carrying a scope that allows it to distinguish between different pattern 
binding ensembles. The scope of the binding-signal changes when beeing propagated through certain types of synapses such as the PrimaryInputBNSynapse of the RelatedInputBNSynapse. The scope is used during the linking process to verify the validity of creating a certain new link. 

As already mentioned the Binding-Neurons of a pattern neuron ensamble are used to bind this pattern to its input features. To verify that all the input-features occured in the correct relation to each other the SamePatternBNSynapse is used. The SamePatternBNSynapse connects two Binding-Neurons within the same pattern neuron ensamble. Therfore, the SamePatternBNSynapse is used to avoid what is called the superposition catastrophe.

Initially, the network starts out empty and is then gradually populated during training. The induction of new neurons and synapses is guided by a network of template neurons and synapses.
