package com.roonin.rargrep;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

public class RarGrep{

  public RarGrep(){}

  public void find(String regex, boolean quiet,
                  boolean lineNumbers, boolean recursive,
                  boolean reverseSorting, boolean timeSorting,
                  int tailingLines, List<String> filepatterns){

    for(String filePattern : filepatterns){
      ArrayList<File> files = new ArrayList<File>(processPattern(filePattern, recursive));
      if(timeSorting){
        Collections.sort(files, new Comparator<File>(){
          public int compare(File f1, File f2){
            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
          }
        });
      }
      if(reverseSorting){
        Collections.reverse(files);
      }
      for (File file : files){
        if(file.getName().endsWith(".rar")){
          processRarFile(file, regex, quiet, lineNumbers, tailingLines);
        }else{
          processAsTextFile(file, regex, quiet, lineNumbers, tailingLines);
        }
      }
    }
  }

  private Collection<File> processPattern(String filePattern, boolean recursive){
    if(filePattern.endsWith(File.separator)){
      return new ArrayList<File>();
    }

    String separator = (separator = File.separator).equals("\\") ? "\\\\" : separator;
    LinkedList <String> path = new LinkedList<String>(Arrays.asList(filePattern.split(separator))){{
      addFirst("."+File.separator);
    }};

    String pattern = path.removeLast();
    return FileUtils.listFiles(new File(StringUtils.join(path,File.separator)),
                               new WildcardFileFilter(pattern),
                               (recursive ? TrueFileFilter.INSTANCE : null));
  }

  private boolean processRarFile(File file, String regex,
                                boolean quiet, boolean lineNumbers,
                                int tailingLines){
    boolean found = false;
    try{
      Archive arch = new Archive(file);
      if(arch.isEncrypted()){
        System.out.println("File is encrypted, can't extract");
        return false;
      }
      Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
      for(FileHeader fh = null;(fh = arch.nextFileHeader()) != null;){
        if (fh.isEncrypted()) {
          System.out.println("File is encrypted, can't extract: "+fh.getFileNameString());
        }
        if(processFile(file.toString()+">>"+fh.getFileNameString(),arch.getInputStream(fh), pattern,
                      quiet, lineNumbers, tailingLines)){
          found = true;
        }
      }
    }catch (RarException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return found;
  }

  private boolean processAsTextFile(File file, String regex,
                                    boolean quiet, boolean lineNumbers,
                                    int tailingLines){
    try{
      return processFile(file.toString(), new FileInputStream(file),
                  Pattern.compile(regex, Pattern.CASE_INSENSITIVE|Pattern.DOTALL),
                  quiet, lineNumbers, tailingLines);
    }catch(IOException e){
      e.printStackTrace();
    }
    return false;
  }

  private boolean processFile(String filename, InputStream is,
                              Pattern pattern, boolean quiet,
                              boolean lineNumbers, int tailingLines)
  throws IOException{
    LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
    LinkedList<String> stack = new LinkedList<String>();
    boolean found = false;

    for(String line = null; (line = reader.readLine()) != null;){
      addToStack(stack, line, tailingLines);
      if(pattern.matcher(line).find()){
        StringBuffer sb = new StringBuffer(filename).append(":");
        if(lineNumbers){
          sb.append(reader.getLineNumber()).append(":");
        }
        if(!quiet){
          if(tailingLines > 0){
            sb.append("\n");
          }
          sb.append(StringUtils.join(stack, "\n"));
          if(tailingLines > 0){
            sb.append("\n---------------------------------\n");
          }
        }
        System.out.println(sb.toString());
        found = true;
      }
    }
    return found;
  }

  private void addToStack(LinkedList<String> stack, String str, int maxSize){
    if(stack.size() >= maxSize){
      stack.poll();
    }
    stack.add(str);
  }
}
