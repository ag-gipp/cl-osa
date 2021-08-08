CL-OSA (Cross-Language Ontology-based Similarity Analysis)
=================================================================

This is an extended version for the Cross-language plagiarism detection using Wikidata method CL-OSA.  
It additionally can do detailed analysis on character-level. To evaluate the detailed analysis results,
currently the PAN-PC-11 evaluation-corpus is used. Also the extended version contains possibilities 
to cache intermediate results and analysis tools (i.e. "Featurama") for optimizing the parametrization of the used algorithms.
 

![Overview CL-OSA](./closa-tng-diag.png?raw=true "Overview over CL-OSA")

Plagiarism detection for Java
-----------------------------

CL-OSA is an algorithm to retrieve similar documents written in different languages and compare them on character-level by leveraging entity and ontology
information from a local Wikidata dump.

The algorithm can be used to assist in plagiarism detection by performing ranked retrieval of potential source documents
for a suspicious input document. Also it can identify sections in the source documents which are possible plagiarism.

The input is:
* a suspicious document (.txt)
* a list of candidate documents (.txt)

The output is:
* a score map of the suspicious id as key and a candidate id score map as value.


#### Plagiarism detection for your files

To execute the application as a standalone, you need to

* [Set up a MongoDB database](#setting-up-the-mongodb-database)
* [Import the Maven project](#import-the-maven-project)
* [Import the Java sources (optional)](#importing-the-java-sources)
* [Use the OntologyUtil class](#how-to-use)

Use the code snippet and adjust the file paths

    String suspiciousPath = "~/documents/en/35157967/0.txt";
    String candidateFolderPath = "~/documents/ja/";

    List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
         .stream()
         .map(File::getPath)
         .collect(Collectors.toList());

    Map<String, Double> candidateScoresMap = new OntologyBasedSimilarityAnalysis()
        .executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);

    System.out.println(candidateScoresMap);



#### Supported languages

* English (en)
* French (fr)
* German (de)
* Spanish (es)
* Chinese (zh)
* Japanese (ja)



### What CL-OSA does

CL-OSA takes documents as input and ranks them according to their semantic similarity.
Also, CL-OSA can compare documents on character-level (currently set to PAN-PC-11, but this can be changed by modifying the source-code, as entrypoint see here: 
PANPC11CharacterLevelEval.doCharacterLevelEval). 

### What CL-OSA does not
CL-OSA does not search the internet for possible sources. This is addressed in different algorithms.

## Extensions to CL-OSA in this project
###  Featurama

The Featurama project is an analysis tool to find correlations between variables during the detailed 
analysis process.  
When having the possibility to choose between a multitude of parameters
and scoring metrics, it can be used to find optimal parametrization.

The project offers functionality to store and save observations during plagiarism detection. Each observation holds several features, which each have a name and numerical value. The observations are stored in an observation holder, which can be transformed into a matrix. For this matrix the correlation and covariance matrices can be calculated. Furthermore it is possible to perform a PCA on the data, which the option of reducing the dimension at the same time.
To use featurama an observationHolder object must be initialised. For each observation a new observation object is created and the features added, by storing them in a hash map and adding this hash map to the obervation. This observation is added to the observationHolder, which in turn can be saved by converting it to a matrix and saving as CSV file.
### Detailed Analysis for PAN-PC-11
See 'How it works section: Detailed Analysis Evaluation'


Setting up the MongoDB database
-------------------------------

### Alternatives to local storage

To use CL-OSA, you need to set up a MongoDB database. Alternatively, you can use the Wikidata SPARQL API directly.
In this case, you have to change the static import inside
[WikidataEntityExtractor](src/main/java/com/iandadesign/closa/util/wikidata/WikidataEntityExtractor.java) from
> import static org.sciplore.pds.util.wikidata.WikidataDumpUtil.*;

to
> import static org.sciplore.pds.util.wikidata.WikidataSparqlUtil.*;

However, this alternative is only recommended for testing purposes as
1. the number of queries is limited,
2. Wikidata is updated so frequently such that results become non-deterministic, and
3. querying a public web service is slow.

### The setup process

#### Docker

Set up using docker-compose.yml


#### Local MongoDB (not recommended)

Launch a new MongoDB instance on your desired host and port.

The default is a MongoDB instance running on localhost, port 27017.
Then, run this [Python script](docker/mongo/wikidata-dump-mongo-import.py) to import the Wikidata dump.

Usage is

    python wikidata-dump-mongo-import.py -h <host> -p <port>

If the current directory already contains a file named *latest-all.json.bz2*
the download step will be skipped. Otherwise, the download of a file of approx. 30 GB will start from
[Wikidata's dump site](https://dumps.wikimedia.org/wikidatawiki/entities/latest-all.json.bz2).

When the file has finished downloading or is already present, it will begin importing its contents
into the given MongoDB instance. This takes about 52 hours. Make sure you have enough disk space available
for the database, about 275 GB.


When the import and index creation has finished, you should have a database called "wikidata", containing
the collections "entities", "entitiesGraph", and "entitiesHierarchyPersistent".

Import the Maven project
------------------------

### Include the library

Add the following dependency to your pom.xml:

     <dependency>
        <groupId>com.iandadesign</groupId>
        <artifactId>closa</artifactId>
        <version>1.3</version>
     </dependency>


### Configuration

If your MongoDB instance runs not on localhost or a port other than 27017, or if you plan to use
a different Wikidata SPARQL host, create a config.properties file inside your main resources folder.

Standard configuration is the following:

    mongodb_host=localhost
    mongodb_port=27017
    wikidata_sparql_endpoint=https://query.wikidata.org/sparql


Importing the Java sources
--------------------------

Note: this only applies if you would like to contribute to the project or if you plan to making direct
changes to the code.

I recommend using IntelliJ IDEA to open the project and install the Maven dependencies. If you prefer using other tools,
you will still need Maven. Checkout the project from the
[GitHub repository](https://github.com/bishutsuka/citeplag-dev-backend/tree/clpd-merge-backup-new)
and make sure you are inside the branch *clpd-merge-backup-new*.

To get all dependencies running, run

    mvn install

inside the cloned repository directory.

How to use
----------

### API

Instantiate OntologyBasedSimilarityAnalysis with a LanguageDetector and a TextClassifier and execute the method "executeAlgorithmAndComputeScores". First argument is the suspicious file path
and second argument is the candidate file path.

    String suspiciousPath = "~/documents/en/35157967/0.txt";
    String candidateFolderPath = "~/documents/ja/";

    List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
         .stream()
         .map(File::getPath)
         .collect(Collectors.toList());

    Map<String, Double> candidateScoreMap = new OntologyBasedSimilarityAnalysis()
         .executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);
                                                            
    System.out.println(candidateScoreMap);

The output is a scored map of candidates.

Pre-processing steps will be saved to a directory named *preprocessed* which will be created in your
home directory if the input documents are also in the home directory. Otherwise, the directory will be created in
the root directory.

The [test class](/com/iandadesign/closa/OntologyBasedSimilarityAnalysisTest.java)
contains unit tests as demonstration. It uses the test resource files that come with the Java project.

If you already know that your files are of a certain language or topic, instantiate a fitting
LanguageDetector and TextClassifier and provide them to the OntologyBasedSimilarityAnalysis constructor:

    LanguageDetector languageDetector = new LanguageDetector(Arrays.asList("en", "ja"));
    TextClassifier textClassifier = new TextClassifier(Arrays.asList("fiction", "neutral"));

    OntologyBasedSimilarityAnalysis analysis = new OntologyBasedSimilarityAnalysis(languageDetector, textClassifier);

### .jar

When working with the .jar package, usage is the following:

    java -jar closa-1.0-SNAPSHOT.jar -s suspicious_file.txt -c candidate_folder -o output.txt [-l lang1 lang2 -t topic1 topic2]


###Entity extraction

If you are interested in extracting Wikidata entities from a text, you can use WikidataEntityExtractor's
methods "extractEntitiesFromText" or "annotateEntitiesInText".


#### .jar

    java -cp closa-1.0-SNAPSHOT.jar com.iandadesign.closa.commandLine.WikidataEntityExtraction -i input.txt -o output.txt [-l lang1 lang2 -t topic1 topic2 -a]

The -a flag switches from simple entity list output to entity annotations inside the text using xml-like tags, e.g.

    Scientists prove there is water on Mars

becomes

    Scientists<span token="Scientists" qid="Q901"/> prove there is water<span token="water" qid="Q283"/> on Mars<span token="Mars" qid="Q111"/>

Evaluation 
----------
## Candidate Retrieval Evaluation
If you desire to evaluate CL-OSA's in terms of precision, recall and F1-score for the candidate retrieval, instantiate the class
CLOSAEvaluationSet with the directory containing the suspicious files and the directory containing the
candidate files.

### Equal number of suspicious and candidate documents

    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/documents/en"), "en",
                new File("~/documents/ja"), "ja"
        );

        evaluationSetCLOSA.printEvaluation();

    } catch (IOException e) {
        e.printStackTrace();
    }

Both folders need to contain the same amount of files because the evaluation method
assumes a one-to-one mapping between suspicious and candidate files to use as a ground truth
to evaluate against.
Because of this, the suspicious files and their respective candidate file have to be named the same.
If the suspicious file name contains the language code, the candidate has to contain its own language code instead, i.e.

| suspicious file name | candidate file name |
|----------------------|---------------------|
| 001028739.EN.txt     | 001028739.ZH.txt    |
| Fragment 014 05.txt  | Fragment 014 05.txt |



If you don't know the documents' languages or you are mixing languages inside the directories, you can
omit the languages parameter, but then both file names have to be exactly the same:

    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/fragments"),
                new File("~/sources")
        );
        
        evaluationSetCLOSA.printEvaluation();
        
    } catch (IOException e) {
        e.printStackTrace();
    }

