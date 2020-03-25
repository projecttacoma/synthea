package org.mitre.synthea.helpers;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/*
 * Helper class for resolving FHIR ValueSet resources from a JSON file
 */
public class ValueSetResolver {
  private JSONParser jsonParser;
  private IParser fhirParser;
  private String filePath;

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

  /**
   * Get ValueSet resource that match an OID in the list of desired ones
   * 
   * @param desiredOIDs List of OIDs to match against
   * @return List of ValueSet resources that match the desired OIDs
   */
  public List<ValueSet> getValueSets(List<String> desiredOIDs) {
    try {
      Bundle vsetBundle = this.parseValueSetBundle();
      List<ValueSet> matchingValueSets = new ArrayList<ValueSet>();
      
      // Check each identifier and add to list if matches
      for (BundleEntryComponent e : vsetBundle.getEntry()) {
        ValueSet vset = (ValueSet) e.getResource();
        String oid = vset.getIdentifierFirstRep().getValue();
        if (oid != null && desiredOIDs.contains(oid)) {
          matchingValueSets.add(vset);
        }
      }

      return matchingValueSets;
    } catch (IOException | ParseException e) {
      e.printStackTrace();
      return null;
    }
  }
}
