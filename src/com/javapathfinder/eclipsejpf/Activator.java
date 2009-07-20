package com.javapathfinder.eclipsejpf;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "RunJPF";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	  /**
	   * Convenience method to log any type of message.
	   * The code for this log will be Status.OK
	   */
	  public static void log(int severity, String message, Throwable exception) {
	    getDefault().getLog().log(new Status(severity, PLUGIN_ID, Status.OK,
	                                         message, exception == null ? new Throwable() : exception));
	  }

	  /**
	   * Convenience method to log plugin information.
	   */
	  public static void logInfo(String message) {
	    log(Status.INFO, message == null ? "null" : message, null);
	  }

	  /**
	   * Convenience method to log Warnings without an exception This call is
	   * exactly equivalent to logWarning(message, null) @param message the message
	   * to include with the warning
	   */
	  public static void logWarning(String message) {
	    logWarning(message, null);
	  }

	  /**
	   * Convenience method to log Warnings along with an exception @param message
	   * the message to include with the warning @param exception the exception to
	   * include with the warning
	   */
	  public static void logWarning(String message, Exception exception) {
	    log(Status.WARNING, message, exception);
	  }

	  /**
	   * Convenience method to log errors
	   * @param message the message to display with this error
	   * @param exception the exception to associate with ths error.
	   */
	  public static void logError(String message, Throwable exception) {
	    log(Status.ERROR, message, exception);
	  }
	  

	  /**
	   * Convenience method to log errors
	   * @param message the message to display with this error
	   */
	  public static void logError(String message) {
	    logError(message, new Exception());
	  }

}
