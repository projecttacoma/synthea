package org.mitre.synthea;

import ca.uhn.fhir.context.FhirContext;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import ca.uhn.fhir.parser.IParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.ValueSetResolver;

public abstract class TestHelper {

  public static final String SNOMED_URI = "http://snomed.info/sct";
  public static final String LOINC_URI = "http://loinc.org";
  public static final String SNOMED_OID = "2.16.840.1.113883.6.96";
  public static final String LOINC_OID = "2.16.840.1.113883.6.1";
  private static FhirContext dstu2FhirContext;
  private static FhirContext stu3FhirContext;
  private static FhirContext r4FhirContext;

  /**
   * Returns a test fixture Module by filename.
   *
   * @param filename The filename of the test fixture Module.
   * @return A Module.
   * @throws Exception On errors.
   */
  public static Module getFixture(String filename) throws Exception {
    Path modulesFolder = Paths.get("generic");
    Path module = modulesFolder.resolve(filename);
    return Module.loadFile(module, false, null, false);
  }

  /**
   * Load the test.properties file.
   *
   * @throws Exception on configuration loading errors.
   */
  public static void loadTestProperties() throws Exception {
    URI uri = Config.class.getResource("/test.properties").toURI();
    File file = new File(uri);
    Config.load(file);
  }

  public static WireMockConfiguration wiremockOptions() {
    return WireMockConfiguration.options().port(5566);
  }

  /**
   * Check whether the <code>synthea.test.httpRecording</code> property is set to enable HTTP
   * recording, for tests with HTTP mocking.
   *
   * @return true if HTTP recording is enabled
   */
  public static boolean isHttpRecordingEnabled() {
    String recordingProperty = System.getProperty("synthea.test.httpRecordingEnabled");
    return recordingProperty != null && recordingProperty.equals("true");
  }

  /**
   * Return the configured URL for recording terminology HTTP responses.
   *
   * @return the configured terminology service URL
   */
  public static String getTxRecordingSource() {
    String recordingSource = System.getProperty("synthea.test.txRecordingSource");
    if (recordingSource == null) {
      throw new RuntimeException("No terminology service recording source configured");
    }
    return recordingSource;
  }

  /**
   * Returns a WireMock response builder representing a response from a FHIR server.
   *
   * @return a ResponseDefinitionBuilder object
   */
  public static ResponseDefinitionBuilder fhirResponse() {
    return WireMock.aResponse().withHeader("Content-Type", "application/fhir+json");
  }

  /**
   * Helper method to disable export of all data formats and database output.
   * Ensures that unit tests do not pollute the output folders.
   */
  public static void exportOff() {
    Config.set("exporter.use_uuid_filenames", "false");
    Config.set("exporter.fhir.use_shr_extensions", "false");
    Config.set("exporter.subfolders_by_id_substring", "false");
    Config.set("exporter.ccda.export", "false");
    Config.set("exporter.fhir_stu3.export", "false");
    Config.set("exporter.fhir_dstu2.export", "false");
    Config.set("exporter.fhir.export", "false");
    Config.set("exporter.fhir.transaction_bundle", "false");
    Config.set("exporter.text.export", "false");
    Config.set("exporter.text.per_encounter_export", "false");
    Config.set("exporter.csv.export", "false");
    Config.set("exporter.split_records", "false");
    Config.set("exporter.split_records.duplicate_data", "false");
    Config.set("exporter.symptoms.csv.export", "false");
    Config.set("exporter.symptoms.text.export", "false");
    Config.set("exporter.cpcds.export", "false");
    Config.set("exporter.cdw.export", "false");
    Config.set("exporter.hospital.fhir_stu3.export", "false");
    Config.set("exporter.hospital.fhir_dstu2.export", "false");
    Config.set("exporter.hospital.fhir.export", "false");
    Config.set("exporter.practitioner.fhir_stu3.export", "false");
    Config.set("exporter.practitioner.fhir_dstu2.export", "false");
    Config.set("exporter.practitioner.fhir.export", "false");
    Config.set("exporter.cost_access_outcomes_report", "false");
    Config.set("generate.terminology_service_url", "");
  }

