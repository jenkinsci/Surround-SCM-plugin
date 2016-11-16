package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.SurroundSCMChangeLogSet.SurroundSCMChangeLogSetEntry;
import hudson.tasks.Mailer;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public final class SurroundSCMChangeLogSet extends ChangeLogSet<SurroundSCMChangeLogSetEntry>
{
  private Collection<SurroundSCMChangeLogSetEntry> changes;

  protected SurroundSCMChangeLogSet(Run<?, ?> run, RepositoryBrowser<?> browser) {
    super(run, browser);
    changes = new ArrayList<SurroundSCMChangeLogSetEntry>();
  }

  @Deprecated
  protected  SurroundSCMChangeLogSet(AbstractBuild<?, ?> build) {
    super(build);
    changes = new ArrayList<SurroundSCMChangeLogSetEntry>();
  }

  @Override
  public Iterator<SurroundSCMChangeLogSetEntry> iterator() {
    return changes.iterator();
  }

  @Override
  public boolean isEmptySet() {
    return changes.isEmpty();
  }

  public boolean addEntry(SurroundSCMChangeLogSetEntry e) {
    return changes.add(e);
  }

  public static class SurroundSCMChangeLogSetEntry extends ChangeLogSet.Entry {
    private String comment;
    private String affectedFile;
    private String version;
    private String date;
    private User user;
    private String action; //default is edit

    public SurroundSCMChangeLogSetEntry(String filePath, String comment, String version, String action, String date,
                                        ChangeLogSet parent,  String userName, String email )
    {
      this.affectedFile = filePath;
      this.comment = comment;
      this.version = version;
      this.action = action;
      this.date = date;
      this.user = User.get(userName);

      // Check to see if we were able to parse an email address...
      if(!email.isEmpty())
      {
        Mailer.UserProperty userMailer = user.getProperty(Mailer.UserProperty.class);
        // Now... if the Jenkin's "User" either doesn't have an email address, or they have never explicitly
        // set the email address, supply our own email address for the user.
        if(userMailer == null || !userMailer.hasExplicitlyConfiguredAddress())
        {
          userMailer = new Mailer.UserProperty(email);
          try {
            user.addProperty(userMailer);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      setParent(parent);
    }

    @Deprecated
    public SurroundSCMChangeLogSetEntry(String filePath, String comment, String version, String action, String date,
                                        ChangeLogSet parent,  String userName)
    {
      this.affectedFile = filePath;
      this.comment = comment;
      this.version = version;
      this.action = action;
      this.date = date;
      this.user = User.get(userName);
      setParent(parent);
    }

    @Override
    public String getMsg() {
      String format = "File: %s Action: %s Version: %s Comment: %s";
      return String.format(format, affectedFile, action, version, comment);

    }

    @Override
    public String getMsgAnnotated() {
      return affectedFile;
    }

    public String getVersion() {
      return version;
    }

    public String getName() {
      return affectedFile;
    }

		public String getAffectedFile(){
			return affectedFile;
		}

    public String getAction() {
      return action;
    }

    public String getComment() {
      return comment;
    }

    public String getDate() {
      return date;
    }

    @Override
    public Collection<String> getAffectedPaths() {
      Collection<String> col = new ArrayList<String>();
      col.add(affectedFile);
      return col;
    }

    @Override
    public User getAuthor() {
      if (user ==  null)
        return User.getUnknown();
      return user;
    }

    @Exported
    public EditType getEditType() {
      if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("remove"))
      {
        return EditType.DELETE;
      }
      if(action.equalsIgnoreCase("add")) {
        return EditType.ADD;
      }
      return EditType.EDIT;
    }

    @Exported
    public String getPath() {
      return affectedFile;
    }
  }
}
