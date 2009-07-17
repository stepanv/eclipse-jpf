package com.javapathfinder.eclipsejpf;

public class JPFProperties {

  public static final String PATH_SEP = ":";

  public static String getJPFClasspath() {
    //hardcoded just for me!
    return "/home/sandro/NetBeansProjects/trunk/build/jpf/:/home/sandro/NetBeansProjects/trunk/build/env/jvm/:/home/sandro/NetBeansProjects/trunk/lib/bcel.jar:/home/sandro/NetBeansProjects/TopicShell/build/classes/:/home/sandro/NetBeansProjects/JavaApplication2/build/classes";
  }

  public static boolean includeTargetPath() {
    return false;
  }

  public static String getVMBootClasspath() {
    
    return "/home/sandro/NetBeansProjects/trunk/build/env/jpf/";
  }

}
