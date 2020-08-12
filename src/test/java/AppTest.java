import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.concepts.Terminology;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Terminology.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.*", "com.helger.schematron.*", "org.hl7.*", "ca.uhn.fhir.*", "java.io.*", "java.util.*" })
public class AppTest {
  private static String testStateDefault;
  private static String testTownDefault;
  private static String testStateAlternative;
  private static String testTownAlternative;

  /**
   * Configure settings across these tests.
   * @throws Exception on test configuration loading errors.
   */
  @BeforeClass
  public static void testSetup() throws Exception {
    TestHelper.loadTestProperties();
    testStateDefault = Config.get("test_state.default", "Massachusetts");
    testTownDefault = Config.get("test_town.default", "Bedford");
    testStateAlternative = Config.get("test_state.alternative", "Utah");
    testTownAlternative = Config.get("test_town.alternative", "Salt Lake City");
    Generator.DEFAULT_STATE = testStateDefault;
  }

  @Test
  public void testApp() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    String[] args = {"-s", "0", "-p", "3", testStateDefault, testTownDefault};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Population:"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("Location:"));
    Assert.assertTrue(output.contains("alive=3"));
    Assert.assertTrue(output.contains("dead="));
    String locationString = "Location: " + testTownDefault + ", " + testStateDefault;
    Assert.assertTrue(output.contains(locationString));
    System.setOut(original);
  }

  @Test
  public void testAppWithGender() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    String[] args = {"-s", "0", "-p", "4", "-g", "M"};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Gender: M"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("alive=4"));
    Assert.assertTrue(output.contains("dead="));
    Assert.assertFalse(output.contains("y/o F"));
    Assert.assertTrue(output.contains("Location: " + Generator.DEFAULT_STATE));
    System.setOut(original);
  }

  @Test
  public void testAppWithAges() throws Exception {
    TestHelper.mockTerminology();
    String[] args = {"-s", "0", "-p", "3", "-a", "30-39"};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("alive=3"));
    Assert.assertTrue(output.contains("Location: " + Generator.DEFAULT_STATE));
    String regex = "(.\n)*(3[0-9] y/o)(.\n)*";
    Assert.assertTrue(Pattern.compile(regex).matcher(output).find());
    regex = "(.\n)*(\\(([0-9]|[0-2][0-9]|[4-9][0-9]) y/o)(.\n)*";
    Assert.assertFalse(output.matches(regex));
    System.setOut(original);
  }


  @Test
  public void testAppWithDifferentLocation() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    String[] args = {"-s", "0", "-p", "3", testStateAlternative, testTownAlternative};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("alive=3"));
    String locationString = "Location: " + testTownAlternative + ", " + testStateAlternative;
    Assert.assertTrue(output.contains(locationString));
    System.setOut(original);
  }

  @Test
  public void testAppWithOverflow() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    String[] args = {"-s", "1", "-p", "3", "-o", "false"};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Seed:"));
    String regex = "\\{alive=(\\d+), dead=(\\d+)\\}";
    Matcher matches = Pattern.compile(regex).matcher(output);
    Assert.assertTrue(matches.find());
    int alive = Integer.parseInt(matches.group(1));
    int dead = Integer.parseInt(matches.group(2));
    Assert.assertEquals(alive + dead, 3);
    System.setOut(original);
  }

  @Test
  public void testAppWithModuleFilter() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    Config.set("test_key", "pre-test value");
    String[] args = {"-s", "0", "-p", "0", "-m", "copd" + File.pathSeparator + "allerg*"};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("Modules:"));
    Assert.assertTrue(output.contains("COPD Module"));
    Assert.assertTrue(output.contains("Allergic"));
    Assert.assertTrue(output.contains("Allergies"));
    Assert.assertFalse(output.contains("asthma"));
    System.setOut(original);
  }

  @Test
  public void testAppWithConfigSetting() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    Config.set("test_key", "pre-test value");
    String[] args = {"-s", "0", "-p", "0",
        "--test_key", "changed value", "--exporter.fhir.export=true"};
    App.main(args);
    
    Assert.assertEquals("changed value", Config.get("test_key"));
    Assert.assertEquals("true", Config.get("exporter.fhir.export"));
  }
  
  @Test
  public void testAppWithLocalConfigFile() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    Config.set("test.bar", "42");
    String[] args = {"-s", "0", "-p", "0",
        "-c", "src/test/resources/test2.properties"};
    App.main(args);
    
    Assert.assertEquals("24", Config.get("test.bar"));
  }
  
  @Test
  public void testAppWithLocalModuleDir() throws Exception {
    TestHelper.mockTerminology();
    TestHelper.exportOff();
    String[] args = {"-s", "0", "-p", "0",
        "-d", "src/test/resources/module", "-m", "copd*"};
    final PrintStream original = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Running with options:"));
    Assert.assertTrue(output.contains("Seed:"));
    Assert.assertTrue(output.contains("Modules:"));
    Assert.assertTrue(output.contains("COPD Module"));
    Assert.assertTrue(output.contains("COPD_TEST Module"));
    System.setOut(original);    
  }
  
  @Test
  public void testInvalidArgs() throws Exception {
    TestHelper.mockTerminology();
    String[] args = {"-s", "foo", "-p", "foo", testStateDefault, testTownDefault};
    final PrintStream original = System.out;
    final PrintStream originalErr = System.err;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final PrintStream print = new PrintStream(out, true);
    System.setOut(print);
    System.setErr(print);
    App.main(args);
    out.flush();
    String output = out.toString();
    Assert.assertTrue(output.contains("Usage"));
    Assert.assertFalse(output.contains("Running with options:"));
    System.setOut(original);
    System.setErr(originalErr);
  }

}
