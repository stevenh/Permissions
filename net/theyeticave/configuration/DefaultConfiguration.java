package net.theyeticave.configuration;

/**
 * Basic configuration loader.
 * 
 * @author Nijiko
 */
public abstract class DefaultConfiguration {
  public String permissionSystem = "default";

  public abstract void load();
}
