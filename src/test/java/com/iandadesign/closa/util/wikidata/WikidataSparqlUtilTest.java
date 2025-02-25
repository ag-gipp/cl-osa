package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.model.WikidataEntity;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static com.iandadesign.closa.util.wikidata.WikidataSparqlUtil.*;

public class WikidataSparqlUtilTest {

    @Test
    public void testGetEntityById() {
        WikidataEntity tree = getEntityById("Q10884");

        Assertions.assertEquals("tree", tree.getLabel());
    }


    @Test
    public void testGetEntityLabelById() {
        System.out.println(getEntityLabelById("Q10884", "en"));
    }

    @Test
    public void getEntitiesByLabel() {
        List<WikidataEntity> entities = WikidataSparqlUtil.getEntitiesByLabel("tree", "en");

        Assertions.assertTrue(entities.contains(new WikidataEntity("Q10884", "tree")));
        Assertions.assertTrue(entities.contains(new WikidataEntity("Q272735", "tree")));

        System.out.println(WikidataSparqlUtil.getEntitiesByLabel("German", "en"));

        System.out.println(WikidataSparqlUtil.getEntitiesByLabel("Auschwitz", "en"));
    }


    @Test
    public void getAllAncestors() {
        WikidataEntity tree = new WikidataEntity("Q10884");

        List<WikidataEntity> allAncestors = WikidataSparqlUtil.getAllAncestors(tree);

        Assertions.assertTrue(allAncestors.contains(new WikidataEntity("Q35120", "entity")));
        Assertions.assertTrue(allAncestors.contains(new WikidataEntity("Q756", "plant")));

    }


    @Test
    public void testGetPropertyValue() {
        String countryProperty = "P17";
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");
        WikidataEntity peoplesRepublicOfChina = new WikidataEntity("Q148");

        WikidataEntity tree = new WikidataEntity("Q10884");
        String subclassOfProperty = "P279";

        if (isHumanLanguage(chineseLanguage)) {
            List<WikidataEntity> propertyValues = getProperty(chineseLanguage, countryProperty);
            Assertions.assertTrue(propertyValues.contains(peoplesRepublicOfChina));

            List<WikidataEntity> nonExistantPropertyValues = getProperty(chineseLanguage, "P1");
            Assertions.assertTrue(nonExistantPropertyValues.isEmpty());

            List<WikidataEntity> multiplePropertyValues = getProperty(tree, subclassOfProperty);
        } else {
            Assertions.fail();
        }
    }


    @Test
    public void testGetPairwiseDistance() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");
        WikidataEntity carnivorousPlant = new WikidataEntity("Q18240", "carnivorous plant");

