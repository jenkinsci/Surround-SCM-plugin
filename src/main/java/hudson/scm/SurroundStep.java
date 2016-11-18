package hudson.scm;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.scm.config.RSAKey;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs Surround SCM using {@link SurroundSCM}
 */
public class SurroundStep extends SCMStep {
  private final String sscm_url;
  private final String credentialsId;
  private RSAKey rsaKey;

  @DataBoundConstructor
  public SurroundStep(String sscm_url, String credentialsId)
  {
    this.sscm_url = Util.fixEmptyAndTrim(sscm_url);
    this.credentialsId = Util.fixEmptyAndTrim(credentialsId);

  }

  @DataBoundSetter
  public void setRsaKey(RSAKey rsaKey) { this.rsaKey = rsaKey; }

  @Nonnull
  @Override
  protected SCM createSCM() {
    String server = SSCMUtils.getServerFromURL(sscm_url);
    String port = SSCMUtils.getPortFromURL(sscm_url);
    String branch = SSCMUtils.getBranchFromURL(sscm_url);
    String repository = SSCMUtils.getRepositoryFromURL(sscm_url);

    SurroundSCM sscm = new SurroundSCM(server, port, branch, repository, credentialsId);
    sscm.setRsaKey(rsaKey);
    return sscm;
  }

  public boolean hasRsaKeyConfigured() {
    return rsaKey == null || rsaKey.getRsaKeyType() != RSAKey.Type.NoKey;
  }

  public boolean isUsingRsaKeyPath() {
    return rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.Path;
  }

  public boolean isUsingRsaKeyFileId() {
    return rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID;
  }

  public String getSscm_url() {
    return sscm_url;
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public RSAKey getRsaKey() {
    return rsaKey;
  }

  public String getRsaKeyFilePath()
  {
    String result = null;
    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.Path) {
      result = rsaKey.getRsaKeyValue();
    }
    Logger.getLogger(SurroundSCM.class.toString()).log(Level.SEVERE, String.format("getRsaKeyPath - Value: [%s]", result));
    return result;
  }

  public String getRsaKeyFileId() {
    String result = null;
    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID) {
      result = rsaKey.getRsaKeyValue();
    }
    Logger.getLogger(SurroundSCM.class.toString()).log(Level.SEVERE, String.format("getRsaKeyFileId - Value: [%s]", result));
    return result;
  }

  // TODO: Somehow make this a shared function between SurroundSCM and SurroundStep
  @CheckForNull
  public StandardUsernameCredentials getCredentials(Job<?,?> owner, EnvVars env) {
    String server = SSCMUtils.getServerFromURL(sscm_url);
    String port = SSCMUtils.getPortFromURL(sscm_url);

    if(credentialsId != null) {
      for (StandardUsernameCredentials c : SurroundSCM.availableCredentials(owner, env.expand("sscm://" + server + ":" + port))) { // TODO: This seems like a royal hack.
        if(c.getId().equals(credentialsId)) {
          return c;
        }
      }
    }
    return null;
  }

  // TODO: Somehow make this a shared function between SurroundSCM and SurroundStep
  @CheckForNull
  public FileCredentials getFileCredentials(Job<?,?> owner, EnvVars env) {
    String server = SSCMUtils.getServerFromURL(sscm_url);
    String port = SSCMUtils.getPortFromURL(sscm_url);

    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID) {
      for(FileCredentials fc : SurroundSCM.availableFileCredentials(owner, env.expand(String.format("sscm://%s:%s", server, port)))) { // TODO: This seems like a royal hack
        if(fc.getId().equals(rsaKey.getRsaKeyValue())) {
          return fc;
        }
      }
    }
    return null;
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

    // TODO: Somehow make this a shared function between SurroundSCM and SurroundStep
    /**
     * This populates the Username//Password credential dropdown on the config page.
     *
     * @return  Returns a list of credentials to populate the combobox with.
     */
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Job<?,?> owner, @QueryParameter String source)
    {
      if(owner == null || !owner.hasPermission(Item.EXTENDED_READ)) {
        return new ListBoxModel();
      }
      return new StandardUsernameListBoxModel().withEmptySelection().withAll(SurroundSCM.availableCredentials(owner, new EnvVars().expand(source)));
    }

    // TODO: Somehow make this a shared function between SurroundSCM and SurroundStep
    /**
     * This populates the rsaKeyFileId dropdown with a list of 'FileCredentials' that could be used.
     *
     * @return  Returns a list of FileCredential objects that have been configured.
     */
    public ListBoxModel doFillRsaKeyFileIdItems(@AncestorInPath Job<?, ?> owner, @QueryParameter String source) {
      if(owner == null || !owner.hasPermission(Item.EXTENDED_READ)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(SurroundSCM.availableFileCredentials(owner, new EnvVars().expand(source)));
    }
  }
}
