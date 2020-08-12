package org.mitre.synthea;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.Terminology;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

public abstract class TestHelper {

  /**
   * Returns a test fixture Module by filename.
   * @param filename The filename of the test fixture Module.
   * @return A Module.
   * @throws Exception On errors.
   */
  public static Module getFixture(String filename) throws Exception {
    Path modulesFolder = Paths.get("generic");
    Path module = modulesFolder.resolve(filename);
    return Module.loadFile(module, modulesFolder, null);
  }

  /**
   * Load the test.properties file.
   * @throws Exception on configuration loading errors.
   */
  public static void loadTestProperties() throws Exception {
    URI uri = Config.class.getResource("/test.properties").toURI();
    File file = new File(uri);
    Config.load(file);
  }

  /**
   * Helper method to disable export of all data formats and database output.
   * Ensures that unit tests do not pollute the output folders.
   */
  public static void exportOff() {
    Config.set("generate.database_type", "none"); // ensure we don't write to a file-based DB
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
    Config.set("exporter.cpcds.export", "false");
    Config.set("exporter.cdw.export", "false");
    Config.set("exporter.hospital.fhir_stu3.export", "false");
    Config.set("exporter.hospital.fhir_dstu2.export", "false");
    Config.set("exporter.hospital.fhir.export", "false");
    Config.set("exporter.practitioner.fhir_stu3.export", "false");
    Config.set("exporter.practitioner.fhir_dstu2.export", "false");
    Config.set("exporter.practitioner.fhir.export", "false");
    Config.set("exporter.cost_access_outcomes_report", "false");
  }

  public static long timestamp(int year, int month, int day, int hr, int min, int sec) {
    return LocalDateTime.of(year, month, day, hr, min, sec).toInstant(ZoneOffset.UTC)
        .toEpochMilli();
  }

  public static void mockTerminology() throws Exception {
    PowerMockito.mockStatic(Terminology.class);
    PowerMockito.doNothing().when(Terminology.class, "loadValueSets", anySet());
    PowerMockito.when(Terminology.class, "getRandomCode", anyString())
        .thenReturn(new Code("http://snomed.info/sct", "mock_code", "mock_display"));
  }

}
