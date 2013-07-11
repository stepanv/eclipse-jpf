/**
 * Copyright (C) 2006 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration
 * (NASA).  All Rights Reserved.
 *
 * This software is distributed under the NASA Open Source Agreement
 * (NOSA), version 1.3.  The NOSA has been approved by the Open Source
 * Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
 * directory tree for the complete NOSA document.
 *
 * THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
 * KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
 * LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
 * SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 * A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
 * THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
 * DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
 */
package gov.nasa.runjpf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * The <code>JPFLauncher</code> class is used by both the netbeans-jpf and
 * eclipse-jpf projects to launch a jpf process from a *.jpf file. This class
 * is meant to be subclassed by applications that launch a jpf process.
 * @see #launch(java.io.File) 
 * @author Sandro Badame
 */
public abstract class JPFLauncher {

  /**
   * The system dependent default location to find the site.properties file. The
   * value is defined as &lt;home directory&gt;/.jpf/site.properties
   */
  public static final String DEFAULT_SITE_PROPERTIES_PATH = getSitePropertiesPath();

  /**
   * The default port that is used to open communication with the Shell.<br>
   * The value is 4242
   */
  public static final int DEFAULT_PORT = 4242;

  /**
   * Saved Site Core directory
   */
  private File lastSiteProperties;
  private long lastSitePropMod;
  private File siteCoreDir; 
  
  PrintWriter errorStream = null;

  static String getSitePropertiesPath() {
    File userHome = new File(System.getProperty("user.home"));
    
    File siteDir = new File(userHome, ".jpf");
    if (!siteDir.isDirectory()){
      // Windows doesn't easily allow dot names
      File alternateSiteDir = new File(userHome,"jpf");
      if (alternateSiteDir.isDirectory()){
        siteDir = alternateSiteDir;
      }
    }

    File siteProperties = new File(siteDir, "site.properties");
    
    return siteProperties.getAbsolutePath(); 
  }
  
