package hudson.scm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SurroundSCM Tester.
 *
 * @author Paul Vincent
 * @since <pre>11/07/2016</pre>
 * @version 1.0
 */
public class SSCMUtilsTest extends TestCase {
  private final static String URL = "sscm://server:4900//branch//Mainline/Path/To/Repository";

  public SSCMUtilsTest(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetServerFromURL() throws Exception {
    String server = SSCMUtils.getServerFromURL(URL);
    assertEquals("server", server);
  }

  public void testGetPortFromURL() throws Exception {
    String port = SSCMUtils.getPortFromURL(URL);
    assertEquals("4900", port);
  }

  public void testGetBranchFromURL() throws Exception {
    String port = SSCMUtils.getBranchFromURL(URL);
    assertEquals("branch", port);
  }

  public void testGetRepositoryFromURL() throws Exception {
    String port = SSCMUtils.getRepositoryFromURL(URL);
    assertEquals("Mainline/Path/To/Repository", port);
  }

  public void testInvalidURL_server() throws Exception {
    String server = SSCMUtils.getServerFromURL("server:4900//branch//Mainline/Path/To/Repository");
    assertEquals("", server);

    server = SSCMUtils.getServerFromURL("sscm://server4900//branch//Mainline/Path/To/Repository");
    assertEquals("", server);

    server = SSCMUtils.getServerFromURL("sscm://server:");
    assertEquals("", server);

    server = SSCMUtils.getServerFromURL("sscm://:4900//branch//Mainline/Path/To/Repository");
    assertEquals("", server);
  }

  public void testInvalidURL_port() throws Exception {
    String port = SSCMUtils.getPortFromURL("server:4900//branch//Mainline/Path/To/Repository");
    assertEquals("", port);

    port = SSCMUtils.getPortFromURL("sscm://:4900//branch//Mainline/Path/To/Repository");
    assertEquals("4900", port);

    port = SSCMUtils.getPortFromURL("sscm://server://branch//Mainline/Path/To/Repository");
    assertEquals("", port);

    port = SSCMUtils.getPortFromURL("sscm://server:4900/branch//Mainline/Path/To/Repository");
    assertEquals("", port);

    port = SSCMUtils.getPortFromURL("sscm://server:4900branch//Mainline/Path/To/Repository");
    assertEquals("", port);

    port = SSCMUtils.getPortFromURL("sscm://server4900//branch//Mainline/Path/To/Repository");
    assertEquals("", port);
  }

  public void testInvalidURL_Branch() throws Exception {
    String branch = SSCMUtils.getBranchFromURL("server:4900//branch//Mainline/Path/To/Repository");
    assertEquals("", branch);

    branch = SSCMUtils.getBranchFromURL("sscm://server:4900////Mainline/Path/To/Repository");
    assertEquals("", branch);

    branch = SSCMUtils.getBranchFromURL("sscm://server:4900branch//Mainline/Path/To/Repository");
    assertEquals("", branch);

    branch = SSCMUtils.getBranchFromURL("sscm://server:4900//branchMainline/Path/To/Repository");
    assertEquals("", branch);

    branch = SSCMUtils.getBranchFromURL("sscm://server:4900//branch/Mainline/Path/To/Repository");
    assertEquals("", branch);

    branch = SSCMUtils.getBranchFromURL("sscm://server:4900/branch//Mainline/Path/To/Repository");
    assertEquals("", branch);
  }

  public void testInvalidURL_Repo() throws Exception {
    String repo = SSCMUtils.getRepositoryFromURL("server:4900//branch//Mainline/Path/To/Repository");
    assertEquals("", repo);

    repo = SSCMUtils.getRepositoryFromURL("sscm://server:4900/branch//Mainline/Path/To/Repository");
    assertEquals("", repo);

    repo = SSCMUtils.getRepositoryFromURL("sscm://server:4900branch//Mainline/Path/To/Repository");
    assertEquals("", repo);

    repo = SSCMUtils.getRepositoryFromURL("sscm://server:4900//branch/Mainline/Path/To/Repository");
    assertEquals("", repo);

    repo = SSCMUtils.getRepositoryFromURL("sscm://server:4900//branchMainline/Path/To/Repository");
    assertEquals("", repo);

    repo = SSCMUtils.getRepositoryFromURL("sscm://server:4900//branch//");
    assertEquals("", repo);
  }

  public void testValidateURL() throws Exception {
    assertTrue(SSCMUtils.validateSSCMURL(URL));
  }

  public static Test suite() {
    return new TestSuite(SSCMUtilsTest.class);
  }
}
