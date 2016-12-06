package hudson.scm;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public final class SurroundTool extends ToolInstallation implements NodeSpecific<SurroundTool>, EnvironmentSpecific<SurroundTool>
{
  public static transient final String DEFAULT_NAME = "Default";

  private static final long serialVersionUID = 1;

  @DataBoundConstructor
  public SurroundTool(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
  }

  public String getSscmExe() { return getHome(); }

  private static SurroundTool[] getInstallations(DescriptorImpl descriptor) {
    SurroundTool[] installations = null;
    try {
      installations = descriptor.getInstallations();
    } catch (NullPointerException e) {
      installations = new SurroundTool[0];
    }
    return installations;
  }

  public static SurroundTool getDefaultInstallation() {
    Jenkins jenkinsInstance = Jenkins.getInstance();
    if(jenkinsInstance == null)
      return null;

    DescriptorImpl surroundTools = jenkinsInstance.getDescriptorByType(SurroundTool.DescriptorImpl.class);
    SurroundTool tool = surroundTools.getInstallation(SurroundTool.DEFAULT_NAME);
    if(tool != null) {
      return tool;
    } else {
      SurroundTool[] installations =  surroundTools.getInstallations();
      if(installations.length > 0) {
        return installations[0];
      } else {
        onLoaded();
        return surroundTools.getInstallations()[0];
      }
    }
  }

  @Override
  public SurroundTool forNode(@NonNull Node node, TaskListener log) throws IOException, InterruptedException {
    return new SurroundTool(getName(), translateFor(node, log), Collections.<ToolProperty<?>>emptyList());
  }

  @Override
  public SurroundTool forEnvironment(EnvVars environment) {
    return new SurroundTool(getName(), environment.expand(getHome()), Collections.<ToolProperty<?>>emptyList());
  }

  @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
  public static void onLoaded() {
    // creates default tool installation if needed. Uses "sscm" or migrates data from previous versions.

    Jenkins jenkinsInstance = Jenkins.getInstance();
    if(jenkinsInstance == null)
      return;

    DescriptorImpl descriptor = (DescriptorImpl)jenkinsInstance.getDescriptor(SurroundTool.class);
    SurroundTool[] installations = getInstallations(descriptor);

    if(installations != null && installations.length > 0) {
      // No need to initialize if there's already something
      return;
    }

    String defaultSscmExe = isWindows() ? "sscm.exe" : "sscm";
    SurroundTool tool = new SurroundTool(DEFAULT_NAME, defaultSscmExe, Collections.<ToolProperty<?>>emptyList());
    descriptor.setInstallations(new SurroundTool[]{ tool });
    descriptor.save();
  }

  @Extension @Symbol("sscm")
  public static class DescriptorImpl extends ToolDescriptor<SurroundTool>
  {
    @Override
    public String getDisplayName() {
      return "Surround SCM";
    }

    @Override
    public FormValidation doCheckHome(@QueryParameter File value) {
      Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
      String path = value.getPath();
      return FormValidation.validateExecutable(path);
    }

    public SurroundTool getInstallation(String name) {
      for(SurroundTool s : getInstallations()) {
        if(s.getName().equals(name))
          return s;
      }
      return null;
    }
  }

  private static boolean isWindows() {
    return File.pathSeparatorChar==';';
  }
}