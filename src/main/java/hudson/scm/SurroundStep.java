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
import org.kohsuke.stapler.export.Exported;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs Surround SCM using {@link SurroundSCM}
 */
public class SurroundStep extends SCMStep {
  private final String url;
  private final String credentialsId;
  private RSAKey rsaKey;

  @DataBoundConstructor
  public SurroundStep(String url, String credentialsId)
  {
    url = Util.fixEmptyAndTrim(url);
    try {
      url = URLDecoder.decode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    this.url = url;
    this.credentialsId = Util.fixEmptyAndTrim(credentialsId);

  }

  @DataBoundSetter
  public void setRsaKeyFileId(String rsaKeyFileId) {
    this.rsaKey = new RSAKey(RSAKey.Type.ID, rsaKeyFileId);
  }

  @DataBoundSetter
  public void setRsaKeyFilePath(String rsaKeyFilePath) {
    this.rsaKey = new RSAKey(RSAKey.Type.Path, rsaKeyFilePath);
  }

  @DataBoundSetter
  public void setRsaKey(RSAKey rsaKey) { this.rsaKey = rsaKey; }

  @Nonnull
  @Override
  protected SCM createSCM() {
    String server = SSCMUtils.getServerFromURL(url);
    String port = SSCMUtils.getPortFromURL(url);
    String branch = SSCMUtils.getBranchFromURL(url);
    String repository = SSCMUtils.getRepositoryFromURL(url);

    SurroundSCM sscm = new SurroundSCM(server, port, branch, repository, credentialsId);
    sscm.setRsaKey(rsaKey);
    return sscm;
  }

  @Exported
  public boolean hasRsaKeyConfigured() {
    return rsaKey == null || rsaKey.getRsaKeyType() != RSAKey.Type.NoKey;
  }

  @Exported
  public boolean isUsingRsaKeyPath() {
    return rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.Path;
  }

  @Exported
  public boolean isUsingRsaKeyFileId() {
    return rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID;
  }

  @Exported
  public String getUrl() {
    return url;
  }

  @Exported
  public String getCredentialsId() {
    return credentialsId;
  }

  /**
   * So... the RSA key combobox requires we use an RSAKey object, however forcing users to define an RSA key object
   * for pipelines is annoying as hell.
   * @return Always returns null to prevent this from showing up in the Snippet Generator
   */
  @Exported
  public RSAKey getRsaKey() {
    return null;
  }

  @Exported
  public String getRsaKeyFilePath()
  {
    String result = null;
    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.Path) {
      result = rsaKey.getRsaKeyValue();
    }
    Logger.getLogger(SurroundSCM.class.toString()).log(Level.SEVERE, String.format("getRsaKeyPath - Value: [%s]", result));
    return result;
  }

  @Exported
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
    String server = SSCMUtils.getServerFromURL(url);
    String port = SSCMUtils.getPortFromURL(url);

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
    String server = SSCMUtils.getServerFromURL(url);
    String port = SSCMUtils.getPortFromURL(url);

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