  public static long timestamp(int year, int month, int day, int hr, int min, int sec) {
    return LocalDateTime.of(year, month, day, hr, min, sec).toInstant(ZoneOffset.UTC)
        .toEpochMilli();
  }

  /**
   * generateValuesetTempfiles is a static method that is intended to be called at the start of a
   * Synthea test run. It parses the modules in the synthea module directory to get a unique list
   * of valuesets that are required, and generates a bundle of mock valueset objects, one for each
   * valueset URL, each of which contains one code.
   * @throws IOException if the tempfile can't be created, or if a module can't be read
   */
  public static void generateValuesetTempfiles() throws IOException {
    Path path = FileSystems.getDefault()
                          .getPath("src", "main", "resources", "terminology")
                          .toAbsolutePath();
    Path file = Files.createTempFile(path, "TEMP_VS_FILE", ".json");

    // This shutdown hook will delete the tempfile at the end of the test run
    // or if the test is exited early (via Ctrl+c or other methods)
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          Files.delete(file);
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    });
    Path modPath = FileSystems.getDefault()
                              .getPath("src", "main", "resources", "modules")
                              .toAbsolutePath();
    Bundle valueSetBundle = new Bundle();
    valueSetBundle.setId("TestValueSetBundle");
    Files.list(modPath)
        .filter(p -> !p.toFile().isDirectory())
        .filter(p -> p.getFileName().toString().endsWith(".json")).flatMap(p -> getValueSetUrls(p))
        .collect(Collectors.toSet()).forEach((vsUrl) -> {
          valueSetBundle.addEntry(generateValuesetBundleEntry(vsUrl));
        });
    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();
    FileWriter vsFileWriter = new FileWriter(file.toFile());
    parser.encodeResourceToWriter(valueSetBundle, vsFileWriter);
  }

  /**
   * generateValuesetBundleEntry encapsulates the creation of the valueset bundle entry
   * from the valueset URL.
   * @param vsUrl String representing the URL of the valueset needing to be mocked
   * @return a {@link BundleEntryComponent} containing a {@link ValueSet} object, to be added to
   *         the bundle for the tempfile
   */
  private static BundleEntryComponent generateValuesetBundleEntry(String vsUrl) {
    BundleEntryComponent bec = new BundleEntryComponent();
    ValueSet valuesetObj = new ValueSet();
    ValueSetComposeComponent vscc = new ValueSetComposeComponent();
    ConceptSetComponent csc = new ConceptSetComponent();
    ConceptReferenceComponent crc = new ConceptReferenceComponent();
    crc.setCode("mock_code").setDisplay("mock_value");
    csc.setSystem("http://snomed.info/sct").addConcept(crc);
    vscc.addInclude(csc);
    valuesetObj.setCompose(vscc).setUrl(vsUrl);
    Identifier i = new Identifier();
    i.setSystem("urn:ietf:rfc:3986");
    String oid = ValueSetResolver.matchOid(vsUrl);
    i.setValue(oid);
    valuesetObj.setId(oid);
    valuesetObj.addIdentifier(i).setName("TEST VALUESET");
    bec.setResource(valuesetObj);
    return bec;
  }

  /**
   * getValueSetUrls takes in a path to a file, and returns a stream of valueset
   * URLs from that file, which can be operated on further by a stream processing chain.
   *
   * @param path a {@link java.nio.file.Path} object that points to a GMF JSON file
   * @return A <code>Stream</code> of <code>String</code>s representing the
   *         valueset URLs contained in the file (not deduplicated)
   */
  private static Stream<String> getValueSetUrls(Path path) {
    try {
      Pattern valueSetPattern = Pattern.compile("\"url\": ?\"(.+)\",");
      Matcher matcher = valueSetPattern.matcher(Files.readString(path));
      List<String> vs = new ArrayList<String>();
      while (matcher.find()) {
        vs.add(matcher.group(1));
      }
      return vs.stream();
    } catch (IOException e) {
      return null;
    }
  }
}
