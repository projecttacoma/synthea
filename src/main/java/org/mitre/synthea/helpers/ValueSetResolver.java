package org.mitre.synthea.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
  private Bundle vsetBundle;

  static final Pattern OID_REGEX = Pattern.compile("([0-9]+\\.)+[0-9]+");

  /**
   * Construct a new ValueSetResolver object.
   *
   * @param filePath absolute path to a Bundle of ValueSets as a JSON file
   * @throws ParseException
   * @throws IOException
   * @throws FileNotFoundException
   */
  public ValueSetResolver() throws FileNotFoundException, IOException, ParseException {
    FhirContext ctx = FhirContext.forR4();
    this.jsonParser = new JSONParser();
    this.fhirParser = ctx.newJsonParser();
    this.filePath = new File("").getAbsolutePath().concat("/src/main/resources/terminology/exm130/valuesets.json");
    this.vsetBundle = parseValueSetBundle();
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
