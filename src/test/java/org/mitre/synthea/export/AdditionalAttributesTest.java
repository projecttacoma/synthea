package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.Test;

/**
 * Unit tests for adding additional attributes in R4 export.
 */
public class AdditionalAttributesTest {
  @Test
  public void testAdditionalAttributesExport() {
    JsonObject additionalAttributes = parseJsonFixture(
        "src/test/resources/export/additional_attributes_encounter.json");
    Encounter encounter = new Encounter();
    encounter = (Encounter)FhirR4.setAdditionalAttributes(encounter, additionalAttributes);
    CodeableConcept priority = encounter.getPriority();
    assertNotNull(priority);
    List<Coding> coding = priority.getCoding();
    assertNotNull(coding);
    Coding firstCoding = coding.get(0);
    assertNotNull(firstCoding);
    assertEquals(firstCoding.getSystem(), "http://snomed.info/sct");
    assertEquals(firstCoding.getCode(), "260385009");
    assertEquals(firstCoding.getDisplay(), "Negative");
    assertEquals(firstCoding.getUserSelected(), true);
    assertEquals(priority.getText(), "THIS IS A TEST VALUE FOR PRIORITY");
  }

  @Test
  public void testInvalidAdditionalAttributesExport() {
    JsonObject additionalAttributes = parseJsonFixture(
        "src/test/resources/export/additional_attributes_encounter_invalid.json");
    Encounter encounter = new Encounter();
    Encounter encounter2 = (Encounter)FhirR4.setAdditionalAttributes(
        encounter, additionalAttributes);
    assertTrue(encounter2.equals(encounter));
  }

  private JsonObject parseJsonFixture(String filePath) {
    Path fixturePath = Paths.get(filePath);
    String fixtureJson = "";
    try {
      byte[] encoded = Files.readAllBytes(fixturePath);
      fixtureJson = new String(encoded);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    JsonParser parser = new JsonParser();
    return parser.parse(fixtureJson).getAsJsonObject();
  }
}