package org.mitre.synthea.world.concepts;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.json.simple.parser.ParseException;
import org.mitre.synthea.helpers.ValueSetResolver;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Class for performing operations on ValueSets
 */
public class Terminology {
  private static Map<String, ValueSet> valuesetLookup = new HashMap<String, ValueSet>();
  private static Map<String, List<Code>> codesLookup = new HashMap<String, List<Code>>();
  private static final Random RANDOM = new Random();

  /**
   * Seed the valuesetLookup map with ValueSet resources from the master bundle
   * 
   * @param urls the list of ValueSet urls to include in the map
   */
  public static void loadValueSets(Set<String> urls) {
    ValueSetResolver valuesetResolver;
    try {
      valuesetResolver = new ValueSetResolver();
      for (String url : urls) {
        ValueSet vset = valuesetResolver.getValueSet(url);
        if (vset == null) {
          throw new RuntimeException("Could not resolve ValueSet " + url);
        }
        valuesetLookup.put(url, vset);
        codesLookup.put(url, new ArrayList<Code>());

        // Iterate through codes and add to codesLookup list
        List<ConceptSetComponent> concepts = vset.getCompose().getInclude();
        for (ConceptSetComponent concept : concepts) {
          String system = concept.getSystem();
          for (ConceptReferenceComponent conceptRef : concept.getConcept()) {
            String display = conceptRef.getDisplay();
            Code code = new Code(system, conceptRef.getCode(), (display != null ? display : ""));
            codesLookup.get(url).add(code);
          }
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  /**
   * Select a random code from a given ValueSet
   * @param valuesetUrl the ValueSet url to pick a code from
   * @return the Code randomly selected from the list
   */
  public static Code getRandomCode(String valuesetUrl) {
    List<Code> codes = codesLookup.get(valuesetUrl);
    return codes.get(RANDOM.nextInt(codes.size()));
  }
}