### More candidate documents than suspicious documents

If you would like to increase the pool of possible candidate files that have no suspicious file associated,
you can add a third directory parameter:


    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/documents/en"), "en",
                new File("~/documents/ja"), "ja",
                new File("~/documents/extra")
        );

        evaluationSetCLOSA.printEvaluation();

    } catch (IOException e) {
        e.printStackTrace();
    }

## Detailed Analysis Evaluation
CL-OSA-TNG offers a component which can compare documents on character level
and find supposed plagiarism within the PAN-PC-11 dataset.

As pre-requisite it is required to download the PAN-PC-11 corpus and place it in the
path defined in PAN11CharacterLevelEval.java. Also to

    public static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";
    public static String preprocessedCachingDir = "/data/CLOSA_data/preprocessed";



For running the PAN-PC-11 detailed evaluation there are a  runDetailedEval.sh
two bash scripts. A single run of the evaluation can be started like this:

    cd cl-osa-tng
    # Modify the settings of the PAN-PC-11 detailed evaluation 
    vim ./src/main/java/com/iandadesign/closa/model/SalvadorAnalysisParameters.java
    # Run the evaluaton-script: this recompiles the java code and saves logs with <nameOfTest> prefix
    # The nohup command is optional, but recommended when running longer tests 
    # The output can be muted with '&' it is redirected to a file in './mylogs/<nameOfTest><params' prefix
    nohup ./runDetailedEval.sh <nameOfTest> &
    # Watch the current log (if nohup and & is used) 
    watch -n 1 tail -n 35 ./nohup.out

