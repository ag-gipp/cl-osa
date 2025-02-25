package com.iandadesign.closa.model;

import com.iandadesign.closa.util.PANFileFilter;

/**
 * Contains the basic settings for PAN-PC Detailed Plagiarism Evaluation.
 * @author Johannes Stegmüller
 */
public class ExtendedAnalysisParameters {

    //public final int LENGTH_SUBLIST_TOKENS;
    public final int NUM_SENTENCES_IN_SLIDING_WINDOW;
    public final int NUM_SENTENCE_INCREMENT_SLIDINGW;
    public final boolean DO_REGRESSION_ANALYSIS;            // Collect information for Regression Analysis form Regression Matrix (Much Hardware cost)
    public final boolean CONCEPT_OCCURENCE_WEIGHTING;       // Creates dictionaries over all entities and for file combinations, weights concepts by their occurence.
    public final boolean DO_RESULTS_ANALYSIS;               // Map actual plagiarism in CSV info, also calculate statistics, can take more CPU/RAM.
    public final boolean USE_ABSOLUTE_MATCHES_COUNT;        // De-normalized scores, just use the absolute matches, requires to change thresholds
    public final boolean ACCURATE_FIRST_LAST_INDICES;       // Gets the character indices of first match within and last match (only works with absolute match atm)
    public final double ADJACENT_THRESH;
    public final double SINGLE_THRESH;                      // CARE: THis is used as minimum thresh with adaptive clustering thresh
    public final boolean USE_ADAPTIVE_CLUSTERING_TRESH;     // Creates a clustering thresh based on the median of 2D-Matrix scores (!Takes longer because median calculation!)
    public final boolean USE_LOCAL_MEDIAN_BASED_THRESH;     // SINGLE_THRESH is replaced by median or mean (see code config) of the 9-sized window
    public final double ADAPTIVE_FORM_FACTOR;               // Form factor for adaptive clustering (Thresh = Median * FormFactor)
    public final boolean USE_BIG_CLUSTER_INCLUSION;         // Include bigger clusters to plagiarism, although they have a lower threshold
    public final double BIG_CLUSTER_SINGLE_THRESH_DIFF;            // This is a diff to the calculated or constant single tresh, in which values below get included.
    public final double BIG_CLUSTER_ADJACENT_THRESH_DIFF;
    public final int BIG_CLUSTER_MIN_SIZE;                  // Minimum size for a cluster to be recognized as 'big' in number of adjacent score values
    public final boolean DESKEW_WINDOW_SIZE;                // The bigger Window  Content Size, the lower the score, this calculates score with a form factor.
    public final double DESKEW_FORM_FACTOR;
    public final int DESKEW_MAX_WINDOW_CONTENT;             // In characters Window Content ( susp: endindex-startindex cand endindex-startindex / 2 )
    public final int CLIPPING_MARGING;                      // Number of characters in squashing clusters when a cluster still is considered clipping
    public int MAX_NUM_CANDIDATES_SELECTED;           // The number of candidates per suspicious file which get selected for detailed comparison
    public final int CR_PRINT_LIMIT;                        // Candidate number which is results are printed to files and log.
    public double CANDIDATE_SELECTION_TRESH;          // Only if scoring above thresh a similar candidate gets selected (0 for off)
    public final boolean LOG_TO_CSV;                        // Log scoring map to csv
    public final boolean LOG_PREPROCESSING_RESULTS;         // Log the sliding window comparisons to a .txt file
    public final boolean LOG_STANDARD_TO_FILE;              // Log relevant standard output to .txt file
    public final boolean LOG_ERROR_TO_FILE;                 // Log error output to .txt file
    public final boolean LOG_VERBOSE;                       // Log more file outputs
    public boolean USE_FILE_FILTER;                         // Pre-Filter the used files like defined in panFileFilter
    public boolean USE_LANGUAGE_WHITELISTING;               // Only finds plagiarism in whitelisted languages.
    public boolean RUN_EVALUATION_AFTER_PROCESSING;   // Run evaluation python script after processing the plagiarism files
    public int PARALLELISM_THREAD_DIF;
    public final boolean USE_ENHANCHED_COSINE_ANALYSIS; // if enhanched taxomony
    public PANFileFilter panFileFilter;
    public final String maxtrixStoreLocation;


