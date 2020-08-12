package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.Terminology;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Terminology.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*", "com.helger.schematron.*", "org.hl7.*", "ca.uhn.fhir.*", "org.apache.commons.*", "com.fasterxml.*" })
public class TextExporterTest {
  /**
   * Temporary folder for any exported files, guaranteed to be deleted at the end of the test.
   */
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Test
  public void testTextExport() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    
    int numberOfPeople = 10;
    Generator generator = new Generator(numberOfPeople);
    generator.options.overflow = false;
    for (int i = 0; i < numberOfPeople; i++) {
      TestHelper.exportOff();
      Person person = generator.generatePerson(i);
      Config.set("exporter.text.export", "true");
      Config.set("exporter.text.per_encounter_export", "true");
      Exporter.export(person, System.currentTimeMillis());
    }

    // if we get here we at least had no exceptions
    
    File expectedExportFolder = tempOutputFolder.toPath().resolve("text").toFile();
    
    assertTrue(expectedExportFolder.exists() && expectedExportFolder.isDirectory());
    
    int count = 0;
    for (File txtFile : expectedExportFolder.listFiles()) {
      if (!txtFile.getName().endsWith(".txt")) {
        continue;
      }
      
      count++;
    }
    
    assertEquals("Expected " + numberOfPeople + " files in the output directory, found " + count, 
        numberOfPeople, count);
  }
}
