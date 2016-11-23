package hudson.scm;

import java.util.Date;

public final class SurroundSCMRevisionState extends SCMRevisionState {

  private final Date  buildDate;
  private final int   buildNumber;
  
	public SurroundSCMRevisionState(Date buildDate, int buildNumber) {

		this.buildDate = new Date(buildDate.getTime());
    this.buildNumber = buildNumber;
	}
	
	public Date getDate() {
		return new Date(buildDate.getTime());
	}
  
  public int getBuildNumber() {
    return buildNumber;
  }
}
