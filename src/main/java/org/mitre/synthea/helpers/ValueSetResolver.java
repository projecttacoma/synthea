package org.mitre.synthea.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.ValueSet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * Helper class for resolving FHIR ValueSet resources from a JSON file
 */
public class ValueSetResolver {
  private JSONParser jsonParser;
  private IParser fhirParser;

  // Bundle containing valuesets for all measures
  private Bundle vsetBundle;

  static final Pattern OID_REGEX = Pattern.compile("([0-9]+\\.)+[0-9]+");

  /**
   * Construct a new ValueSetResolver object.
   */
  public ValueSetResolver() {
    FhirContext ctx = FhirContext.forR4();
    this.jsonParser = new JSONParser();
    this.fhirParser = ctx.newJsonParser();
    this.vsetBundle = new Bundle();
    this.vsetBundle.setType(BundleType.TRANSACTION);

    // If IO is thrown, then the walk on the terminology directory could not be performed
    try {
      initBundle();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Could not initialize ValueSet bundles."
        + "Ensure src/main/resources/terminology has proper contents");
    }
  }

  private void initBundle() throws IOException {
    String filePath = new File("").getAbsolutePath().concat("/src/main/resources/terminology");
    List<BundleEntryComponent> entries = new ArrayList<BundleEntryComponent>();
    Stream<Path> paths = Files.walk(Paths.get(filePath));
    paths.filter(p -> {
      return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json");
    }).forEach(f -> {
      JSONObject jsonBundle;
      try {
        // Add this Bundle's entry to the master Bundle
        jsonBundle = (JSONObject) this.jsonParser.parse(new FileReader(f.toFile()));
        String bundleString = jsonBundle.toString();
        Bundle currentBundle = fhirParser.parseResource(Bundle.class, bundleString);
        entries.addAll(currentBundle.getEntry());
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        throw new RuntimeException("File " + f.toString() + " could not be found");
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException("Could not read file " + f.toString());
      } catch (ParseException e) {
        e.printStackTrace();
        throw new RuntimeException("Could not parse " + f.toString() + " as JSON");
      }
    });
    paths.close();
    this.vsetBundle.setEntry(entries);
  }

  private String matchOid(String input) {
    if (input == null) {
      return null;
    }

    Matcher m = OID_REGEX.matcher(input);

    if (m.find()) {
      return m.group(0);
    }

    return null;
  }

  /**
   * Retrieve a FHIR ValueSet from the master Bundle.
   * @param oid the desired OID to retreive
   * @return the corresponding ValueSet, null if it does not exist
   */
  public ValueSet getValueSet(String oid) {
    String desiredOid = this.matchOid(oid);
    for (BundleEntryComponent e : vsetBundle.getEntry()) {
      ValueSet vset = (ValueSet) e.getResource();
      String knownOid = this.matchOid(vset.getIdentifierFirstRep().getValue());

      if (knownOid != null && knownOid.equals(desiredOid)) {
        return vset;
      }
    }
    return null;
  }
}
