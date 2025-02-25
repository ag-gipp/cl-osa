package com.iandadesign.closa.evaluation.impl;

import com.google.common.collect.ImmutableMap;
import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.ConceptUtil;
import com.iandadesign.closa.util.TokenUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Cross-Language Explicit Semantic Analysis, by Potthast, Stein and Anderka (2008).
 * <p>
 * Created by Fabian Marquart on 2018/11/11.
 */
public class CLESAEvaluationSet extends EvaluationSet<Double> {

    private static final String databaseName = "wikipedia";
    private static final String pathPrefix = "/home/marquart/eval_data";

    // text stores documents with format:
    //
    // {
    //   id: "12",
    //   url: "https://en.wikipedia.org/wiki?curid=12",
    //   title: "Anarchism",
    //   ids: { "en": "12", "ja": .... },
    //   text: "..."
    // }
    private static final String articleCollectionName = "articles";
    private static MongoCollection<Document> articleCollection;

    // max number of wikipedia articles (recommended by Potthast 2008)
    // "If high retrieval speed or a high multilinguality is desired, documents should be represented as 1000- dimensional concept vectors.
    // At a lower dimension the retrieval quality deteriorates significantly.
    // A reasonable trade-off between retrieval quality and runtime is achieved for a concept space dimensionality between 1 000 and 10 000."
    private static final int wikipediaArticleLimit = 20000;

    static {
        MongoDatabase database = WikidataDumpUtil.getMongoClient().getDatabase(databaseName);
        articleCollection = database.getCollection(articleCollectionName);
        // extractWikipediaArticlesAndStore();
    }

