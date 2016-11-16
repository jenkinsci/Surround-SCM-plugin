package hudson.scm;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.*;
import hudson.model.*;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public final class SurroundSCM extends SCM {

  /*--------------------- INNER CLASS ------------------------------------------------------------*/

  public static class SurroundSCMDescriptor extends SCMDescriptor<SurroundSCM> {

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
      return new StandardUsernameListBoxModel().withEmptySelection().withAll(availableCredentials(owner, new EnvVars().expand(source)));
    }

    /**
     * This populates the rsaKeyFileId dropdown with a list of 'FileCredentials' that could be used.
     *
     * @return  Returns a list of FileCredential objects that have been configured.
     */
    public ListBoxModel doFillRsaKeyFileIdItems(@AncestorInPath Job<?, ?> owner, @QueryParameter String source) {
      if(owner == null || !owner.hasPermission(Item.EXTENDED_READ)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(availableFileCredentials(owner, new EnvVars().expand(source)));
    }
  }
  /*--------------------- END INNER CLASS ------------------------------------------------------------*/

  // if there are > changesThreshold changes, that it's build now incomparable
  // if there are < changesThreshold changes, but > 0 changes, then it's significant
  private static transient final int changesThreshold = 1;
  private static transient final int pluginVersion = 9;

  // config options
  private String rsaKeyPath;
  private String server;
  private String serverPort;
  private String branch ;
  private String repository;

  private String credentialsId;
  private String rsaKeyFileId;

  // TODO: Review if this is needed.
  private String sscm_tool_name;

  private boolean bIncludeOutput;

  /**
   * @deprecated This was used to store the local path to the Surround SCM Executable. It is no longer needed since we moved
   * to using the SurroundTool.
   */
  private String surroundSCMExecutable;

  /**
   * @deprecated This was used to store the username used to connect to Surround. For legacy support reasons
   * we are leaving the variable here so people can have a smooth upgrade. However if they edit their project
   * they will be forced to use the new Credentials interface.
   */
  private String userName ;

  /**
   * @deprecated This was used to store the password used to connect to Surround. For legacy support reasons
   * we are leaving the variable here so people can have a smooth upgrade. However if they edit their project
   * they will be forced to use the new Credentials interface.
   */
  private String password ;

  //getters
  public String getRsaKeyPath() {
    return rsaKeyPath;
  }

  public String getServer() {
    return server;
  }

  public String getServerPort() {
    return serverPort;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public String getBranch() {
    return branch;
  }

  public String getRepository() {
    return repository;
  }

  public boolean getIncludeOutput() {
    return bIncludeOutput;
  }

  public String getCredentialsId() { return credentialsId; }

  public String getRsaKeyFileId() { return rsaKeyFileId; }

  //DataBoundSetters

  //TODO: @DataBoundSetter
  public void setIncludeOutput(boolean includeOutput) {
    this.bIncludeOutput = includeOutput;
  }

  @DataBoundSetter
  public void setRsaKeyPath(String rsaKeyPath) {
    this.rsaKeyPath = Util.fixEmptyAndTrim(rsaKeyPath);
  }

  @DataBoundSetter
  public void setRsaKeyFileId(String rsaKeyFileId) { this.rsaKeyFileId = Util.fixEmptyAndTrim(rsaKeyFileId); }

  /**
   * Singleton descriptor.
   */
  @Extension
  public static final SurroundSCMDescriptor DESCRIPTOR = new SurroundSCMDescriptor();

  private static final String SURROUND_DATETIME_FORMAT_STR = "yyyyMMddHHmmss";
  private static final String SURROUND_DATETIME_FORMAT_STR_2 = "yyyyMMddHH:mm:ss";

  @DataBoundConstructor
  public SurroundSCM(String server, String serverPort, String branch, String repository, String credentialsId)
  {
    this.rsaKeyPath = null;
    this.rsaKeyFileId = null;

    this.server = Util.fixEmptyAndTrim(server);
    this.serverPort = Util.fixEmptyAndTrim(serverPort);
    this.branch = Util.fixEmptyAndTrim(branch);
    this.repository = Util.fixEmptyAndTrim(repository);
    this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    this.bIncludeOutput = true; // Leaving this here for future functionality.

    this.userName = null;
    this.password = null;
    this.surroundSCMExecutable = null;
  }

  @Deprecated
  public SurroundSCM(String server, String serverPort, String userName,
                     String password, String branch,  String repository)
  {
    this(server, serverPort, branch, repository, null);

    this.userName = Util.fixEmptyAndTrim(userName);
    this.password = Util.fixEmptyAndTrim(password);
  }

  @Deprecated
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch,  String repository)
  {
    this(server, serverPort, branch, repository, null);
    this.rsaKeyPath = Util.fixEmptyAndTrim(rsaKeyPath);
    this.userName = Util.fixEmptyAndTrim(userName);
    this.password = Util.fixEmptyAndTrim(password);
  }

  @Deprecated
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch, String repository, String surroundSCMExecutable,
                     boolean includeOutput)
  {
    this(server, serverPort, branch, repository, null);
    this.rsaKeyPath = Util.fixEmptyAndTrim(rsaKeyPath);
    this.userName = Util.fixEmptyAndTrim(userName);
    this.password = Util.fixEmptyAndTrim(password);
    this.surroundSCMExecutable = Util.fixEmptyAndTrim(surroundSCMExecutable);
    this.bIncludeOutput = includeOutput;
  }

  /**
   * @deprecated Deprecated as of release v9, added option to include // exclude output.
   */
  public SurroundSCM(String rsaKeyPath, String server, String serverPort, String userName,
                     String password, String branch, String repository, String surroundSCMExecutable)
  {
    this(rsaKeyPath, server, serverPort, userName, password, branch, repository, surroundSCMExecutable, true);
  }

  @Deprecated
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



    double countChanges = determineChangeCount(project, launcher,  listener, lastBuild, now, temporaryFile, workspace);

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
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-d".concat(workspace.getRemote()));
    cmd.add("-r");
    cmd.add("-s" + scm_datetime_formatter.format(currentDate));
    if(!bIncludeOutput)
    {
      cmd.add("-q");
    }
    cmd.add(getServerConnectionArgument(build.getParent(), environment, workspace));
    cmd.addMasked(getUserPasswordArgument(build.getParent(), environment));

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
        captureChangeLog(build, launcher, workspace, listener, lastBuildDate, currentDate, changelogFile, environment);
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

  private boolean captureChangeLog(@Nonnull Run<?, ?> build, Launcher launcher, FilePath workspace,
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
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-r");

    cmd.add(getServerConnectionArgument(build.getParent(), env, workspace));
    cmd.addMasked(getUserPasswordArgument(build.getParent(), env));

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

  private double determineChangeCount(Job<?,?> project, Launcher launcher, TaskListener listener, Date lastBuildDate,
                                      Date currentDate, File changelogFile, FilePath workspace) throws IOException, InterruptedException
  {
    SimpleDateFormat scm_datetime_formatter = new SimpleDateFormat(SURROUND_DATETIME_FORMAT_STR);

    double changesCount = 0;
    if (server != null )
      listener.getLogger().println("in determine Change Count server: "+server);

    String dateRange = scm_datetime_formatter.format(lastBuildDate);
    dateRange = dateRange.concat(":");
    dateRange = dateRange.concat(scm_datetime_formatter.format(currentDate));

    EnvVars env = project.getEnvironment(SSCMUtils.workspaceToNode(workspace),  listener);

    ArgumentListBuilder cmd = new ArgumentListBuilder();
    cmd.add(getSscmExe(workspace, listener, null));
    cmd.add("cc");
    cmd.add("/");
    cmd.add("-d".concat(dateRange));
    cmd.add("-b".concat(branch));
    cmd.add("-p".concat(repository));
    cmd.add("-r");
    cmd.add(getServerConnectionArgument(project, env, workspace));
    cmd.addMasked(getUserPasswordArgument(project, env));

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
      } catch(IOException e ) {
        listener.getLogger().println("Failed to get sscm executable");
      } catch(InterruptedException e) {
        listener.getLogger().println("Failed to get sscm executable");
      }
    }
    if(env != null) {
      tool = tool.forEnvironment(env);
    }

    return tool.getSscmExe();
  }

  /**
   * Creates the Username // Password argument taking into account that this might be an 'upgraded' plugin
   * that has not yet been modified to use hte more  secure UsernamePasswordCredentials.
   *
   * It first checks to see if it can create a username // password from passed in credentials, then checks to see
   * if there is a legacy username // password saved.
   *
   * @param owner Used to lookup user credentials
   * @param env   Used to lookup user credentials
   * @return      Returns the fleshed out username // password "-yUsername:Password" parameter.
   * @throws IOException
   */
  private String getUserPasswordArgument(Job<?,?> owner, EnvVars env) throws IOException {
    String result = null;
    StandardUsernameCredentials credentials = getCredentials(owner, env);
    if(credentials != null && credentials instanceof UsernamePasswordCredentials)
    {
      UsernamePasswordCredentials upc = (UsernamePasswordCredentials) credentials;

      result = String.format("-y%s:%s", upc.getUsername(), upc.getPassword().getPlainText());
    }
    else if(userName != null && !userName.isEmpty())
    {
      if(password != null) {
        result = String.format("-y%s:%s", userName, password);
      } else {
        result = String.format("-y%s", userName);
      }
    }
    else
    {
      throw new IOException(String.format("Failed to find currently defined username//password credential. [%s] %s",
        getCredentialsId(), credentials != null ? CredentialsNameProvider.name(credentials) : "Failed to find credential ID"));
    }
    return result;
  }

  /**
   * Creates the "Server Connection Information" argument used for Surround SCM CLI commands.  Automatically
   * determines if it should pass in a path to an RSA Key File or use the Server // Port setting.
   *
   * @param owner     Used to lookup fileCredentials
   * @param env       Used to lookup fileCredentials
   * @param workspace Used to store any potential key retrieved from fileCredentials
   * @return          Fleshed out "-z..." parameter with either server:port or rsaKeyPath.
   */
  private String getServerConnectionArgument(Job<?, ?> owner, EnvVars env, FilePath workspace) {
    String result = null;
    String rsaKeyPath = getRSAKeyFilePath(owner, env, workspace);
    if(rsaKeyPath != null && !rsaKeyPath.isEmpty())
    {
      result = String.format("-z%s", rsaKeyPath);
    }
    else
    {
      result = String.format("-z%s:%s", getServer(), getServerPort());
    }

    return result;
  }

  private static List<? extends StandardUsernameCredentials> availableCredentials(Job<?,?> owner, String source) {
    return CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, owner, null, URIRequirementBuilder.fromUri(source).build());
  }

  private static List<? extends FileCredentials> availableFileCredentials(Job<?,?> owner, String source) {
    return CredentialsProvider.lookupCredentials(FileCredentials.class, owner, null, URIRequirementBuilder.fromUri(source).build());
  }

  @CheckForNull
  public StandardUsernameCredentials getCredentials(Job<?,?> owner, EnvVars env) {
    if(credentialsId != null) {
      for (StandardUsernameCredentials c : availableCredentials(owner, env.expand("sscm://" + server + ":" + serverPort))) { // TODO: This seems like a royal hack.
        if(c.getId().equals(credentialsId)) {
          return c;
        }
      }
    }
    return null;
  }

  @CheckForNull
  public FileCredentials getFileCredentials(Job<?,?> owner, EnvVars env) {
    if(rsaKeyFileId != null) {
      for(FileCredentials fc : availableFileCredentials(owner, env.expand(String.format("sscm://%s:%s", server, serverPort)))) { // TODO: This seems like a royal hack
        if(fc.getId().equals(rsaKeyFileId)) {
          return fc;
        }
      }
    }
    return null;
  }

  /**
   * Checks to see if there is an existing stored 'fileCredential' for the rsaKeyFileId. If there is, it will write out
   * that file to the remote computer's workspace and return a path to it on the remote computer.
   *
   * @param owner     Used to lookup the fileCredential
   * @param env       Used to expand the possible sscm:// url with build variables.
   * @param workspace Used as the destination for the temp file to be created to be used for the build.
   * @return  If there an RSAKeyFile was retrieved from the fileCredentials, this returns the path to the file. Otherwise
   *          it returns null.
   */
  private String populateRSAKeyFile(Job<?, ?> owner, EnvVars env, @Nullable FilePath workspace) {
    String result = null;
    FileCredentials fc = getFileCredentials(owner, env);
    if(fc != null && workspace != null) {
      try {
        FilePath rsaFilePath = workspace.createTempFile("RSAKeyFile",".xml");
        rsaFilePath.copyFrom(fc.getContent());
        result = rsaFilePath.getRemote();
      } catch (IOException e) {
        Logger.getLogger(SurroundSCM.class.toString()).log(Level.SEVERE,
          String.format("Found RSA Key File by ID [%s], however failed to retrieve file to destination machine.\n" +
            "Error Message: %s", rsaKeyFileId, e.toString()));
      } catch (InterruptedException e) {
        Logger.getLogger(SurroundSCM.class.toString()).log(Level.SEVERE,
          String.format("Exception while attempting to retrieve RSA Key File to destination machine. Error message: %s", e.toString()));
      }
    }

    return result;
  }

  /**
   * This first checks the rsaKeyFileId and then the rsaKeyPath.  If it can find a path to an RSA Key file from either
   * of these items it will return the path to the RSA Key file on the remote machine (or as defined via rsaKeyPath)
   * If no RSA Key file is found, it returns null
   *
   * @param owner     Used to lookup possible fileCredentials
   * @param env       Used as part of the lookup for fileCredentials
   * @param workspace Used as a destination for any RSA Key File retrieved from fileCredentials
   * @return  Returns either the path to an RSA Key File, or null indicating no RSA Key File.
   */
  private String getRSAKeyFilePath(Job<?, ?> owner, EnvVars env, FilePath workspace)
  {
    String result = null;
    if(rsaKeyFileId != null && !rsaKeyFileId.isEmpty())
    {
      result = populateRSAKeyFile(owner, env, workspace);
    }

    if(result == null && rsaKeyPath != null && !rsaKeyPath.isEmpty())
    {
      result = rsaKeyPath;
    }

    return result;
  }
}
