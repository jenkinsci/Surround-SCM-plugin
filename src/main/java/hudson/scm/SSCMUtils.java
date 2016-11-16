package hudson.scm;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class of basic utils for working with 'sscm://' urls.
 */
public class SSCMUtils {
  private final static String URI_FORMAT = "sscm://(.*):(.*)//(.*)//(.*)";
  private final static Pattern URI_PATTERN = Pattern.compile(URI_FORMAT);

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

    if(URL != null)
    {
      Matcher changeMatcher = URI_PATTERN.matcher(URL);

      if(changeMatcher.find() && changeMatcher.groupCount() == 4)
      {
        result = changeMatcher.group(1);
      }
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
  public static String getPortFromURL(String URL)
  {
    String result = "";

    if(URL != null)
    {
      Matcher changeMatcher = URI_PATTERN.matcher(URL);

      if(changeMatcher.find() && changeMatcher.groupCount() == 4)
      {
        result = changeMatcher.group(2);
      }
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
  public static String getBranchFromURL(String URL)
  {
    String result = "";

    if(URL != null)
    {
      Matcher changeMatcher = URI_PATTERN.matcher(URL);

      if(changeMatcher.find() && changeMatcher.groupCount() == 4)
      {
        result = changeMatcher.group(3);
      }
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
  public static String getRepositoryFromURL(String URL)
  {
    String result = "";

    if(URL != null)
    {
      Matcher changeMatcher = URI_PATTERN.matcher(URL);

      if(changeMatcher.find() && changeMatcher.groupCount() == 4)
      {
        result = changeMatcher.group(4);
      }
    }

    return result;
  }

  public static boolean validateSSCMURL(String URL)
  {
    if(URL == null)
      return false;

    Matcher changeMatcher = URI_PATTERN.matcher(URL);

    boolean result = false;

    if(changeMatcher.find() && changeMatcher.groupCount() == 4)
    {
      result = true;
    }

    return result;
  }

  public static Node workspaceToNode(FilePath workspace)
  {
    Jenkins j = Jenkins.getInstance();
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