  /**
   * Launch a new process to run jpf with specified *.jpf file. This method
   * will report all errors to the stream returned by getErrorStream(), no
   * exceptions should be thrown by this method.
   * <p>
   * This method runs jpf in its own process and returns the process immediately.
   * <p>
   * The command launched will contain the following pattern:
   * <p><code>
   * java <i>&lt;{@link #getVMArgs(java.lang.String)}&gt;</i>
   * -jar <i>&lt;path to RunJPF.jar defined by
   * jpf.core property in the {@link #getSiteProperties()}</a>&gt;&lt;{@link #getArgs(java.lang.String)}&gt;
   * &lt;if a non default
   * site.properties is used then +site=&lt;{@link #getSiteProperties()}&gt;&gt;
   * </i>+shell.port=<i>&lt;{@link #getPort()}&gt; &lt;file.getAbsolutePath()&gt;
   * </i></code>
   *
   * @param file the selected *.jpf configuration file to be used.
   * @return the process running jpf
   */
  protected Process launch(File file, File workingDir){

    errorStream = getErrorStream();

    String path = getSiteProperties();
    if (path == null || path.isEmpty()){
      printError("getSiteProperties() is null or empty, using default path: " + DEFAULT_SITE_PROPERTIES_PATH);
      path = DEFAULT_SITE_PROPERTIES_PATH;
    }

    File siteProperties = new File(path);
    if (!siteProperties.isFile()){
      printError("site.properties file: \"" + siteProperties.getPath() + "\" does not exist.");
      return null;
    }
    
    // check if preferences were changed or the file was modified
    if (!siteProperties.equals(lastSiteProperties) || siteProperties.lastModified() > lastSitePropMod){
      String coreProperty = JPFSiteUtils.getMatchFromFile(path, "jpf-core");
      if (coreProperty == null || coreProperty.isEmpty()) {
        printError("Property: \"jpf-core\" is not defined in: " + siteProperties.getPath());
        return null;
      }

      siteCoreDir = new File(coreProperty);
      if (!siteCoreDir.isDirectory()) {
        printError("jpf-core specification in " + path + " not a valid directory: " + siteCoreDir);
        return null;
      }

      lastSiteProperties = siteProperties;
      lastSitePropMod = siteProperties.lastModified();
    }
    

    File runJPFJar = new File(siteCoreDir, "build" + File.separatorChar + "RunJPF.jar");
    if (!runJPFJar.isFile()){
      printError("RunJPF.jar not found at: " + runJPFJar.getPath());
    }

    ArrayList<String> commandList = new ArrayList<String>();

    //Create command
    commandList.add("java");
    
    //Add the host vm args
    String vm_args = getVMArgs(null);
    if (vm_args != null && !vm_args.isEmpty()){
      commandList.add(vm_args);
    }
    
    //Point to the RunJPF jar
    commandList.add("-jar");
    commandList.add(runJPFJar.getAbsolutePath());

    //Add the JPF args
    String args = getArgs(null);
    if (args != null && !args.isEmpty()){
        for (String string : args.split(" ")) {
            commandList.add(string);
        }
    }

    //Define site.properties location if it's not the default path
    if ( new File(DEFAULT_SITE_PROPERTIES_PATH).equals(siteProperties) == false){
      commandList.add("+site="+siteProperties.getAbsolutePath());
    }

    //Define port if its available
    int port = getPort();
    if (port > -1){
      commandList.add("+shell.port="+port);
    }

    //Add the property file path
    commandList.add(file.getAbsolutePath());

    //Startup the JPF process
    try {
      String[] cmdArgs = commandList.toArray(new String[commandList.size()]);
      // we inherit the parent proc environment
      Process jpf = Runtime.getRuntime().exec(cmdArgs, null, workingDir);
      PrintWriter outputStream = getOutputStream();
      if (outputStream != null){
        StringBuffer buff = new StringBuffer();
        for (String string : commandList) {
          buff.append(string).append(" ");
        }
        outputStream.println("Executing command: " + buff.toString());
        new IORedirector(jpf.getInputStream(), outputStream).start();
        new IORedirector(jpf.getErrorStream(), errorStream).start();
      }
      if ( port > -1 )
        new ShellListener(port).start();

      return jpf;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  //Safe way to print output...
  private void printError(String msg){
    if (errorStream != null) 
      errorStream.println(msg);
  }

  /**
   * Returns the arguments for the host VM.
   * @param def the default return value if no VMArgs exist.
   * @return the text that will be placed in the command to launch JPF
   *         ex.) "java &lt;text placed here&gt; -jar ...<br>
   *         or def if non exist.
   * @see #launch(java.io.File) 
   */
  protected abstract String getVMArgs(String def);

   /**
   * Returns the arguments for JPF. These should not include port or site properties
   * @param def the default return value if no JPF arguments exist.
   * @return the text that will be placed in the command to launch JPF
   *         ex.) "java &lt;vm args&gt; -jar &lt;path to RunJPF.jar&gt; &lt;placed
             here&gt;...<br>
   *         or def if non exist.
   * @see #launch(java.io.File)
   */
  protected abstract String getArgs(String def);

  /**
   * Returns a system dependent absolute path to the site properties file.
   * @return Returns a system dependent absolute path to the site properties file.
   * @see #launch(java.io.File) 
   */
  protected abstract String getSiteProperties();

  /**
   * Returns the port that should be opened by the shell (If one were to be
   * created by JPF) for Shell<->JPFLauncher implementor (Typically IDEs like
   * Eclipse and NetBeans) communication.
   *
   * @return the port that should be opened or a negative number if no port
   *         communication is requested. Returns DEFAULT_PORT
   * @see #launch(java.io.File)
   */
   protected int getPort(){
    return DEFAULT_PORT;
   }

  /**
   * Returns an PrintWriter to print JPFLauncher and JPF process output.
   *
   * @return the PrintWriter that will be used to print output from JPFLauncher
   * and the JPF process started. Can be null if no such output is needed.
   */
  protected abstract PrintWriter getOutputStream();

  /**
   * Returns the PrintWriter to print JPFLauncher and JPF process error output.
   *
   * @return the PrintWriter that will be used to print error output from JPFLauncher
   * and the JPF process started. Can be null if no such output is needed.
   */
  protected abstract PrintWriter getErrorStream();

  /**
   * If a shell has been created by JPF, this method is called when the shell
   * requests that a source file and line be shown.
   * if getPort() returns a negative value, it is safe to say that this
   * method will never be called.
   * @param filepath the absolute path to the pertinent file.
   * @param line the 0 indexed pertinent line in the file.
   */
  protected abstract void gotoSource(String filepath, int line);

   private class ShellListener extends Thread{

    int port = -1;
    boolean keepTrying = true;

    public ShellListener(int port){
      if (port < 0 ){
        throw new IllegalArgumentException("port cannot be a negative value");
      }
      this.port = port;
    }

    @Override
    public void run() {
      Socket socket = null;
      //We aren't sure when the port is going to open (if it ever does) so keep
      //on trying until we get a hit.
      while (keepTrying && socket == null) {
        try {
          socket = new Socket((String)null, port);
        } catch (IOException io) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
        }
      }
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String input = null;
        while ((input = reader.readLine()) != null) {
          if (input.startsWith("[LINK]")){
            //remove "[LINK]"
            input = input.substring(6);
            final String[] location = input.split(":");
            gotoSource(location[0], new Integer(location[1]));
          }
        }
      } catch (SocketException se){
        //Probably nothing to worry about. Just the socket closing.
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
}
}

//Funnels the output from the process into the console
class IORedirector extends Thread {

  private PrintWriter out;
  private BufferedReader in;

  public IORedirector(final InputStream in, final PrintWriter out) {
    this.in = new BufferedReader(new InputStreamReader(in));
    this.out = out;
  }

  @Override
  public void run() {
    try {
      String s;
      while ((s = in.readLine()) != null) {
        out.println(s);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
