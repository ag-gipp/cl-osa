package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.model.WikidataEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;


public class WikidataDisambiguaterTest {

    @Test
    public void testDisambiguateBySmallestId() {
        WikidataEntity tree1 = new WikidataEntity("Q10884", "tree", Collections.singletonMap("en", "tree"));
        WikidataEntity tree2 = new WikidataEntity("Q272735", "tree", Collections.singletonMap("en", "tree"));

        WikidataEntity disambiguatedEntity = WikidataDisambiguator.disambiguateBySmallestId(Arrays.asList(tree1, tree2));

        Assertions.assertTrue(disambiguatedEntity.equals(tree1));
    }

    @Test
    public void testAncestorCountDisambiguate() {
        WikidataEntity tree1 = new WikidataEntity("Q10884", "tree", Collections.singletonMap("en", "tree"));
        WikidataEntity tree2 = new WikidataEntity("Q272735", "tree", Collections.singletonMap("en", "tree"));

        WikidataEntity disambiguatedEntity = WikidataDisambiguator.disambiguateByAncestorCount(Arrays.asList(tree1, tree2),
                "A tree is a plant", "en");

        Assertions.assertTrue(disambiguatedEntity.equals(tree1));
    }


    @Test
    public void testDisambiguateByDescription() {
        WikidataEntity tree1 = WikidataDumpUtil.getEntityById("Q10884"); // perennial woody plant
        WikidataEntity tree2 = WikidataDumpUtil.getEntityById("Q272735"); // data structure

        WikidataEntity disambiguatedEntity = WikidataDisambiguator.disambiguateByDescription(Arrays.asList(tree1, tree2),
                "A tree is a plant", "en");

        Assertions.assertTrue(disambiguatedEntity.equals(tree1));

        Assertions.assertTrue(WikidataDisambiguator.disambiguateByDescription(Arrays.asList(tree1, tree2),
                "A tree is a graph", "en")
                .equals(tree2));
    }
}
