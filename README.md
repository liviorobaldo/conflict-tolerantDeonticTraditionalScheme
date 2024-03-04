<p align="justify">
This is the GitHub repository associated with the paper "Compliance checking in the Semantic Web: an RDF-based conflict-tolerant version of the Deontic Traditional Scheme", by Livio Robaldo and Gianluca Pozzato.
</p>

<p align="justify">
This repository contains the computational ontology proposed in the paper as well as all examples shown and discussed therein. The ontology includes RDF resources that implement a conflict-tolerant version of the well-known Deontic Traditional Scheme (see the paper for details). Some of these resources specify SPARQL rules in the form CONSTRUCT-WHERE that enable the inferences in the Deontic Traditional Scheme.
</p>

<p align="justify">
To re-execute the examples locally you need Java installed. The source code downloadable from this GitHub repository has been developed using Java v.19 but it should work also with other versions of the Java Runtime Environment.
</p>

<p align="justify">
If you have Java installed, simply download all files from this repository and write the following in the consolle:
</p>

<p align="center">
<i>java -cp .;./lib/* -Dfile.encoding=utf-8 ctDTSreasoner ctDTS.ttl ./Examples/Example1.ttl inferredABox.rdf</i>
</p>

<p align="justify">
The SPARQL rules in the file ctDTS.ttl will be executed on the state of affairs in the file Example1.ttl and the result will be written in the file inferredABox.ttl. To run the other examples, simply change the second parameter.
</p>

