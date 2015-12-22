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