        Assertions.assertEquals(distance(tree, carnivorousPlant), 3);
    }

    @Test
    public void testGetRootDistance() {
        WikidataEntity entity = new WikidataEntity("Q35120", "entity");
        long depthEntity = getRootDistance(entity);
        Assertions.assertTrue(depthEntity == 0);

        WikidataEntity object = new WikidataEntity("Q488383", "object");
        long depthObject = getRootDistance(object);
        Assertions.assertTrue(depthObject == 1);

        WikidataEntity perceptibleObject = new WikidataEntity("Q337060", "perceptible object");
        long depthPerceptibleObject = getRootDistance(perceptibleObject);
        Assertions.assertTrue(depthPerceptibleObject == 2);

        WikidataEntity scotus = new WikidataEntity("Q11201");

        System.out.println(getRootDistance(scotus));
        WikidataEntity crisis = new WikidataEntity("Q381072");
    }


    @Test
    public void testGetDistanceToAncestor() {
        WikidataEntity tree = new WikidataEntity("Q10884");
        WikidataEntity perennialPlant = new WikidataEntity("Q157957");
        WikidataEntity organism = new WikidataEntity("Q7239");

        Assertions.assertTrue(getDistanceToAncestor(tree, perennialPlant) == 1);
        Assertions.assertTrue(WikidataDumpUtil.getDistanceToAncestor(tree, organism) == 3);
    }

    @Test
    public void testSubclassOf() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");

        List<WikidataEntity> subclassOf = subclassOf(tree);

        WikidataEntity perennialPlant = new WikidataEntity("Q157957", "perennial plant");
        WikidataEntity woodyPlant = new WikidataEntity("Q757163", "woody plant");

        Assertions.assertTrue(subclassOf.contains(perennialPlant));
        Assertions.assertTrue(subclassOf.contains(woodyPlant));
    }


    @Test
    public void testInstanceOf() {
        WikidataEntity tomHanks = new WikidataEntity("Q2263", "Tom Hanks");

        List<WikidataEntity> instanceOf = instanceOf(tomHanks);

        Assertions.assertTrue(instanceOf.contains(new WikidataEntity("Q5", "human")));

        WikidataEntity auschwitz = new WikidataEntity("Q7342", "Oświęcim");

        List<WikidataEntity> instanceOf2 = instanceOf(auschwitz);

        System.out.println(instanceOf2);

        instanceOf2.forEach(classEntity -> System.out.println(WikidataSparqlUtil.getAllAncestors(classEntity)));
    }

    @Test
    public void testGetSubclasses() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");

        List<WikidataEntity> subclasses = getSubclasses(tree);

        Assertions.assertTrue(subclasses.contains(new WikidataEntity("Q47128", "Christmas tree")));

    }


    @Test
    public void testGetInstances() {
        WikidataEntity fictionalDuck = new WikidataEntity("Q3247351", "fictional duck");

        List<WikidataEntity> instances = getInstances(fictionalDuck);

        Assertions.assertTrue(instances.contains(new WikidataEntity("Q6550", "Donald Duck")));
    }


    @Test
    public void testGetMostSpecificParentEntity() {
        WikidataEntity mosque = new WikidataEntity("Q32815");
        WikidataEntity synagogue = new WikidataEntity("Q34627");

        WikidataEntity mostSpecificParentEntity = getMostSpecificParentEntity(mosque, synagogue);

        WikidataEntity temple = new WikidataEntity("Q44539");
        System.out.println(mostSpecificParentEntity);

        Assertions.assertEquals(mostSpecificParentEntity, temple);
    }

    @Test
    public void testGetMostSpecificParentEntity2() {
        WikidataEntity lawnMower = new WikidataEntity("Q260521");
        WikidataEntity snowBlower = new WikidataEntity("Q1351693");
        WikidataEntity chainsaw = new WikidataEntity("Q208040");

        WikidataEntity tool = new WikidataEntity("Q39546");

        Assertions.assertTrue(getMostSpecificParentEntity(getMostSpecificParentEntity(lawnMower, snowBlower), chainsaw).equals(tool));

        // WikidataEntity bicycle = new WikidataEntity("Q11442");
        WikidataEntity ski = new WikidataEntity("Q172226");

        System.out.println(getMostSpecificParentEntity(tool, ski).equals(tool));
    }

    @Test
    public void testIsNumber() {
        WikidataEntity one = new WikidataEntity("Q199", "1");
        WikidataEntity pi = new WikidataEntity("Q167", "pi");
        WikidataEntity three = new WikidataEntity("Q201", "3");
        WikidataEntity threeDigit = new WikidataEntity("Q3431160", "3");

        Assertions.assertTrue(isNumber(one));
        Assertions.assertTrue(isNumber(three));
        Assertions.assertFalse(isNumber(threeDigit));
        Assertions.assertTrue(isNumber(pi));
    }


    @Test
    public void testIsNaturalNumber() {
        WikidataEntity one = new WikidataEntity("Q199", "1");
        WikidataEntity pi = new WikidataEntity("Q167", "pi");

        Assertions.assertTrue(isNaturalNumber(one));
        Assertions.assertFalse(isNaturalNumber(pi));
    }


    @Test
    public void testIsCreativeWork() {
        WikidataEntity theMinisters = new WikidataEntity("Q7751572", "The Ministers");
        WikidataEntity isna = new WikidataEntity("Q1672387", "ISNA");
        WikidataEntity starWars = new WikidataEntity("Q462");

        Assertions.assertTrue(isCreativeWork(theMinisters));
        Assertions.assertFalse(isCreativeWork(isna));
        Assertions.assertTrue(isCreativeWork(starWars));
    }

    @Test
    public void testIsGene() {
        WikidataEntity hi = new WikidataEntity("Q29724302", "hi");

        Assertions.assertTrue(isGene(hi));
    }

    @Test
    public void testIsTransitiveInstanceOf() {
        WikidataEntity pi = new WikidataEntity("Q167", "pi");
        WikidataEntity number = new WikidataEntity("Q11563", "number");
        WikidataEntity creativeWork = new WikidataEntity("Q17537576", "creative work");

        Assertions.assertTrue(isTransitiveInstanceOf(pi, number));
        Assertions.assertFalse(isTransitiveInstanceOf(pi, creativeWork));
    }

    @Test
    public void testIsHuman() {
        WikidataEntity tomHanks = new WikidataEntity("Q2263", "Tom Hanks");

        Assertions.assertTrue(isHuman(tomHanks));
    }

    @Test
    public void testIsLocation() {
        WikidataEntity auschwitz = new WikidataEntity("Q7342", "Oświęcim");

        Assertions.assertTrue(isLocation(auschwitz));
    }

    @Test
    public void testIsOrganization() {
        WikidataEntity apple = new WikidataEntity("Q312", "Apple Inc.");

        Assertions.assertTrue(isOrganization(apple));
    }

    @Test
    public void testIsHumanLanguage() {
        WikidataEntity chineseLanguage = new WikidataEntity("Q7850");

        Assertions.assertTrue(isHumanLanguage(chineseLanguage));
    }

    @Test
    public void testIsInstance() {
        // should be an instance of female given name
        Assertions.assertTrue(isInstance(new WikidataEntity("Q1682564", "Jane")));
    }

    @Test
    public void testIsClass() {
        // should be a class
        Assertions.assertTrue(isClass(new WikidataEntity("Q11879590", "female given name")));
    }


    @Test
    public void testQuery() {
        String queryString =
                "SELECT ?lcs ?lcsLabel WHERE {\n" +
                        "    ?lcs ^wdt:P279* wd:Q32815, wd:Q34627, wd:Q16970, wd:Q16560 .\n" +
                        "    filter not exists {\n" +
                        "    ?sublcs ^wdt:P279* wd:Q32815, wd:Q34627, wd:Q16970, wd:Q16560 ;\n" +
                        "          wdt:P279 ?lcs . }\n" +
                        "    SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "  }";

        sendQuery(queryString);
    }


    @Test
    public void testMultipleQueries() {
        String queryString =
                "SELECT ?computer ?computerLabel WHERE {\n" +
                        "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\". }\n" +
                        "  ?computer wdt:P279 wd:Q68.\n" +
                        "}\n" +
                        "LIMIT 100";

        IntStream.range(1, 100).forEach(integer -> {
            sendQuery(queryString);
        });
    }


    @Test
    public void testErrorReport() {
        String path = "src/test/resources/com/iandadesign/closa/evaluation/error-reports";

        try {
            if (Files.notExists(Paths.get(path))) {
                Files.createDirectories(Paths.get(path));
            }
            String fileName = LocalDateTime.now().toString() + ".txt";

            Files.createFile(Paths.get(path, fileName));
            FileUtils.writeStringToFile(new File(path, fileName), "test", StandardCharsets.UTF_8);
            System.out.println("Error report written.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}