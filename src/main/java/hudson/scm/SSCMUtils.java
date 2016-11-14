package hudson.scm;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;

/**
 * Class of basic utils for working with 'sscm://' urls.
 */
public class SSCMUtils {
  private final static String URI = "sscm://";

  /**
   * Parses the Surround SCM Server host name from the passed in sscm:// url.
   *
   * ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   *
   * @param URL ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   * @return  Returns server
   */
  public static String getServerFromURL(String URL)
  {
    String result = "";

    if(validateSSCMURL(URL))
    {
      int startOfServer = URI.length();
      int endOfServer = URL.indexOf(":", startOfServer);
      if(startOfServer > 0 && endOfServer > 0)  // Everything should be greater than 0 because 0 = the URI
        result = URL.substring(startOfServer, endOfServer);
    }

    return result;
  }

  /**
   * Parses the Surround SCM Server port from the pased in sscm:// url.
   *
   * ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   *
   * @param URL ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   * @return returns the port (ex. 4900)
   */
  public static String getPortFromURL(String  URL)
  {
    String port = "";
    if(validateSSCMURL(URL))
    {
      int startOfPort = URL.indexOf(":",URI.length()) ; // start search after the URI's ':'
      int endOfPort = URL.indexOf("//", startOfPort);
      if(startOfPort > 0 && endOfPort > 0)   // Everything should be greater than 0 because 0 = the URI
        port  = URL.substring(startOfPort + 1, endOfPort); // Need to account for the ':'
        try {
          Integer.parseInt(port);
        } catch( NumberFormatException ex)
        {
          port = "";
        }
    }
    return port;
  }

  /**
   * Parses the Surround SCM Server port from the pased in sscm:// url.
   *
   * ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   *
   * @param URL ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   * @return returns the port (ex. 4900)
   */
  public static String getBranchFromURL(String  URL)
  {
    String branch = "";
    if(validateSSCMURL(URL))
    {
      int startOfBranch = URL.indexOf("//", URI.length()); // start search after the URI's '//'
      int endOfBranch = URL.indexOf("//", startOfBranch + 2);
      if(startOfBranch > 0 && endOfBranch > 0) {   // Everything should be greater than 0 because 0 = the URI
        branch  = URL.substring(startOfBranch + 2, endOfBranch);
      }
    }
    return branch;
  }

  /**
   * Parses the Surround SCM Server port from the pased in sscm:// url.
   *
   * ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   *
   * @param URL ex. sscm://server:4900//branch//Mainline/Path/To/Repository
   * @return returns the port (ex. 4900)
   */
  public static String getRepositoryFromURL(String  URL)
  {
    String repository = "";
    if(validateSSCMURL(URL))
    {
      int startOfRepository = URL.lastIndexOf("//");
      if(startOfRepository > 0)   // Everything should be greater than 0 because 0 = the URI
        repository  = URL.substring(startOfRepository + 2); // Need to account for the fact that start found "//"
    }
    return repository;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static boolean validateSSCMURL(String URL)
  {
    if(!URL.startsWith(URI))
      return false;

    String[] splitURL = URL.split("//");

    if(splitURL.length != 4)
      return false;

    // Check section 1
    if(!splitURL[0].equals("sscm:"))
      return false;

    // Check section 2
    String[] splitServerPort = splitURL[1].split(":");
    if(splitServerPort.length != 2)
      return false;

    if(splitServerPort[0].isEmpty() || splitServerPort[1].isEmpty())
      return false;

    try
    {
      Integer.parseInt(splitServerPort[1]);
    } catch (NumberFormatException ex)
    {
      return false;
    }

    // Check section 3 & 4
    return !(splitURL[2].isEmpty() || splitURL[3].isEmpty());
  }

  public static Node workspaceToNode(FilePath workspace)
  {
    Jenkins j = Jenkins.getActiveInstance();
    if(workspace != null && workspace.isRemote())
    {
      for(Computer c : j.getComputers())
      {
        if(c.getChannel() == workspace.getChannel())
        {
          Node n = c.getNode();
          if(n != null)
            return n;
        }
      }
    }
    return j;
  }

}
