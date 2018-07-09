package hudson.scm;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Node;
import hudson.scm.config.RSAKey;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for common, shared methods used to make the Surround SCM integration function.
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

  /**
   * Performs basic sscm:// url validation by attempting to match it to the URI_PATTERN variable.
   *
   * @param URL URL to test to see if it is valid.
   * @return  Returns true if the URL is valid, false if not.
   */
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

  /**
   * Helper function which finds the 'Node' for a provided 'workspace'
   *
   * @param workspace A 'workspace' that was provided by Jenkins
   * @return  The 'Node' that the workspace exists on. Defaults to the primary Jenkins instance.
   */
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

  public static ListBoxModel doFillCredentialsItems(@AncestorInPath Item context, @QueryParameter String remote, Class credentialType) {
    if (context == null && !Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER) ||
            context != null && !context.hasPermission(Item.EXTENDED_READ)) {
      return new StandardListBoxModel();
    }
    List<DomainRequirement> domainRequirements;
    if (remote == null) {
      domainRequirements = Collections.<DomainRequirement>emptyList();
    } else {
      domainRequirements = URIRequirementBuilder.fromUri(remote.trim()).build();
    }
    return new StandardListBoxModel()
            .withEmptySelection()
            .withMatching(
                    CredentialsMatchers.instanceOf(credentialType),
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM, domainRequirements)
            );
  }

  /**
   * This populates the Username//Password credential dropdown on the config page.
   *
   * @param context - Owner
   * @param remote - Source
   * @return  Returns a list of credentials to populate the combobox with.
   */
  public static ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String remote)
  {
    return doFillCredentialsItems(context, remote, StandardUsernamePasswordCredentials.class);
  }

  /**
   * This populates the rsaKeyFileId dropdown with a list of 'FileCredentials' that could be used.
   *
   * @param context - Owner
   * @param remote - Source
   * @return  Returns a list of FileCredential objects that have been configured.
   */
  public static ListBoxModel doFillRsaKeyFileIdItems(@AncestorInPath Item context, @QueryParameter String remote) {
    return doFillCredentialsItems(context, remote, FileCredentials.class);
  }

  /**
   * Uses the {@link CredentialsProvider} to lookup {@link StandardUsernameCredentials} which will be used to populate
   * the username // password  dropdown box.
   *
   * @param owner   Job that this is being performed for
   * @param source  A.... source?
   * @return  Returns a list of {@link StandardUsernameCredentials} which can be thrown into a
   *          {@link StandardUsernameListBoxModel}
   */
  public static List<? extends StandardUsernameCredentials> availableCredentials(Job<?, ?> owner, String source) {
    return CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, owner, null, URIRequirementBuilder.fromUri(source).build());
  }

  /**
   * Uses the {@link CredentialsProvider} to lookup {@link FileCredentials} which will be used to populate
   * the username // password  dropdown box.
   *
   * @param owner   A.... Jobish object?
   * @param source  A.... source?
   * @return  Returns a list of {@link FileCredentials} which can be thrown into a {@link StandardListBoxModel}
   */
  public static List<? extends FileCredentials> availableFileCredentials(Job<?, ?> owner, String source) {
    return CredentialsProvider.lookupCredentials(FileCredentials.class, owner, null, URIRequirementBuilder.fromUri(source).build());
  }


  /**
   * Looks up a specific credential based on the credential ID.
   * @param owner   Used during credential lookup from the CredentialProvider
   * @param env     Used to generate a 'Source' string
   * @param server  Used to generate the source string
   * @param port    Used to generate the source string
   * @param credentialsId ID string of the credential to lookup.
   * @return  Returns the {@link StandardUsernameCredentials} matching the specified credentialsID, or null
   */
  @CheckForNull
  public static StandardUsernameCredentials getCredentials(Job<?,?> owner, EnvVars env,
                                                           String server, String port, String credentialsId) {
    if(credentialsId != null) {
      List<? extends StandardUsernameCredentials> credentials = availableCredentials(owner, env.expand(String.format("sscm://%s:%s", server, port)));

      for (StandardUsernameCredentials c : credentials) {
        if(c.getId().equals(credentialsId)) {
          return c;
        }
      }
    }
    return null;
  }

  /**
   * Looks up a specific file credential based on its ID.
   * @param owner   Used during credential lookup from the CredentialProvider
   * @param env     Used to generate a 'Source' string
   * @param server  Used to generate the source string
   * @param port    Used to generate the source string
   * @param rsaKey  This will only work if this is an rsaKey with a {@link RSAKey.Type} of "ID"
   * @return  Returns the fileCredential specified in the {@link RSAKey} 'value', or null
   */
  @CheckForNull
  public static FileCredentials getFileCredentials(Job<?,?> owner, EnvVars env,
                                            String server, String port, RSAKey rsaKey) {

    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID) {
      List<? extends FileCredentials> credentials = availableFileCredentials(owner, env.expand(String.format("sscm://%s:%s", server, port)));

      for(FileCredentials fc : credentials) {
        if(fc.getId().equals(rsaKey.getRsaKeyValue())) {
          return fc;
        }
      }
    }
    return null;
  }
}
