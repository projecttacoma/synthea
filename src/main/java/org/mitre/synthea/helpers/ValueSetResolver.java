package org.mitre.synthea.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
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
  private String filePath;

  static final Pattern OID_REGEX = Pattern.compile("([0-9]+\\.)+[0-9]+");

  /**
   * Construct a new ValueSetResolver object.
   *
   * @param filePath absolute path to a Bundle of ValueSets as a JSON file
   */
  public ValueSetResolver(String filePath) {
    FhirContext ctx = FhirContext.forR4();
    this.jsonParser = new JSONParser();
    this.fhirParser = ctx.newJsonParser();
    this.filePath = filePath;
  }

  private Bundle parseValueSetBundle() throws FileNotFoundException, IOException, ParseException {
    JSONObject jsonBundle = (JSONObject) this.jsonParser.parse(new FileReader(this.filePath));
    String bundleString = jsonBundle.toString();
    Bundle bundle = fhirParser.parseResource(Bundle.class, bundleString);
    return bundle;
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
   * Get ValueSet resource that match an OID in the list of desired ones.
   * 
   * @param desiredOids List of OIDs to match against
   * @return List of ValueSet resources that match the desired OIDs
   * @throws ParseException when unable to parse ValueSet bundle into JSON
   * @throws IOException when unable to read in file
   * @throws FileNotFoundException when provided an invalid path to ValueSet Bundle
   * @throws RuntimeException when ValueSet is not found
   */
  public List<ValueSet> getValueSets(List<String> desiredOids)
      throws FileNotFoundException, IOException, ParseException {
    Bundle vsetBundle = this.parseValueSetBundle();
    List<ValueSet> matchingValueSets = new ArrayList<ValueSet>();

    // Check each identifier and add to list if matches
    for (String oid : desiredOids) {
      String desiredOid = this.matchOid(oid);

      boolean foundMatchingOid = false;
      for (BundleEntryComponent e : vsetBundle.getEntry()) {
        ValueSet vset = (ValueSet) e.getResource();
        String knownOid = this.matchOid(vset.getIdentifierFirstRep().getValue());

        if (knownOid != null && knownOid.equals(desiredOid)) {
          matchingValueSets.add(vset);
          foundMatchingOid = true;
          break;
        }
      }

      if (!foundMatchingOid) {
        throw new RuntimeException(String.format("Could not resolve ValueSet %s", oid));
      }
    }
    return matchingValueSets;
  }
}
