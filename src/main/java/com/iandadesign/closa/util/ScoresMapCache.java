package com.iandadesign.closa.util;

import java.io.*;
import java.util.Map;

public class ScoresMapCache {

    public void serializeScoresMap(String filekey, Map<Long, Map<Long, Float>> scoresMap) throws IOException {
        // Serialization
        FileOutputStream fileOutputStream = new FileOutputStream(filekey);
        ObjectOutputStream objectOutputStream  = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(scoresMap);
        objectOutputStream.flush();
        objectOutputStream.close();
    }
    public Map<Long, Map<Long, Float>> deserializeScoresMap(String filekey) throws IOException, ClassNotFoundException {
        File tempFile = new File(filekey);
        boolean exists = tempFile.exists();
        if(!exists) return null;

        FileInputStream fileInputStream = new FileInputStream(filekey);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Map<Long, Map<Long, Float>>  scoresMapDes = (Map<Long, Map<Long, Float>>) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return scoresMapDes;
    }
    public String generateFileKey(String language, String basePath, int fragmentSize, int fragmentIncrement, boolean absoluteScoring, boolean filePrefiltering, int filterLimit,boolean plagsizedFragments, int suspfileselectionoffset, boolean sortsuspbysize, boolean enchanched_analysis, String prefilter){
        String addendum="";

        if(enchanched_analysis){
            absoluteScoring = false; // in misconfiguration case, set absolute scoring to false
            addendum += "enhanched";
        }
        addendum += "float";
        if(!prefilter.equals("NONE")){
            addendum += prefilter;
        }
        if(!filePrefiltering){
            filterLimit = 0;
        }
        String generatedKey = basePath+"/"+language+"/scoresmap"+fragmentSize+"_"+fragmentIncrement+"_"+absoluteScoring+"_"+filePrefiltering+"_"+filterLimit+"_"+suspfileselectionoffset+"_"+plagsizedFragments+"_"+sortsuspbysize+addendum+".ser";
        File tempFile = new File(generatedKey).getParentFile();
        if(!tempFile.exists()){
            boolean dirCreated = tempFile.mkdir();
        }
        return generatedKey;
    }

}
