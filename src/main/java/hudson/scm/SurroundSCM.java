package hudson.scm;

import hudson.*;
import hudson.model.*;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Extension
public final class SurroundSCM extends SCM {

  /*---------------------INNER CLASS------------------------------------------------------------*/

  public static class SurroundSCMDescriptor extends
        SCMDescriptor<SurroundSCM> {

    @Override
    public boolean isApplicable(Job project) {
      return true;
    }

    /**
     * Constructs a new SurroundSCMDescriptor.
     */
    protected SurroundSCMDescriptor() {
      super(SurroundSCM.class, null);
      load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return "Surround SCM";
    }

    @Override
    public SCM newInstance(StaplerRequest req, JSONObject formData)
          throws FormException {
      return req.bindJSON(SurroundSCM.class, formData);
    }

  }

  // if there are > changesThreshold changes, that it's build now -
  // incomparable
  // if there are < changesThreshold changes, but > 0 changes, then it's
  // significant
  private final int changesThreshold = 5;
  private final int pluginVersion = 9;

  // config options
  private String rsaKeyPath;
  private String server;
  private String serverPort;
  private String userName ;
  private String password ;
  private String branch ;
  private String repository;

  @Deprecated
  private String surroundSCMExecutable;

  private String sscm_tool_name;

  private boolean bIncludeOutput;


  //getters and setters
  public String getRsaKeyPath() {
    return rsaKeyPath;
  }

  public void setRsaKeyPath(String rsaKeyPath) {
    this.rsaKeyPath = rsaKeyPath;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getServerPort() {
    return serverPort;
  }

  public void setServerPort(String serverPort) {
    this.serverPort = serverPort;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public boolean getIncludeOutput() {
    return bIncludeOutput;
  }

  public void setIncludeOutput(boolean includeOutput) {
    this.bIncludeOutput = includeOutput;
  }

  /**
   * Singleton descriptor.
   */
  @Extension
  public static final SurroundSCMDescriptor DESCRIPTOR = new SurroundSCMDescriptor();

  private static final String SURROUND_DATETIME_FORMAT_STR = "yyyyMMddHHmmss";
  private static final String SURROUND_DATETIME_FORMAT_STR_2 = "yyyyMMddHH:mm:ss";

  @DataBoundConstructor
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch,  String repository)
  {
    this.rsaKeyPath = rsaKeyPath;
    this.server = server;
    this.serverPort = serverPort;
    this.userName = userName;
    this.password = password;
    this.branch = branch;
    this.repository = repository;
    this.bIncludeOutput = true; // Leaving this here for future functionality.

    this.surroundSCMExecutable = null;
  }

  @Deprecated
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch, String repository, String surroundSCMExecutable,
                     boolean includeOutput) {
    this.rsaKeyPath = rsaKeyPath;
    this.server = server;
    this.serverPort = serverPort;
    this.userName = userName;
    this.password = password;
    this.branch = branch;
    this.repository = repository;
    this.surroundSCMExecutable = surroundSCMExecutable;
    this.bIncludeOutput = true; // Leaving this here for future functionality.
  }

