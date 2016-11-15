package hudson.scm;

import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

/**
 * Runs Surround SCM using {@link SurroundSCM}
 */
public class SurroundStep extends SCMStep {
  private final String sscm_url;
  private final String username;
  private final String password;
  private String RSAKeyFile;



  @DataBoundConstructor
  public SurroundStep(String sscm_url, String username, String password)
  {
    this.sscm_url = Util.fixEmptyAndTrim(sscm_url);
    this.username = Util.fixEmptyAndTrim(username);
    this.password = Util.fixEmptyAndTrim(password);
    this.RSAKeyFile = null;

  }

  @DataBoundSetter
  public void setRSAKeyFile(String RSAKeyFile) { this.RSAKeyFile = Util.fixEmptyAndTrim(RSAKeyFile); }

  @SuppressWarnings("unused")
  @Deprecated
  public SurroundStep(String sscm_url, String username, String password, String RSAKeyFile)
  {
    this.sscm_url = sscm_url;
    this.username = username;
    this.password = password;
    this.RSAKeyFile = RSAKeyFile;

  }

  @Nonnull
  @Override
  protected SCM createSCM() {
    String server = SSCMUtils.getServerFromURL(sscm_url);
    String port = SSCMUtils.getPortFromURL(sscm_url);
    String branch = SSCMUtils.getBranchFromURL(sscm_url);
    String repository = SSCMUtils.getRepositoryFromURL(sscm_url);

    SurroundSCM sscm = new SurroundSCM(server, port, username, password, branch, repository);
    sscm.setRsaKeyPath(RSAKeyFile);
    return sscm;
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
