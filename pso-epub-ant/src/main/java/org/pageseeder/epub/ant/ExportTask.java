/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * An ANT task to export PageSeeder processed PSML documents as an epub.
 *
 * @author Philip Rutherford
 */
public final class ExportTask extends Task {

  /**
   * The PageSeeder PSML processed root document
   */
  private File source;

  /**
   * The destination file where the epub should be stored.
   */
  private File destination;

  /**
   * The working folder
   */
  private File working;

  /**
   * The configuration file.
   */
  private File config;

  /**
   * The CSS override file.
   */
  private File css;

  /**
   * The media folder.
   */
  private File media;

  /**
   * The components folder name
   */
  private String componentsName = "components";

  // Set properties
  // ----------------------------------------------------------------------------------------------

  /**
   * Set the source PSML processed root document
   *
   * @param source The file
   */
  public void setSrc(File source) {
    if (!(source.exists())) {
      throw new BuildException("the source " + source.getName()+ " doesn't exist");
    }
    if (source.isDirectory()) {
      throw new BuildException("the source " + source.getName() + " must be a file");
    }
    this.source = source;
  }

  /**
   * Set the destination file where the epub should be stored.
   *
   * @param destination The file
   */
  public void setDest(File destination) {
    if (destination.exists() && destination.isDirectory()) {
      throw new BuildException("if destination epub exists, it must be a file");
    }
    this.destination = destination;
  }

  /**
   * Set the working folder (optional).
   *
   * @param working The working folder.
   */
  public void setWorking(File working) {
    if (working.exists() && !working.isDirectory()) {
      throw new BuildException("if working folder exists, it must be a directory");
    }
    this.working = working;
  }

  /**
   * Set the configuration file (optional).
   *
   * @param config The configuration file.
   */
  public void setConfig(File config) {
    if (!config.exists() || config.isDirectory()) {
      throw new BuildException("your configuration file must exist and be a file");
    }
    this.config = config;
  }

  /**
   * Set the CSS override file (optional).
   *
   * @param css The CSS file.
   */
  public void setCSS(File css) {
    if (!css.exists() || css.isDirectory()) {
      throw new BuildException("your css file must exist and be a file");
    }
    this.css = css;
  }

  /**
   * Set the media folder (optional).
   * @param media The media folder.
   */
  public void setMedia(File media) {
    if (!media.exists() || !media.isDirectory()) {
      throw new BuildException("your media folder must exist and be a directory"); }
    this.media = media;
  }

  /**
   * Set the components folder name (optional).
   * @param componentsName The components folder name.
   */
  public void setComponentsName(String componentsName) {
    this.componentsName = componentsName;
  }

  // Execute
  // ----------------------------------------------------------------------------------------------

  @Override
  public void execute() throws BuildException {
    if (this.source == null)
      throw new BuildException("Source must be specified using 'src' attribute");
    if (this.destination == null)
      throw new BuildException("Destination must be specified using 'dest' attribute");

    // Defaulting working directory
    if (this.working == null) {
      String tmp = "antepub-"+System.currentTimeMillis();
      this.working = new File(System.getProperty("java.io.tmpdir"), tmp);
    }
    if (!this.working.exists()) {
      this.working.mkdirs();
    }

    // TODO Add export code

  }

}