    private static final Map<String, Integer> supportedLanguages = ImmutableMap.of(
            "en", 5904000,
            "fr", 2128000,
            "es", 1536000,
            "zh", 1068000,
            "ja", 1162000);

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @throws IOException if files not found.
     */
    public CLESAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    /**
     * Initializes the evaluationSet with max file count. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @param maxFileCount       maximum file count.
     */
    public CLESAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int maxFileCount) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, maxFileCount);
    }

    /**
     * Get Wikipedia articles (in document language) from Wikipedia dump that has been preprocessed
     * by WikiExtractor.py, tokenize them and store them into the mongo collection {{documentLanguage}}Tokens.
     */
    public static void extractWikipediaArticlesAndStore() {
        try {
            // 2.1 Walk the Wikipedia dump files
            //     only if not all have been processed into the MongoDB collection
            articleCollection.createIndex(new Document("title", 1), new IndexOptions().unique(true));

            for (Map.Entry<String, Integer> languageEntry : supportedLanguages.entrySet()) {
                String language = languageEntry.getKey();
                articleCollection.createIndex(new Document("languages", 1), new IndexOptions());

                System.out.println("Current language = " + language);

                if (articleCollection.find(new Document("text." + language, new Document("$exists", true))).first() != null) {
                    System.out.println("Language present.");
                    continue;
                }

                Path wikipediaExtractedDumpPath = Paths.get(pathPrefix + "/wikipedia/output_" + language + "/");

                // check wikipedia dump files
                if (Files.notExists(Paths.get(pathPrefix + "/wikipedia/output_" + language + "/"))) {
                    throw new FileNotFoundException("The WikiExtractor.py output file is missing or not named according " +
                            "to the format \"output-{LANGUAGE_CODE}\".");
                }


                System.out.println("Preprocess Wikipedia dump.");

                ProgressBar progressBar = new ProgressBar("Find relevant Wikipedia dump files:", languageEntry.getValue(), ProgressBarStyle.ASCII).start();
                AtomicInteger progress = new AtomicInteger(0);

                // get the articles in the Wikipedia dump
                Files.walk(wikipediaExtractedDumpPath)
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.endsWith(".DS_Store"))
                        .sorted()
                        .map(path -> {
                            // split the file into the documents it contains
                            try {
                                org.jsoup.nodes.Document parsedFile = Jsoup.parse(FileUtils.readFileToString(new File(path.toUri()), UTF_8));
                                return parsedFile.select("doc");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .forEach((Element document) -> {
                            try {
                                String id = document.attr("id");
                                String url = document.attr("url");
                                String title = document.attr("title");
                                String text = document.text();

                                storeWikipediaArticleDocument(title, url, text, language);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                            progressBar.stepTo(progress.incrementAndGet());
                        });

                progressBar.stop();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<Double> preProcess(String documentPath, String documentLanguage) {
        // FIXME: this method consumes too much memory
        List<Double> preProcessed = new ArrayList<>();
        int existingWikipediaArticleCount = 0;

        Path newFullPath =
                documentPath.contains(pathPrefix)
                        ? Paths.get(documentPath.replace(pathPrefix, pathPrefix + "/preprocessed/" + this.getClass().getName() + "/"))
                        : Paths.get(documentPath.replace("src", "src/preprocessed/" + this.getClass().getName() + "/"));

        if (new File(newFullPath.toUri()).exists()) {
            try {
                List<String> lines = FileUtils.readLines(new File(newFullPath.toUri()), UTF_8);

                if (lines.size() == wikipediaArticleLimit) {
                    return lines.stream().map(Double::parseDouble).collect(Collectors.toList());
                } else if (lines.size() < wikipediaArticleLimit) {
                    preProcessed = lines.stream().map(Double::parseDouble).collect(Collectors.toList());
                    existingWikipediaArticleCount = lines.size();
                } else {
                    return lines.stream().limit(wikipediaArticleLimit).map(Double::parseDouble).collect(Collectors.toList());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            List<Token> documentTokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(
                    FileUtils.readFileToString(new File(documentPath), UTF_8),
                    documentLanguage);

            Document query = new Document("languages", new Document("$all", documentLanguages));

            FindIterable<Document> queryResult = articleCollection.find(query)
                    .noCursorTimeout(true)
                    .sort(new Document("title", 1))
                    .skip(existingWikipediaArticleCount)
                    .limit(wikipediaArticleLimit - existingWikipediaArticleCount);

            if (!queryResult.iterator().hasNext()) {
                // throw new IllegalStateException("No common articles");
            }

            ProgressBar progressBar = new ProgressBar("Preprocessing", wikipediaArticleLimit - existingWikipediaArticleCount, ProgressBarStyle.ASCII);
            progressBar.start();

            // collect all articles into list
            for (Document article : queryResult) {
                List<Token> articleTokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(
                        article.get("text", Document.class).getString(documentLanguage),
                        documentLanguage);
                double similarity = Dictionary.cosineSimilarity(documentTokens, articleTokens);
                preProcessed.add(similarity);

                progressBar.step();
            }

            progressBar.stop();

            return preProcessed;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Perform analysis (and retrieval).
     */
    @Override
    protected void performAnalysis() {
        for (Map.Entry<String, List<Double>> suspiciousEntry : suspiciousIdTokensMap.entrySet()) {
            SparseRealVector suspiciousVector = toVector(suspiciousEntry.getValue());

            for (Map.Entry<String, List<Double>> candidateEntry : candidateIdTokensMap.entrySet()) {
                SparseRealVector candidateVector = toVector(candidateEntry.getValue());
                double similarity = Dictionary.cosineSimilarity(suspiciousVector, candidateVector);

                if (!suspiciousIdCandidateScoresMap.containsKey(suspiciousEntry.getKey())) {
                    suspiciousIdCandidateScoresMap.put(suspiciousEntry.getKey(), new HashMap<>());
                }

                suspiciousIdCandidateScoresMap.get(suspiciousEntry.getKey())
                        .put(candidateEntry.getKey(), similarity);
            }
        }
    }

    /**
     * Convert double list to sparse real vector.
     *
     * @param doubles double list
     * @return vector
     */
    private SparseRealVector toVector(List<Double> doubles) {
        SparseRealVector vector = new OpenMapRealVector(doubles.size());

        for (int i = 0; i < doubles.size(); i++) {
            vector.setEntry(i, doubles.get(i));
        }

        return vector;
    }


    /**
     * Take in id, url, title, and text from wikipedia article to create a BSON document of the form
     * <p>
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * text:  { en: "...", ja: "..." }
     * }
     *
     * @param title            article title
     * @param url              article url
     * @param text             article text
     * @param documentLanguage language code
     */
    private static void storeWikipediaArticleDocument(String title, String url, String text, String documentLanguage) {
        // title in language
        String titleInEnglish = WikidataDumpUtil.getSiteLinkInEnglish(title, documentLanguage);

        if (titleInEnglish == null) {
            return;
        }

        // main information
        Document articleDocument = new Document("title", titleInEnglish)
                .append("languages", Collections.singletonList(documentLanguage))
                .append("url", url);

        articleDocument.append("text", new Document(documentLanguage, text));

        // insert into to collection
        Document existingDocument = articleCollection.find(new Document("title", titleInEnglish))
                .noCursorTimeout(true)
                .first();

        try {
            if (existingDocument == null) {
                articleCollection.insertOne(articleDocument);
            } else {
                articleCollection.updateOne(new Document("title", titleInEnglish),
                        new Document("$set", new Document("text." + documentLanguage, text))
                                .append("$push", new Document("languages", documentLanguage)));

            }
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
        }
    }
}
