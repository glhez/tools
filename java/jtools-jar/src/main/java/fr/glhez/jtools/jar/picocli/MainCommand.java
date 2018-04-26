package fr.glhez.jtools.jar.picocli;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, version = "JAR Tool")
public class MainCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("Hello world");
  }

}
