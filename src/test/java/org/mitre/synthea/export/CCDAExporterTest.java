package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.mdht.uml.cda.util.BasicValidationHandler;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.Terminology;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Uses Model Driven Health Tools (MDHT) to validate exported CCDA R2.1.
 * https://github.com/mdht/mdht-models
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Terminology.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*", "com.helger.schematron.*", "org.hl7.*", "ca.uhn.fhir.*", "org.apache.commons.*" })
public class CCDAExporterTest {
  
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Test
  public void testCCDAExport() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("exporter.baseDirectory", tempFolder.newFolder().toString());
    CDAUtil.loadPackages();
    List<String> validationErrors = new ArrayList<String>();

    int numberOfPeople = 10;
    Generator generator = new Generator(numberOfPeople);
    generator.options.overflow = false;
    for (int i = 0; i < numberOfPeople; i++) {
      int x = validationErrors.size();
      TestHelper.exportOff();
      Person person = generator.generatePerson(i);
      Config.set("exporter.ccda.export", "true");
      String ccdaXml = CCDAExporter.export(person, System.currentTimeMillis());
      InputStream inputStream = IOUtils.toInputStream(ccdaXml, "UTF-8");
      try {
        CDAUtil.load(inputStream, new BasicValidationHandler() {
          public void handleError(Diagnostic diagnostic) {
            System.out.println("ERROR: " + diagnostic.getMessage());
            validationErrors.add(diagnostic.getMessage());
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
        validationErrors.add(e.getMessage());
      }
      int y = validationErrors.size();
      if (x != y) {
        Exporter.export(person, System.currentTimeMillis());
      }
    }
    assertEquals(0, validationErrors.size());
  }
}
