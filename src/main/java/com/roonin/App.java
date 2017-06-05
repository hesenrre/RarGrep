package com.roonin;

import java.util.*;
import com.roonin.rargrep.RarGrep;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ){
      CommandLineParser parser = new DefaultParser();
      Options options = new Options();
      options.addOption("q", "quiet", false, "Quiet mode just prints where the pattern was found");
      options.addOption("n", false, "Line number where the pattern was found");
      options.addOption("tl", "tail-lines", true, "Number of tailing lines you want to see");
      options.addOption("R", "recursive", false, "Recursive pattern search on subdirectories");
      options.addOption("r", false, "Reverse sorting");
      options.addOption("t", false, "Time sorting");

      CommandLine line = null;
      HelpFormatter formatter = new HelpFormatter();
      int tailLines = 0;
      try{
        line = parser.parse(options, args);
        if(line.hasOption("tl")){
          tailLines = Integer.parseInt(line.getOptionValue("tl"));
        }
      }catch(ParseException e){
        System.out.println(e.getMessage());
        formatter.printHelp( "java --jar RarGrep.jar [options] [files...]", options );
        return;
      }catch(NumberFormatException e){
        System.out.println("tail-lines must be a number");
        formatter.printHelp( "java --jar RarGrep.jar [options] [files...]", options );
        return;
      }

      if(line.getArgList().size() < 2){
        formatter.printHelp( "java --jar RarGrep.jar [options] [files...]", options );
        return;
      }

      List <String> parsedArgs = line.getArgList();
      new RarGrep().find(parsedArgs.remove(0), line.hasOption("q"),
                          line.hasOption("n"), line.hasOption("R"),
                          line.hasOption("r"), line.hasOption("t"),
                          tailLines,parsedArgs);
    }
}