For running more than one PAN-PC-11 detailed evaluation, i.e. for testing
multiple sets of parameters, a script running batches of evaluations can be used.

    cd cl-osa-tng
    # Modify the basic settings of the PAN-PC-11 detailed evaluation
    vim ./src/main/java/com/iandadesign/closa/model/SalvadorAnalysisParameters.java
    # Modify the batched script (especially <batch base name>) 
    vim runDetailedEvalBatched.sh
    # Run the batched script  
    # The nohup command is optional, but recommended when running longer tests 
    # The output can be muted with '&' it is redirected to a file in './mylogs/<batch base name><params' prefix
    nohup ./runDetailedEvalBatched.sh &
    # Watch the current log (if nohup and & is used) 
    watch -n 1 tail -n 35 ./nohup.out




How it works
------------

The CL-OSA pipeline consists of the following steps:

1. Preprocessing
    1. Tokenization and named entity recognition
    2. Text classification
    3. Entity extraction
        1. Entity filtering by topic
        2. Entity filtering by named entity type
    4. Entity disambiguation
2. Analysis
    1. Calculate cosine similarity between all document pairs (or selected document fragments)
    2. Return top ranked document (or fragments)
    3. (in detailed analysis) Merge supposed plagiarized-fragments on base of their proximity within the document


