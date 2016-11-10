package hudson.scm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Runs Surround SCM using {@link SurroundSCM}
 */
public class SurroundStep extends SCMStep {
  private final String username;
  private final String password;
  private final String RSAKeyFile;
  private final String sscmPath;

  private final String server;
  private final String port;
  private final String branch;
  private final String repository;

  @DataBoundConstructor
  public SurroundStep(String sscm_url, String username, String password, String sscmPath, String RSAKeyFile)
  {
    this.username = username;
    this.password = password;
    this.sscmPath = sscmPath;
    this.RSAKeyFile = RSAKeyFile;

    this.server = SSCMUtils.getServerFromURL(sscm_url);
    this.port = SSCMUtils.getPortFromURL(sscm_url);
    this.branch = SSCMUtils.getBranchFromURL(sscm_url);
    this.repository = SSCMUtils.getRepositoryFromURL(sscm_url);
  }

  @Nonnull
  @Override
  protected SCM createSCM() {
    // Parse Server out of combined variable
    // Parse Branch out of combined
    // Parse Repository path
    return new SurroundSCM(RSAKeyFile, server, port, username, password, branch, repository, "Executable", true);
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
