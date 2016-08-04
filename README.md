Usage
=====

To see a list of available parameters:

    java -jar target/udaner-1.0-jar-with-dependencies.jar

Using the basic CRF model:

    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -test data/fly_test

The input file should be in the format that each line is a token and its label (seperated by TAB), and sentences are seperated by a blank line (./data directory contains some examples):

    Cervicovaginal	O
    foetal	B-GENE
    fibronectin	I-GENE
    in	O
    the	O
    prediction	O
    of	O
    preterm	O
    labour	O
    in	O
    a	O
    low	O
    -	O
    risk	O
    population	O
    .	O

    Varicella	B-GENE
    -	I-GENE
    zoster	I-GENE
    virus	I-GENE
    (	I-GENE
    VZV	I-GENE
    )	I-GENE
    glycoprotein	I-GENE
    gI	I-GENE
    is	O
    a	O
    type	B-GENE
    1	I-GENE
    transmembrane	I-GENE
    glycoprotein	I-GENE
    which	O
    is	O
    one	O
    component	O
    of	O
    the	O
    heterodimeric	O
    gE	B-GENE
    :	O
    gI	B-GENE
    Fc	I-GENE
    receptor	I-GENE
    complex	O
    .	O

The default method used to train the model does not perform domain adaptation. Change the method with `-method`:

    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -test data/fly_test -method FEATURE_SUBSETTING

Supported methods include `LIKELIHOOD` (default), `FEATURE_SUBSETTING`, `BOOTSTRAPPING`, `ENTROPY_REGULARIZATION`.

To use SCL, just specify `-scl` ([SVDLIBC](http://tedlab.mit.edu/~dr/SVDLIBC/) must be installed and `svd` must be in the PATH. See Installation)
    
    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -test data/fly_test -scl

To output the labels for an unlabeled data set, use `-pred` instead of `-test`

    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -pred data/fly_test

The program outputs the labels directly to `STDOUT`. If you want to save it in a file, you can redirect `STDOUT`:

    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -pred data/fly_test > fly_test_labels

To save the model in a file and use it later, use `-write-model FILE` and `-read-model FILE`:

    java -jar target/udaner-1.0-jar-with-dependencies.jar -source-train data/genetag_train -target-train data/fly_train -write-model model
    java -jar target/udaner-1.0-jar-with-dependencies.jar -read-model model -test data/fly_test 

Installation
============

There is a compiled version at `./target/udaner-1.0-jar-with-dependencies.jar`. You can just use it. 

You can also build from the source code. You need to install [Apache Maven](http://maven.apache.org/) first. Then you can execute

    mvn install

in the current directory. The executable jar should be in `./target`.

If you want to use SCL, you also need to install [SVDLIBC](http://tedlab.mit.edu/~dr/SVDLIBC/). Its source code is also in `utils/svdlibc.tgz`. After installation, svd should be in the PATH of your system.

References
==========

This work was done when I was in [the Zhang Lab, Beijing Institute of Genomics](http://cbb.big.ac.cn). The paper describing this work is in submission. Please contat the Zhang Lab for further information. Following are the main references of this work.

1. Leaman R, Gonzalez G. BANNER: an executable survey of advances in biomedical named entity recognition. Pacific Symp Biocomput. 2008;663: 652–663. Available: http://www.ncbi.nlm.nih.gov/pubmed/18229723
2. Blitzer J, Dredze M, Pereira F. Biographies, bollywood, boom-boxes and blenders: Domain adaptation for sentiment classification. ACL. 2007; 440–447. Available: http://acl.ldc.upenn.edu/P/P07/P07-1056.pdf
3. Satpal S, Sarawagi S. Domain adaptation of conditional probability models via feature subsetting. Knowl Discov Databases PKDD 2007. 2007; Available: http://link.springer.com/chapter/10.1007/978-3-540-74976-9_23
4. Wu D, Lee WS, Ye N, Chieu HL. Domain adaptive bootstrapping for named entity recognition. Proc 2009 Conf Empir Methods Nat Lang Process. 2009; 1523–1532. Available: http://dl.acm.org/citation.cfm?id=1699699
5. Lafferty JD, McCallum A, Pereira FCN. Conditional Random Fields: Probabilistic Models for Segmenting and Labeling Sequence Data. Proceedings of the Eighteenth International Conference on Machine Learning. 2001. pp. 282–289. Available: http://dl.acm.org/citation.cfm?id=645530.655813
6. McCallum AK. MALLET: A Machine Learning for Language Toolkit [Internet]. 2002. Available: http://mallet.cs.umass.edu