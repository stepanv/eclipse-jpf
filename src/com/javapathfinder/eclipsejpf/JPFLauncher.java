package com.javapathfinder.eclipsejpf;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;

/*
 * @throws IOException if the property file could not be loaded.
 */
public abstract class JPFLauncher{
  public static final String DEFAULT_SITE_PROPERTIES_PATH = System.getProperty("user.home") + File.separator + ".jpf" + File.separator + "site.properties";

  protected PrintWriter error;

  protected void launch(File file){
    PrintWriter errorStream = getErrorStream();

    File siteProperties = new File(getSitePropertiesPath(""));
    if (!siteProperties.exists()){
      errorStream.println("site.properties file: \"" + siteProperties.getPath() + "\" does not exist.");
      return;
    }
    if (!siteProperties.isFile()) {
      errorStream.println("site.properties file: \"" + siteProperties.getPath() + "\" is a directory.");
      return;
    }

    Properties props = new Properties();
    try {
      props.load(new FileInputStream(siteProperties));
    } catch (IOException ex) {
      //We already checked, this can't happen
      ex.printStackTrace();
    }
    String coreProperty = props.getProperty("jpf.core");

    if (coreProperty == null || coreProperty.isEmpty()){
      errorStream.println("Property: \"jpf.core\" is not defined in: " + siteProperties.getPath());
      return;
    }

    File corePath = new File(coreProperty);
    File runJPFJar = new File(corePath, "RunJPF.jar");
    if (!runJPFJar.isFile()){
      errorStream.println("RunJPF.jar not found at: " + runJPFJar.getPath());
    }

    //Create command
    StringBuffer command = new StringBuffer("java ");
    
    //Add the host vm args
    String vm_args = getVMArgs(null);
    if (vm_args != null && !vm_args.isEmpty())
      command.append(vm_args).append(" ");
    
    //Point to the RunJPF jar
    command.append("-jar ");
    command.append(runJPFJar.getAbsolutePath());
    command.append(" ");

    //Add the JPF args
    String args = getArgs(null);
    if (args != null && !args.isEmpty())
      command.append(args).append(" ");

    //Define site.properties location if it's not the default path
    if ( new File(DEFAULT_SITE_PROPERTIES_PATH).equals(siteProperties) == false){
      command.append("+site=").append(siteProperties.getAbsolutePath()).append(" ");
    }

    //Define port if its available
    int port = getPort();
    if (port > -1)
      command.append("+shell.port=").append(port).append(" ");

    //Add the property file path
    command.append(file.getAbsolutePath());

    //Startup the JPF process
    Thread jpfKiller = null;
    try {
      final Process jpf = Runtime.getRuntime().exec(command.toString());

      //Make sure that we kill the thread if this VM exists. (Most probably from eclipse closing)
      jpfKiller = new Thread(){
        @Override
        public void run(){
          jpf.destroy();
        }
      };

      Runtime.getRuntime().addShutdownHook(jpfKiller);
      PrintWriter outputStream = getOutputStream();
      outputStream.println("Executing command: " + command.toString());
      outputStream.println("------------------------ start JPF with config file: " + file.getName());
      new IORedirector(jpf.getInputStream(), outputStream).start();
      new IORedirector(jpf.getErrorStream(), errorStream).start();
      if ( port > -1 )
        new ShellListener().start();
      jpf.waitFor();
      outputStream.println("------------------------  exit JPF");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }finally{
      if (jpfKiller != null)
        Runtime.getRuntime().removeShutdownHook(jpfKiller);
    }
  }

  protected abstract String getVMArgs(String def);
  protected abstract String getArgs(String def);
  protected abstract String getSitePropertiesPath(String def);
  protected abstract int getPort();
  protected abstract PrintWriter getOutputStream();
  protected abstract PrintWriter getErrorStream();
  protected abstract void gotoSource(String filepath, int line);

  class ShellListener extends Thread{

    @Override
    public void run() {
      Socket socket = null;
      //We aren't sure when the port is going to open (if it ever does) so keep
      //on trying until we get a hit.
      while (socket == null) {
        try {
          socket = new Socket(InetAddress.getLocalHost(), getPort());
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
          final String[] location = input.split(":");
          gotoSource(location[0], new Integer(location[1]));
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

