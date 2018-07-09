package hudson.scm.config;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.Exported;

import java.io.Serializable;

public class RSAKey extends AbstractDescribableImpl<RSAKey> implements Serializable {

  public enum Type {
    NoKey,
    Path,
    ID
  }

  private String rsaKeyValue;
  private Type rsaKeyType;

  @Exported
  public String getName() { return "RSAKey"; }

  @DataBoundConstructor
  public RSAKey() {
    this.rsaKeyValue = null;
    rsaKeyType = Type.NoKey;
  }

  public RSAKey(Type type, String value) {
    this.rsaKeyValue = Util.fixEmptyAndTrim(value);

    // If the value is empty, then we can't really use it, so default to 'No Key' for the type.
    if(this.rsaKeyValue == null)
      this.rsaKeyType = Type.NoKey;
    else
      this.rsaKeyType = type;
  }

  @DataBoundSetter
  public void setRsaKeyFileId(String rsaKeyFileId) {
    this.rsaKeyValue = Util.fixEmptyAndTrim(rsaKeyFileId);
    this.rsaKeyType = Type.ID;
  }

  /**
   * This solely exists to fix the stupid Snippet Generator from crashing when asking about this function.
   * @return Returns 'null'
   */
  @Exported
  public String getRsaKeyFileId() { return null; }

  @DataBoundSetter
  public void setRsaKeyFilePath(String rsaKeyFilePath) {
    this.rsaKeyValue = Util.fixEmptyAndTrim(rsaKeyFilePath);
    this.rsaKeyType = Type.Path;
  }

  /**
   * This solely exists to fix the stupid Snippet Generator from crashing when asking about this function.
   * @return Returns 'null'
   */
  @Exported
  public String getRsaKeyFilePath() { return null; }

  @DataBoundSetter
  public void setRsaKeyValue(String rsaKeyValue) { this.rsaKeyValue = rsaKeyValue; }

  @DataBoundSetter
  public void setRsaKeyType(Type rsaKeyType) { this.rsaKeyType = rsaKeyType; }

  public Type getRsaKeyType() { return rsaKeyType; }
  public String getRsaKeyValue() { return rsaKeyValue; }

  @Extension
  public static class DescriptorImpl extends Descriptor<RSAKey> {
    @Override
    public String getDisplayName() { return "RSA Key"; }
  }
}