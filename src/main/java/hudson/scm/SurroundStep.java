package hudson.scm;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
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
      if(url != null)
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
    return result;
  }

  @Exported
  public String getRsaKeyFileId() {
    String result = null;
    if(rsaKey != null && rsaKey.getRsaKeyType() == RSAKey.Type.ID) {
      result = rsaKey.getRsaKeyValue();
    }
    return result;
  }

  @Exported
  @CheckForNull
  public StandardUsernameCredentials getCredentials(Job<?,?> owner, EnvVars env) {
    String server = SSCMUtils.getServerFromURL(url);
    String port = SSCMUtils.getPortFromURL(url);

    return SSCMUtils.getCredentials(owner, env, server, port, credentialsId);
  }

  @Exported
  @CheckForNull
  public FileCredentials getFileCredentials(Job<?,?> owner, EnvVars env) {
    String server = SSCMUtils.getServerFromURL(url);
    String port = SSCMUtils.getPortFromURL(url);

    return SSCMUtils.getFileCredentials(owner, env, server ,port, rsaKey);
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

    /**
     * This populates the Username//Password credential dropdown on the config page.
     *
     * @return  Returns a list of credentials to populate the combobox with.
     */
    @Exported
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String remote) {
      return SSCMUtils.doFillCredentialsIdItems(context, remote);
    }

    /**
     * This populates the rsaKeyFileId dropdown with a list of 'FileCredentials' that could be used.
     *
     * @return  Returns a list of FileCredential objects that have been configured.
     */
    @Exported
    public ListBoxModel doFillRsaKeyFileIdItems(@AncestorInPath Item context, @QueryParameter String remote) {
      return SSCMUtils.doFillRsaKeyFileIdItems(context, remote);
    }
  }
}
