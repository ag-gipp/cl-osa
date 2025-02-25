package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.TokenUtil;
import com.iandadesign.closa.util.WordNetUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * WikidataEntityExtractor is an almost singleton class that performs entity extraction from a text.
 * <p>
 * Created by Fabian Marquart on 2018/08/03.
 */
public class WikidataEntityExtractor {

    private static boolean useSparql = true;
    private static boolean doParallelRequests = true;
    private static int lengthSublistTokens = 3;

    static {
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read MongoDB properties.
     *
     * @throws IOException When property file could not be loaded.
     */
    private static void loadConfig() throws IOException {
        InputStream inputStream = null;

        try {
            Properties properties = new Properties();
            String propFileName = "config.properties";
            String propFileLocalName = "config-local.properties";

            // switch to config-local if it exists
            if (WikidataDumpUtil.class.getClassLoader().getResource(propFileLocalName) != null) {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileLocalName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            } else {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            }

            // get the property value and print it out
            useSparql = properties.getProperty("use_sparql").equals("true");
            doParallelRequests = properties.getProperty("do_parallel_requests").equals("true");
            lengthSublistTokens = Integer.parseInt(properties.getProperty("length_sublist_tokens"));
            System.out.println("Settings for WikidataEnitityExtractor");
            System.out.println("Use SparQL:    "+ useSparql);
            System.out.println("Do ParallelRQ: "+ doParallelRequests);
            System.out.println("Len Subtoken:  "+ lengthSublistTokens);



        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * Extract Wikidata entities from given text, language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction, entity disambiguation
     *
     * @param text         the text
     * @param languageCode language
     * @return list of Wikidata entities found in the text
     */
    public static List<WikidataEntity> extractEntitiesFromText(String text, String languageCode) {
        return extractEntitiesFromText(text, languageCode, new TextClassifier().classifyText(text, languageCode));
    }

    /**
     * Annotate Wikidata entities in a given text, with language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction, entity disambiguation
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entities found in the text
     */
    public static String annotateEntitiesInText(String text, String languageCode, Category category) {
        StringBuilder annotatedText = new StringBuilder(text);

        Map<Token, List<WikidataEntity>> tokenEntitiesMap = buildTokenEntitiesMap(text, languageCode, category);

        LinkedHashMap<Token, WikidataEntity> tokenEntityMap = new LinkedHashMap<>();

        tokenEntitiesMap.forEach((token, entities) -> {
            // disambiguate
            if (entities.size() > 1) {
                tokenEntityMap.put(token, WikidataDisambiguator.disambiguateByAncestorCount(entities, text, languageCode));
            } else if (entities.size() == 1) {
                tokenEntityMap.put(token, entities.get(0));
            }
        });

        List<Token> tokensBackwards = new ArrayList<>(tokenEntityMap.keySet());
        Collections.reverse(tokensBackwards);

        for (Token token : tokensBackwards) {
            annotatedText.insert(token.getEndCharacter(), "<span token=\"" + token.getToken() + "\" qid=\"" + tokenEntityMap.get(token).getId() + "\"/>");
        }

        return annotatedText.toString();
    }


    public static List<SavedEntity> extractSavedEntitiesFromText(String text, String languageCode, Category category) {
        // In technical terms this finds one corresponding WikidataEntity when there have been multiple found for a token.
        List<SavedEntity> savedEntities = new ArrayList<>();
        Map<Token, List<WikidataEntity>> tokenEntitiesMap = buildTokenEntitiesMap(text, languageCode, category);
        for(Map.Entry<Token, List<WikidataEntity>> mapEntry:tokenEntitiesMap.entrySet()){
            Token currentToken = mapEntry.getKey();
            List<WikidataEntity> correspondingEntities = mapEntry.getValue();
            SavedEntity savedEntity = new SavedEntity();
            savedEntity.setToken(currentToken);

            if (correspondingEntities.size() == 1) {
                // first pass to see which entities are already unambiguous
                savedEntity.setWikidataEntityId((correspondingEntities.get(0).getId()));
            }else if(correspondingEntities.size() > 1){
                // disambiguate
                WikidataEntity disambiguatedEntity = WikidataDisambiguator
                        .disambiguateByAncestorCount(correspondingEntities, text, languageCode);

                if (disambiguatedEntity != null) {
                    savedEntity.setWikidataEntityId(disambiguatedEntity.getId());
                }
            }
            if(savedEntity.getWikidataEntityId()!=null) {
                // For now only save tokens with an entity.
                savedEntities.add(savedEntity);
            }
        }
        return savedEntities;
    }

    /**
     * Extract Wikidata entities from given text, language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction, entity disambiguation
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entities found in the text
     */
    public static List<WikidataEntity> extractEntitiesFromText(String text, String languageCode, Category category) {
        List<List<WikidataEntity>> extractedEntities = extractEntitiesFromTextWithoutDisambiguation(text, languageCode, category);
        List<WikidataEntity> disambiguatedEntities = new ArrayList<>();

        // first pass to see which entities are already unambiguous
        for (List<WikidataEntity> currentEntities : extractedEntities) {
            if (currentEntities.size() == 1) {
                disambiguatedEntities.add(currentEntities.get(0));
            }
        }

        for (List<WikidataEntity> currentEntities : extractedEntities) {
            // disambiguate
            if (currentEntities.size() > 1) {
                WikidataEntity disambiguatedEntity = WikidataDisambiguator.disambiguateByAncestorCount(currentEntities, text, languageCode);

                if (disambiguatedEntity != null) {
                    disambiguatedEntities.add(disambiguatedEntity);
                }
            }
        }

        return disambiguatedEntities;
    }


    /**
     * Extract Wikidata entities from given text, language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(String text, String languageCode) {
        return extractEntitiesFromTextWithoutDisambiguation(
                text,
                languageCode,
                new TextClassifier().classifyText(text, languageCode));
    }

    /**
     * Extract Wikidata entities from given text, language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(
            String text,
            String languageCode,
            Category category) {
        return new ArrayList<>(buildTokenEntitiesMap(text, languageCode, category).values());
    }


    /**
     * Extract Wikidata entities from given text, language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static Map<Token, List<WikidataEntity>> buildTokenEntitiesMap(
            String text,
            String languageCode,
            Category category) {

        // tokenize
        List<Token> tokenList = TokenUtil.namedEntityTokenize(text, languageCode).stream()
                .flatMap(List::stream).collect(Collectors.toList());

        // optional per-language stop-word removal
        if (languageCode.equals("ja")) {
            tokenList = TokenUtil.removeStopwords(tokenList, languageCode);
        }
        //JS: why is this done?, combined labels don't give answers
        //https://query.wikidata.org/#SELECT%20DISTINCT%20%3Fitem%20%3FitemDescription%20WHERE%20%7B%0A%20%20%3Fitem%20%3Flabel%20%22media%20is%20an%22%40en.%0A%20%20%3Farticle%20schema%3Aabout%20%3Fitem.%0A%20%20SERVICE%20wikibase%3Alabel%20%7B%20bd%3AserviceParam%20wikibase%3Alanguage%20%22en%22.%20%7D%0A%7D
        List<List<List<Token>>> subtokensLists = getSublistsOfSize(tokenList, lengthSublistTokens);
        //List<List<List<Token>>> subtokensLists = getSublistsOfSize(tokenList, 1);
        // filter
        List<String> forbiddenPartOfSpeechTags = Arrays.asList(",", ":", ".", "SYM", "TO", "IN", "PP", "PP$", "SENT");

        ProgressBar progressBar = new ProgressBar(String.format("Extract entities from %s %s text.", languageCode, category), subtokensLists.size(), ProgressBarStyle.ASCII);
        progressBar.start();

        //construction site
        Map<Token, List<WikidataEntity>> tokenEntitiesMap;
        if (doParallelRequests) {
            // Run with parallelism.
            tokenEntitiesMap = subtokensLists.parallelStream()
                    //Map<Token, List<WikidataEntity>> tokenEntitiesMap = subtokensLists.stream()
                    .map((List<List<Token>> subtokensList) -> {
                        return processSubtokens(subtokensList, languageCode, forbiddenPartOfSpeechTags, category, progressBar);
                    })
                    .flatMap(m -> m.entrySet().stream())
                    .filter(distinctByKey(Map.Entry::getKey))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            progressBar.stop();
        } else{
            // Run without parallelism.
            tokenEntitiesMap = subtokensLists.stream()
                    .map((List<List<Token>> subtokensList) -> {
                        return processSubtokens(subtokensList,languageCode,forbiddenPartOfSpeechTags, category, progressBar);
                    })
                    .flatMap(m -> m.entrySet().stream())
                    .filter(distinctByKey(Map.Entry::getKey))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            progressBar.stop();
        }

        return tokenEntitiesMap;
    }

    public static LinkedHashMap<Token, List<WikidataEntity>>  processSubtokens(List<List<Token>> subtokensList,
                                                                               String languageCode,
                                                                               List<String> forbiddenPartOfSpeechTags,
                                                                               Category category,
                                                                               ProgressBar progressBar){

        LinkedHashMap<Token, List<WikidataEntity>> currentTokenEntitiesMap = new LinkedHashMap<>();

        for (int i = 0; i < subtokensList.size(); i++) {
            List<Token> subtokens = subtokensList.get(i);

            boolean isWhitespaceSeparatedLanguage = !(languageCode.equals("zh") || languageCode.equals("ja") ||
                    languageCode.equals("ar"));

            // build token from sub-tokens TODO: JS here make the correct sentence assignment for tokens
            Token token = new Token(subtokens, isWhitespaceSeparatedLanguage ? " " : "");

            // filter
            if (Objects.requireNonNull(TokenUtil.getStopwords(languageCode)).contains(token.getLemma())) {
                continue;
            }
            if (forbiddenPartOfSpeechTags.contains(token.getPartOfSpeech())) {
                continue;
            }

            // verb to noun mapping
            if (token.getPartOfSpeech().contains("V")) {
                if (token.getToken().contains("ing") || token.getToken().contains("tion")) {
                    token.setLemma(token.getToken());
                } else {
                    token = WordNetUtil.mapVerbToNoun(token);
                }
            }

            // extract entities
            List<WikidataEntity> currentEntities = getEntitiesByToken(token,
                    // if english text bits are contained in a chinese text
                    languageCode.equals("zh") && TokenUtil.isLatinAlphabet(token)
                            ? "en"
                            : languageCode, category)
                    .stream()
                    // no CJK character pages
                    .filter(entity ->
                            (!languageCode.equals("zh") && !languageCode.equals("ja"))
                                    || !entity.getDescriptions().getOrDefault("en", "").contains("CJK character (hanzi/kanji/hanja)"))
                    // no Wikimedia disambiguation pages
                    .filter(entity -> !entity.getDescriptions().getOrDefault(languageCode, "").contains("Wikimedia "))
                    // if text is not about fiction, remove pages about music, movies or tv shows
                    .filter(entity -> category.equals(Category.fiction) || !isCreativeWork(entity))
                    // if text is not about biology, remove pages about genes
                    .filter(entity -> category.equals(Category.biology) || !isGene(entity))
                    // only keep pages about numbers when the token is numeric
                    .filter(entity -> !StringUtils.isNumeric(entity.getOriginalLemma())
                            || (StringUtils.isNumeric(entity.getOriginalLemma()) && isNaturalNumber(entity)))
                    .collect(Collectors.toList());

            currentTokenEntitiesMap.put(token, currentEntities);

            // if larger group has result, don't consider smaller token groups anymore
            if (currentEntities.size() > 0) {
                if (i + 1 < subtokensList.size() && subtokens.size() > subtokensList.get(i + 1).size()) {
                    break;
                }
            }
        }
        progressBar.step();
        return currentTokenEntitiesMap;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Use either SPARQL or MongoDB implementation according to config.
     *
     * Retrieves the entity matching the token:
     * - tokens that are named entities only retrieve instances from Wikidata
     * - tokens that are not named entities only retrieve classes from Wikidata
     *
     * @param token        : the token to find, by lemma.
     * @param languageCode : the label language.
     * @param category     : the text's category from which the token was taken.
     * @return the results as Wikidata entities.
     */
    private static List<WikidataEntity> getEntitiesByToken(Token token, String languageCode, Category category) {
        // System.out.println("TOKENLOG: "+token.getToken());
        if(useSparql){
            return WikidataSparqlUtil.getEntitiesByToken(token, languageCode, category);
        }else{
            return WikidataDumpUtil.getEntitiesByToken(token, languageCode, category);
        }



    }

    /**
     * Use either SPARQL or MongoDB implementation according to config.
     *
     * Returns true if the entity is instance of gene.
     *
     * @param entity entity
     * @return true if the entity is instance of gene.
     */
    private static boolean isGene(WikidataEntity entity) {
        return useSparql ? WikidataSparqlUtil.isGene(entity) : WikidataDumpUtil.isGene(entity);
    }

    /**
     * Use either SPARQL or MongoDB implementation according to config.
     *
     * Returns true if the entity is instance of a subclass of creative work.
     *
     * @param entity entity
     * @return true if the entity is instance of a subclass of creative work.
     */
    private static boolean isCreativeWork(WikidataEntity entity) {
        return useSparql ? WikidataSparqlUtil.isCreativeWork(entity) : WikidataDumpUtil.isCreativeWork(entity);
    }


    /**
     * Use either SPARQL or MongoDB implementation according to config.
     * Returns true if the entity is instance of natural number.
     *
     *
     * @param entity entity
     * @return true if the entity is instance of natural number.
     */
    private static boolean isNaturalNumber(WikidataEntity entity) {
        return useSparql ? WikidataSparqlUtil.isNaturalNumber(entity) : WikidataDumpUtil.isNaturalNumber(entity);
    }



    /**
     * Extract Wikidata entities from given text and language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER, filter by POS, entity extraction.
     *
     * @param text           the text
     * @param languageCode   language
     * @param textClassifier text classifier to use
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(String text, String languageCode, TextClassifier textClassifier) {
        return extractEntitiesFromTextWithoutDisambiguation(text, languageCode, textClassifier.classifyText(text, languageCode));
    }


    /**
     * Gets a list of T sublists of sizes 1 to n, e.g. {1, 2, 3, 4} with n = 3 becomes
     * {
     * {{1,2,3},{1,2},{2,3},{1},{2},{3}},
     * {{2,3,4},{2,3},{3,4},{2},{3},{4}}
     * }
     *
     * @param list input list.
     * @param n    target sublist size maximum.
     * @param <T>  type parameter.
     * @return List of T sublists of sizes 1 to n.
     */
    static <T> List<List<List<T>>> getSublistsOfSize(List<T> list, int n) {
        if (list.size() == 1) {
            return Collections.singletonList(Collections.singletonList(list));
        }

        List<List<List<T>>> sublists = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {

            List<List<T>> sublist = new ArrayList<>();

            List<T> currentSublist = new ArrayList<>();

            for (int j = 0; j < n && i + j < list.size(); j++) {
                currentSublist.add(list.get(i + j));
                sublist.add(new ArrayList<>(currentSublist));
            }

            sublists.add(sublist);
        }

        // move last n-2 lists into preceding list
        int initialSublistsSize = sublists.size();

        for (int i = initialSublistsSize - 1; i > 0; i--) {
            if (sublists.get(i).stream().anyMatch(currentList -> currentList.size() == n)) {
                break;
            }

            sublists.get(i - 1).addAll(sublists.get(i));
            sublists.remove(i);
        }

        // sort by list length
        sublists.forEach(sublist -> sublist.sort(Comparator.comparing(List::size, Comparator.reverseOrder())));

        return sublists;
    }


}


