package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.world.agents.Payer;
import org.mitre.synthea.world.geography.Location;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.Terminology;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Terminology.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*", "com.helger.schematron.*", "org.hl7.*", "ca.uhn.fhir.*" })
public class CPCDSExporterTest {
  /**
   * Temporary folder for any exported files, guaranteed to be deleted at the end of the test.
   */
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCPCDSExport() throws Exception {
    PowerMockito.mockStatic(Terminology.class);
    PowerMockito.doNothing().when(Terminology.class, "loadValueSets", anySet());
    PowerMockito.when(Terminology.class, "getRandomCode", anyString())
        .thenReturn(new Code("mock_system", "mock_code", "mock_display"));
    TestHelper.exportOff();
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("exporter.cpcds.export", "true");
    Config.set("exporter.csv.folder_per_run", "false");
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());

    Payer.clear();
    Config.set("generate.payers.insurance_companies.default_file",
        "generic/payers/test_payers.csv");
    Payer.loadPayers(new Location(Generator.DEFAULT_STATE, null));

    int numberOfPeople = 10;
    Generator generator = new Generator(numberOfPeople);
    generator.options.overflow = false;
    for (int i = 0; i < numberOfPeople; i++) {
      generator.generatePerson(i);
    }
    // Adding post completion exports to generate organizations and providers CSV files
    Exporter.runPostCompletionExports(generator);

    // if we get here we at least had no exceptions

    File expectedExportFolder = tempOutputFolder.toPath().resolve("cpcds").toFile();

    assertTrue(expectedExportFolder.exists() && expectedExportFolder.isDirectory());

    int count = 0;
    for (File csvFile : expectedExportFolder.listFiles()) {
      if (!csvFile.getName().endsWith(".csv")) {
        continue;
      }

      String csvData = new String(Files.readAllBytes(csvFile.toPath()));

      // the CPCDS exporter doesn't use the SimpleCSV class to write the data,
      // so we can use it here for a level of validation
      SimpleCSV.parse(csvData);

      count++;
    }

    assertEquals("Expected 3 CSV files in the output directory, found " + count, 3, count);
  }
}
