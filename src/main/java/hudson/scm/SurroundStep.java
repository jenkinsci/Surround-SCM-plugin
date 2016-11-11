package hudson.scm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Runs Surround SCM using {@link SurroundSCM}
 */
public class SurroundStep extends SCMStep {
  private final String sscm_url;
  private final String username;
  private final String password;
  private final String RSAKeyFile;
  private final String sscmPath;


  @DataBoundConstructor
  public SurroundStep(String sscm_url, String username, String password, String sscmPath, String RSAKeyFile)
  {
    this.sscm_url = sscm_url;
    this.username = username;
    this.password = password;
    this.sscmPath = sscmPath;
    this.RSAKeyFile = RSAKeyFile;

  }

  @Nonnull
  @Override
  protected SCM createSCM() {
    // Parse Server out of combined variable
    // Parse Branch out of combined
    // Parse Repository path

    String server = SSCMUtils.getServerFromURL(sscm_url);
    String port = SSCMUtils.getPortFromURL(sscm_url);
    String branch = SSCMUtils.getBranchFromURL(sscm_url);
    String repository = SSCMUtils.getRepositoryFromURL(sscm_url);
    return new SurroundSCM(RSAKeyFile, server, port, username, password, branch, repository, sscmPath, true);
  }

  public String getSscm_url() {
    return sscm_url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getRSAKeyFile() {
    return RSAKeyFile;
  }

  public String getSscmPath() {
    return sscmPath;
  }

  @Extension
  public static final class DescriptorImpl extends SCMStepDescriptor {

    @Override
    public String getFunctionName() {
      return "sscm";
    }

    @Override
    public String getDisplayName() {
      return "Surround SCM";
    }
  }
}
