# aika

Aika is a Java library that automatically extracts and annotates semantic information in text. 
In case this information is ambiguous, Aika will generate several hypothetical interpretations 
about the meaning of this text and retrieve the most likely one. The Aika algorithm is based 
on various ideas and approaches from the field of AI such as artificial neural networks, 
frequent pattern mining and logic based expert systems and can be applied to a broad spectrum 
of text analysis tasks. It combines these concepts in a single very compact algorithm. 

A good starting point to get familiar with this project are probably the following three test cases:
- SimplePatternMatchingTest (Demonstrates how a pattern like a word or a phrase can be matched)
- MutualExclusionTest (Demonstrates a negative feed back loop)
- NamedEntityRecognitionTest (A more complex example: recognize the name jackson cook)
