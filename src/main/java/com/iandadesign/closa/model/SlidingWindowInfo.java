package com.iandadesign.closa.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This is a helper class for packing return values of
 * OntologyBasedSimilarityAnalysis.getWikiEntityStringsForSlidingWindow.
 *
 * @author Johannes Stegmüller on 2020/05/29/
 */
public class SlidingWindowInfo {
    private final String fileName;
    private final WeakHashMap<String, List<String>> filenameToEntities;
    private final int characterStartIndex;
    private final int characterEndIndex;
    private final int startSentence;
    private final int endSentence;
    private final boolean noEntitiesInWindow;

    public SlidingWindowInfo(String fileName,
                             List<String> entityIdsForWindow,
                             int characterStartIndex,
                             int characterEndIndex,
                             int startSentence,
                             int endSentence) {
        // Create a reusable map for comparison
        WeakHashMap <String, List<String>> filenameToEntities = new WeakHashMap<>();
        filenameToEntities.put(fileName, entityIdsForWindow);

        this.fileName = fileName;
        this.filenameToEntities = filenameToEntities;
        this.characterStartIndex = characterStartIndex;
        this.characterEndIndex = characterEndIndex;
        this.startSentence = startSentence;
        this.endSentence = endSentence;
        // Window without entities usually doesn't have to be processed in cosine similarity
        this.noEntitiesInWindow = entityIdsForWindow.size() <= 0;
    }

    public String getFileName() {
        return fileName;
    }
    public WeakHashMap<String, List<String>> getFilenameToEntities() {
        return filenameToEntities;
    }
    public int getCharacterStartIndex() {
        return characterStartIndex;
    }
    public int getCharacterEndIndex() {
        return characterEndIndex;
    }
    public int getStartSentence(){ return startSentence;}
    public int getEndSentence() { return endSentence; }

    public boolean isNoEntitiesInWindow() {
        return noEntitiesInWindow;
    }


    public void deinitialize(){
        if(filenameToEntities!=null) {
            filenameToEntities.clear();
        }
     }
}