    public ExtendedAnalysisParameters() throws Exception{
        // Token forming before making Wikidata query
        //LENGTH_SUBLIST_TOKENS = 3; // This is not used atm, but the parameter in config.properties dung refactoring reasons
        // Sliding window parameters (atm only possible increment == num_sentences)
        DO_REGRESSION_ANALYSIS = true; // TODO Kay: If usage, make sure DO_RESULTS_ANALYSIS is activated also.

        DO_RESULTS_ANALYSIS = true; // Marks results in CSV and does Statistical result analysis.
        USE_ABSOLUTE_MATCHES_COUNT = true; //TODO perfomance adaptions, false calculates the score twice
        ACCURATE_FIRST_LAST_INDICES = true;
        NUM_SENTENCES_IN_SLIDING_WINDOW = 20; //2  // 20
        NUM_SENTENCE_INCREMENT_SLIDINGW = 10; //1  // 10
        // Sliding window comparison thresholds
        ADJACENT_THRESH = 3.0; //0.09;//0.9; //0.09;   //0,3; 0,1
        SINGLE_THRESH =  8.0;//0.125; //0.0125; //0.125; //6.0; //0.125;// 0,7; 0.8; //0.45act; //0.7;     //0,6 8 for abs
        USE_LOCAL_MEDIAN_BASED_THRESH = false;
        USE_ADAPTIVE_CLUSTERING_TRESH = false; // false
        ADAPTIVE_FORM_FACTOR = 5.1; //5.2; // 6 still false positive, rec ok
        CLIPPING_MARGING = 6000;
        CONCEPT_OCCURENCE_WEIGHTING = true;
        USE_ENHANCHED_COSINE_ANALYSIS  = false;

        // Big cluster inclusion settings.
        USE_BIG_CLUSTER_INCLUSION = false;
        BIG_CLUSTER_SINGLE_THRESH_DIFF = 2.0; //0.060;  // 0.125; // 0.09;        // THESE PARAMETERS SEEM NOT RELEVANT
        BIG_CLUSTER_ADJACENT_THRESH_DIFF = 0.0; // 0.050; // 0.09; // 0.08;       // THESE PARAMETERS SEEM NOT RELEVANT
        BIG_CLUSTER_MIN_SIZE = 10;  // 9 too many detections, 10 ok better results, 11 no change (at 10)

        // Deskew settings.
        DESKEW_WINDOW_SIZE = false;
        DESKEW_FORM_FACTOR = 0.2;
        DESKEW_MAX_WINDOW_CONTENT = 6500;       // Just leave this COnstant at 6500 atm.
        // Candidate retrieval settings
        MAX_NUM_CANDIDATES_SELECTED = 20;
        CR_PRINT_LIMIT = 10;
        CANDIDATE_SELECTION_TRESH = 0; //0.2;
        //Settings for Logging etc
        LOG_TO_CSV= true;
        LOG_PREPROCESSING_RESULTS= true;
        LOG_STANDARD_TO_FILE= false;
        LOG_ERROR_TO_FILE=false;
        LOG_VERBOSE=false;
        // Evaluation settings
        RUN_EVALUATION_AFTER_PROCESSING = true;
        // Parallelism Settings
        PARALLELISM_THREAD_DIF = 20; // difference from available processors

        // File Filter Options
        USE_FILE_FILTER=false;
        USE_LANGUAGE_WHITELISTING=false;
        // Add a file filter (only used if USE_FILE_FILTER is true)
        panFileFilter = new PANFileFilter();
        // Add Candidate Whitelisting
        panFileFilter.addToWhiteListMultiple(true, 1, 14, 3164, 4001, 71, 76, 3317);
        // Add Suspicious Whitelisting
        panFileFilter.addToWhiteListMultiple(false, 2, 45, 20, 34);
        //panFileFilter.addToWhiteListMultiple(true, 3164);
        //panFileFilter.addToWhiteListMultiple(false, 20);
        // cand has only: "en", "de", "es"
        // susp has only: "en"
        panFileFilter.addLanguageToWhitelist("de", "es");

        maxtrixStoreLocation = System.getProperty("user.home");

    }

    public boolean checkIfInLanguageWhitelist(String language){
        if(!this.USE_LANGUAGE_WHITELISTING){
            return true;
        }
        return panFileFilter.checkIfLanguageWhitelisted(language);
    }
}
