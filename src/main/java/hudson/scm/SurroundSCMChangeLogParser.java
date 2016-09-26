package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.SurroundSCMChangeLogSet.SurroundSCMChangeLogSetEntry;
import org.xml.sax.SAXException;

import java.io.*;

public class SurroundSCMChangeLogParser extends ChangeLogParser {

  @Override
  public ChangeLogSet<? extends Entry> parse(AbstractBuild build,
                                             File changelogFile) throws IOException, SAXException {

    //open the changelog File
    SurroundSCMChangeLogSet cls = new SurroundSCMChangeLogSet(build);
    String line = null;
    BufferedReader br = null;

    boolean foundAnItem=false;
    InputStreamReader is = new InputStreamReader(new FileInputStream(changelogFile), "UTF-8");
    try{
      br = new BufferedReader(is);
      while ((line = br.readLine())!=null)
      {
        //skip the total line
        if (!foundAnItem){
          foundAnItem=true;
          continue;
        }

        //check for count first
        if (line.startsWith("total-0"))
          break; //there are none, abandon ship

        //get the path
        int end = line.indexOf(">");

        //sanity check
        if (end<=0)
          break;

        String path = line.substring(1,end);
        line = line.substring(end+1);

        //get the name
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String name = line.substring(1,end);
        line = line.substring(end+1);
        name = path.concat("/").concat(name);

        //get the version
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String version = line.substring(1,end);
        line = line.substring(end+1);

        //get the action
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String action = line.substring(1,end);
        line = line.substring(end+1);

        //get the date
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String date = line.substring(1,end);
        line = line.substring(end+1);

        //get the comment
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String comment = line.substring(1,end);
        line = line.substring(end+1);

        //get the user
        end = line.indexOf(">");
        //sanity check
        if (end<=0)
          break;
        String userName = line.substring(1,end);
        line = line.substring(end+1);

        SurroundSCMChangeLogSetEntry next = new SurroundSCMChangeLogSetEntry(name,comment,version,action,date,cls ,userName);
        if (!cls.addEntry(next)) //terminate on error
          break;

      }

    }catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    finally {
      if(br != null)
        br.close();
    }

    return cls;
  }



}