Implementation details
----------------------

### Class descriptions

The classes of concern, sorted by decreasing order of relevance. For general use, the first is enough.

1. **OntologyBasedSimilarityAnalysis**: main class
2. **WikidataEntity**: represents a Wikidata entity
3. **WikidataEntityExtractor**: converts a text to a list of Wikidata entities
4. **WikidataDisambiguator**: resolves lists of Wikidata entity candidates to a single one
5. **WikidataDumpUtil**: queries entity and ontology data from the MongoDB database
6. **WikidataSparqlUtil**: queries entity properties from Wikidata's SPARQL endpoint
7. **TokenUtil**: performs tokenization on texts, including NER
8. **Dictionary**: builds inverted index dictionaries
9. **WordNetUtil**: uses WordNet to map verbs to nouns of same meaning
10. **PAN11CharacterLevelEval** PAN-PC-11 based detailed analysis evaluation


### Stack trace

The methods called, starting with the "main" method executeAlgorithmAndComputeScores inside OntologyUtil:

* OntologyBasedSimilarityAnalysis.executeAlgorithmAndComputeScores
    * OntologyBasedSimilarityAnalysis.preProcess
        * OntologyBasedSimilarityAnalysis.extractEntitiesFromText
            * WikidataEntityExtractor.extractEntitiesFromText
                * extractEntitiesFromTextWithoutDisambiguation
                    * TokenUtil.namedEntityTokenize
                    * TextClassificationUtil.classifyText
                    * WordNetUtil.mapVerbToNoun
                    * WikidataDumpUtil.getEntitiesByToken
                        * WikidataSparqlUtil.isInstance
                        * WikidataSparqlUtil.isLocation
                        * WikidataSparqlUtil.isHuman
                        * WikidataSparqlUtil.isOrganization
                        * WikidataSparqlUtil.isNaturalNumber
                        * WikidataSparqlUtil.isHumanLanguage
                        * WikidataSparqlUtil.getPropertyValue
                    * TokenUtil.isLatinAlphabet
                    * WikidataSparqlUtil.isGene
                    * WikidataSparqlUtil.isNaturalNumber
                * WikidataEntity.ancestorCountDisambiguate
    * OntologyBasedSimilarityAnalysis.performCosineSimilarityAnalysis
        * Dictionary


Issue management
----------------

You can create issues here:

https://github.com/ag-gipp/cl-osa-tng/issues/new/choose

Contributing
------------

If you wish to contribute, please create a feature branch named YYYY-MM-dd_name_of_feature.
When your branch is ready to review, please create a pull request.


DevOps
------

### Moving to a new version number

A new snapshot version is set like this:

    $ mvn versions:set -DnewVersion=1.x-SNAPSHOT

A new version is set like the following:

    $ mvn versions:set -DnewVersion=1.x


### Deploying to Maven Central

Create a GPG key for signing.

    gpg --gen-key

The public key's last 8 characters are the key id:

    gpg --list-keys

Publish the key:

    gpg --keyserver keyserver.ubuntu.com --send-keys {{keyId}}

Settings file ~/.m2/settings.xml should look like the following:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
      <servers>
        <server>
          <id>ossrh</id>
          <username>{{username}}</username>
          <password>{{password}}</password>
        </server>
      </servers>
      <profiles>
        <profile>
          <id>ossrh</id>
          <activation>
            <activeByDefault>true</activeByDefault>
          </activation>
          <properties>
            <gpg.executable>gpg</gpg.executable>
            <gpg.passphrase>{{passphrase}}</gpg.passphrase>
          </properties>
        </profile>
      </profiles>
    </settings>

Signing:

    $ mvn verify -DskipTests=true -Denv=deploy

Deploying:

    $ mvn deploy -Denv=deploy

Releasing is performed under [OSS Sonatype Nexus Repository Manager](https://oss.sonatype.org/#stagingRepositories).
A repository called comiandadesign should be present with status open. Close it and wait for the tests to succeed.