  /**
   * @deprecated Deprecated as of release v9, added option to include // exclude output.
   */
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch, String repository, String surroundSCMExecutable)
  {
    this(rsaKeyPath, server, serverPort, userName, password, branch, repository, surroundSCMExecutable, true);
  }

  public SurroundSCM() {

  }

  @Override
  public SCMDescriptor<?> getDescriptor() {
    return DESCRIPTOR;
  }

  /**
   * Calculates the SCMRevisionState that represents the state of the
   * workspace of the given build. The returned object is then fed into the
   * compareRemoteRevisionWith(AbstractProject, Launcher, FilePath,
   * TaskListener, SCMRevisionState) method as the baseline SCMRevisionState
   * to determine if the build is necessary.
   *
   * {@inheritDoc}
   */
  @Override
  public SCMRevisionState calcRevisionsFromBuild(@Nonnull Run<?, ?> build,
                                                 @Nullable FilePath workspace,
                                                 @Nullable Launcher launcher,
                                                 @Nonnull TaskListener listener) throws IOException, InterruptedException
  {
    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR);

    final Date lastBuildDate = build.getTime();
    final int lastBuildNum = build.getNumber();
    SurroundSCMRevisionState scmRevisionState = new SurroundSCMRevisionState(lastBuildDate, lastBuildNum);
    listener.getLogger().println("calcRevisionsFromBuild determined revision for build #" + scmRevisionState.getBuildNumber() + " built originally at " + scm_datetime_formatter.format(scmRevisionState.getDate()) + " pluginVer: " + pluginVersion);

    return scmRevisionState;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean requiresWorkspaceForPolling() {
    return true; // We don't actually NEED a workspace for polling. We are saving our info to a system temp file.
  }

  @Override
  public boolean supportsPolling() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PollingResult compareRemoteRevisionWith(
    @Nonnull Job<?, ?> project, @Nullable Launcher launcher, @Nullable FilePath workspace,
    @Nonnull TaskListener listener, @Nonnull SCMRevisionState baseline) throws IOException, InterruptedException
  {
    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR);

    Date lastBuild = ((SurroundSCMRevisionState)baseline).getDate();
    int lastBuildNum = ((SurroundSCMRevisionState)baseline).getBuildNumber();

    Date now = new Date();
    File temporaryFile = File.createTempFile("changes","txt");

    listener.getLogger().println("Calculating changes since build #" + lastBuildNum + " which happened at " + scm_datetime_formatter.format(lastBuild) + " pluginVer: " + pluginVersion);


    double countChanges = determineChangeCount(launcher,  listener, lastBuild, now, temporaryFile, workspace);

    if(!temporaryFile.delete())
    {
      listener.getLogger().println("Failed to delete temporary file [" + temporaryFile.getAbsolutePath() + "] marking the file to be deleted when Jenkins restarts.");
      temporaryFile.deleteOnExit();
    }

    if(countChanges == 0)
      return PollingResult.NO_CHANGES;
    else if(countChanges < changesThreshold)
      return PollingResult.SIGNIFICANT;

    return PollingResult.BUILD_NOW;
  }

  /**
   * Obtains a fresh workspace of the module(s) into the specified directory of the specified machine. We'll use
   * sscm get.
   *
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public void checkout(
    @Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace, @Nonnull TaskListener listener,
    @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException
  {

    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR_2);

    Date currentDate = new Date(); //defaults to current

    EnvVars environment = build.getEnvironment(listener);
    if (build instanceof AbstractBuild) {
      EnvVarsUtils.overrideAll(environment, ((AbstractBuild) build).getBuildVariables());
    }

    ArgumentListBuilder cmd = new ArgumentListBuilder();
    cmd.add(getSscmExe(workspace, listener, environment));//will default to sscm user can put in path
    cmd.add("get");
    cmd.add("/" );
    cmd.add("-wreplace");
    cmd.addMasked("-y".concat(userName).concat(":").concat(password));
    if(rsaKeyPath != null && !rsaKeyPath.isEmpty()) {
      cmd.add("-z".concat(rsaKeyPath));
    }
    else {
      cmd.add("-z".concat(server).concat(":").concat(serverPort));
    }
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-d".concat(workspace.getRemote()));
    cmd.add("-r");
    cmd.add("-s" + scm_datetime_formatter.format(currentDate));
    if(!bIncludeOutput)
    {
      cmd.add("-q");
    }

    //int cmdResult = workspace.createLauncher(listener).launch().envs(environment).cmds(cmd).stdout(listener.getLogger()).join();
    int cmdResult = launcher.launch().envs(environment).cmds(cmd).stdout(listener.getLogger()).join();
    if (cmdResult == 0)
    {
      Date lastBuildDate = new Date();
      lastBuildDate.setTime(0); // default to January 1, 1970

      if(baseline instanceof SurroundSCMRevisionState) {
        lastBuildDate = ((SurroundSCMRevisionState) baseline).getDate();
      }
      else
        listener.getLogger().print("No previous build information detected.");

      // Setup the revision state based on what we KNOW to be correct information.
      SurroundSCMRevisionState scmRevisionState = new SurroundSCMRevisionState(currentDate, build.number);
      build.addAction(scmRevisionState);
      listener.getLogger().println("Checkout calculated ScmRevisionState for build #" + build.number + " to be the datetime " + scm_datetime_formatter.format(currentDate) + " pluginVer: " + pluginVersion);

      if(changelogFile != null)
        captureChangeLog(launcher, workspace, listener, lastBuildDate, currentDate, changelogFile, environment);
    }

    listener.getLogger().println("Checkout completed.");
  }

  @Nonnull
  @Override
  public String getKey() {
    // Key=sscm-ServerName-BranchName-RepositoryPath
    String unsafeString = String.format("sscm-%s-%s-%s", getServer(), getBranch(), getRepository());
    String result = Util.getDigestOf(unsafeString);
    return result;
  }

  @Override
  public ChangeLogParser createChangeLogParser() {
    return new SurroundSCMChangeLogParser();
  }

  private boolean captureChangeLog(Launcher launcher, FilePath workspace,
                                   TaskListener listener, Date lastBuildDate, Date currentDate, File changelogFile,
                                   EnvVars env) throws IOException, InterruptedException {

    boolean result = true;

    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR);

    String dateRange = scm_datetime_formatter.format(lastBuildDate);
    dateRange = dateRange.concat(":");
    dateRange = dateRange.concat(scm_datetime_formatter.format(currentDate));

    ArgumentListBuilder cmd = new ArgumentListBuilder();
    cmd.add(getSscmExe(workspace, listener, env));//will default to sscm user can put in path
    cmd.add("cc");
    cmd.add("/");
    cmd.add("-d".concat(dateRange));
    cmd.addMasked("-y".concat(userName).concat(":").concat(password));
    if(rsaKeyPath != null && !rsaKeyPath.isEmpty()) {
      cmd.add("-z".concat(rsaKeyPath));
    }
    else {
      cmd.add("-z".concat(server).concat(":").concat(serverPort));
    }
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-r");

    FileOutputStream os = new FileOutputStream(changelogFile);
    try {
      BufferedOutputStream bos = new BufferedOutputStream(os);
      Writer w = new OutputStreamWriter(new FileOutputStream(changelogFile), "UTF-8");
      PrintWriter writer = new PrintWriter(w);
      try {


        int cmdResult = launcher.launch().cmds(cmd).envs(env).stdout(bos).join();
        if (cmdResult != 0)
        {
          listener.fatalError("Changelog failed with exit code " + cmdResult);
          result = false;
        }


      } finally {
        writer.close();
        bos.close();
      }
    } finally {
      os.close();
    }

    listener.getLogger().println("Changelog calculated successfully.");
    listener.getLogger().println("Change log file: " + changelogFile.getAbsolutePath() );

    return result;
  }

  private double determineChangeCount(Launcher launcher, TaskListener listener, Date lastBuildDate,
                                      Date currentDate, File changelogFile, FilePath workspace) throws IOException, InterruptedException
  {
    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR);

    double changesCount = 0;
    if (server != null )
      listener.getLogger().println("in determine Change Count server: "+server);

    String dateRange = scm_datetime_formatter.format(lastBuildDate);
    dateRange = dateRange.concat(":");
    dateRange = dateRange.concat(scm_datetime_formatter.format(currentDate));

    ArgumentListBuilder cmd = new ArgumentListBuilder();
    cmd.add(getSscmExe(workspace, listener, null));
    cmd.add("cc");
    cmd.add("/");
    cmd.add("-d".concat(dateRange));
    cmd.addMasked("-y".concat(userName).concat(":").concat(password));
    if(rsaKeyPath != null && !rsaKeyPath.isEmpty()) {
      cmd.add("-z".concat(rsaKeyPath));
    }
    else {
      cmd.add("-z".concat(server).concat(":").concat(serverPort));
    }
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-r");

    listener.getLogger().println("determineChangeCount executing the command: " + cmd.toString() + " with date range: [ " + dateRange + " ]");

    // TODO: This seems like a stupid hack.  Why are we dumping command output to a text file? Can we guarantee
    //       that the 'changelogFile' (a temp file on some machine) is at an accessible path wherever this is run?
    //       why don't we just read the command output straight into memory & immediately process it?
    FileOutputStream os = new FileOutputStream(changelogFile);
    try {
      BufferedOutputStream bos = new BufferedOutputStream(os);

      try {
        int cmdResult = launcher.launch().cmds(cmd).stdout(bos).join();
        if (cmdResult != 0)
        {
          listener.fatalError("Determine changes count failed with exit code " + cmdResult);
        }
      } finally {
        bos.close();
      }
    } finally {
      os.close();
    }

    BufferedReader br = null;
    String line = null;
    InputStreamReader is = new InputStreamReader(new FileInputStream(changelogFile), "UTF-8");
    try{
      br = new BufferedReader(is);
      line = br.readLine();
      if (line != null){
        listener.getLogger().println(line);
        String num = line.substring(6);
        try {
          changesCount = Double.valueOf(num.trim());
        } catch (NumberFormatException nfe) {
          listener.fatalError("NumberFormatException: " + nfe.getMessage());
        }
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      if(br != null)
      {
        br.close();
      }
    }
    listener.getLogger().println("Number of changes determined to be: "+changesCount);
    return changesCount;
  }

  /**
   * Attempt to find a pre-configured 'SurroundTool' with a saved 'sscm_tool_name'
   * Currently this will always fall back to the 'default' tool for the current node and requires some further
   * testing of edge conditions
   *
   * @param listener
   * @return
   */
  public SurroundTool resolveSscmTool(TaskListener listener)
  {
    // TODO_PTV: Review this function, should we allow users to override the sscm_tool_name in the project level?
    // TODO_PTV: Review this function, does this work when a node is configured to use a 2nd Surround SCM tool?
    SurroundTool sscm = null;
    if(sscm_tool_name == null || sscm_tool_name.isEmpty()) {
      sscm = SurroundTool.getDefaultInstallation();
    } else {
      sscm = Jenkins.getInstance().getDescriptorByType(SurroundTool.DescriptorImpl.class).getInstallation(sscm_tool_name);
      if (sscm == null) {
        listener.getLogger().println(String.format("Selected sscm installation [%s] does not exist. Using Default", sscm_tool_name));
        sscm = SurroundTool.getDefaultInstallation();
      }
    }
    // TODO_PTV: Is it safe to save off the tool name so we don't need to perform a lookup again?
    //if(sscm != null)
    //  sscm_tool_name = sscm.getName();

    return sscm;
  }

  public String getSscmExe( FilePath workspace, TaskListener listener, EnvVars env) throws IOException, InterruptedException {
    if(workspace != null) {
      workspace.mkdirs(); // ensure it exists.
    }
    return getSscmExe(SSCMUtils.workspaceToNode(workspace), env, listener);
  }

  /**
   * See "public String getGitExe(Node builtOn, EnvVars env, TaskListener listener)" Line 848 @ GitSCM.java
   * @param builtOn
   * @param env
   * @param listener
   * @return
   */
  public String getSscmExe(Node builtOn, EnvVars env, TaskListener listener)
  {
    SurroundTool tool = resolveSscmTool(listener);
    if(builtOn != null) {
      try {
        tool = tool.forNode(builtOn, listener);
      } catch(IOException | InterruptedException e) {
        listener.getLogger().println("Failed to get sscm executable");
      }
    }
    if(env != null) {
      tool = tool.forEnvironment(env);
    }

    return tool.getSscmExe();
  }
}
