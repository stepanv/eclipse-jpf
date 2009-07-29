package com.javapathfinder.eclipsejpf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/*
 * @throws IOException if the property file could not be loaded.
 */
public abstract class JPFLauncher{
  public static final String INTERNAL = "INTERNAL";
  public static final String EXTERNAL = "EXTERNAL";

  protected PrintWriter error;

  protected void launch(File file) throws IOException{
    PrintWriter errorStream = getErrorStream();

    //Create command
    StringBuffer command = new StringBuffer("java ");
    String vm_args = getVMArgs(null);
    if (vm_args != null && !vm_args.isEmpty())
      command.append(vm_args).append(" ");
    command.append("-jar ");

    if (getJPFRunMode(INTERNAL).equals(EXTERNAL)){
      String jar = getRunJPFJarPath(null);
      if (jar == null) errorStream.println("ABORTING: jpf.jar was not found.");
      command.append(jar);
    }else{
      if (!getJPFRunMode(INTERNAL).equals(INTERNAL)) errorStream.println("Bad String in Property: RUN_MODE. Defaulting to using internal JPF installation");
      String jar = getJPFJarPath(null);
      if (jar == null) errorStream.println("ABORTING: RunJPF.jar was not found.");
      command.append(jar);
    }

    command.append(" ");
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
      if ( isSourceSupported() )
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
  protected abstract String getJPFRunMode(String def);
  protected abstract String getJPFJarPath(String def);
  protected abstract String getRunJPFJarPath(String def);
  protected abstract PrintWriter getOutputStream();
  protected abstract PrintWriter getErrorStream();
  protected abstract boolean isSourceSupported();
  protected abstract void gotoSource(String filepath, int line);

  class ShellListener extends Thread{
    @Override
    public void run() {
      Socket socket = null;
      //We aren't sure when the port is going to open (if it ever does) so keep
      //on trying until we get a hit.
      while (socket == null) {
        try {
          socket = new Socket(InetAddress.getLocalHost(), 8000);
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
