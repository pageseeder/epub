/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Templates;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.pageseeder.epub.Config;
import org.pageseeder.epub.EPubPreProcessor;
import org.pageseeder.epub.util.Files;
import org.pageseeder.epub.util.XSLT;
import org.pageseeder.epub.util.ZipUtils;

/**
 * An ANT task to import a epub file as one or more PageSeeder documents
 *
 * @author Christophe Lauret
 * @version 13 February 2013
 */
public final class ImportTask extends Task {

  /**
   * The Word document to import.
   */
  private File _source;

  /**
   * Where to create the PageSeeder documents (a directory).
   */
  private File _destination;

  /**
   * The name of the working directory
   */
  private File _working;

  /**
   * The configuration.
   */
  private File _config;

  /**
   * List of parameters specified for the transformation into PSML
   */
  private List<Param> _parameters = new ArrayList<Param>();

  // Set properties
  // ----------------------------------------------------------------------------------------------

  /**
   * Set the source file (a epub file).
   *
   * @param epub The Word document (epub) to import.
   */
  public void setSrc(File epub) {
    if (!(epub.exists())) {
      throw new BuildException("the document " + epub.getName()+ " doesn't exist");
    }
    if (epub.isDirectory()) {
      throw new BuildException("the document " + epub.getName() + " can't be a directory");
    }
    String name = epub.getName();
    if (!name.endsWith(".epub") && !name.endsWith(".zip")) {
      log("An EPUB file should generally end with .epub or .zip - but was "+name);
    }
    this._source = epub;
  }

  /**
   * Set the destination folder where the PageSeeder document(s) should be created.
   *
   * @param destination The destination folder.
   */
  public void setDest(File destination) {
    this._destination = destination;
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
    this._working = working;
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
    this._config = config;
  }

  /**
   * Create a parameter object and stores it in the list To be used by the XSLT transformation
   */
  public Param createParam() {
    Param param = new Param();
    this._parameters.add(param);
    return param;
  }

  // Execute
  // ----------------------------------------------------------------------------------------------

  @Override
  public void execute() throws BuildException {
    if (this._source == null)
      throw new BuildException("Source document must be specified using 'src' attribute");

    // Defaulting working directory
    if (this._working == null) this._working = getDefaultWorkingFolder();
    if (!this._working.exists()) this._working.mkdirs();

    // Defaulting destination directory
    if (this._destination == null) {
      this._destination = this._source.getParentFile();
      log("Destination set to source directory "+this._destination.getAbsolutePath()+"");
    }

    // Defaulting config file
    if (this._config == null) {
      this._config = null; // TODO
      log("Using default epub configuration for import");
    }

    // The folder and name of the epub
    File folder = null;
    String name = null;
    if (this._destination.isFile()) {
      folder = this._destination.getParentFile();
      name = this._destination.getName();
      if (name.endsWith(".psml")) name = name.substring(0, name.length()-5);
    } else {
      folder = this._destination;
      name = this._source.getName();
      if (name.endsWith(".epub")) name = name.substring(0, name.length()-5);
    }

    // Ensure that output folder exists
    Files.ensureDirectoryExists(folder);

    // 1. Unzip file
    log("Extracting EPUB: " + this._source.getName());
    File unpacked = new File(this._working, "unpacked");
    unpacked.mkdir();
    ZipUtils.unzip(this._source, unpacked);

    // 2. Sanity check
    // TODO: Look for 'application/epub+zip' in mimetype document

    // 3. Preprocess
    File preprocessed = new File(this._working, "preprocessed");
    EPubPreProcessor processor = new EPubPreProcessor(unpacked, preprocessed);
    try {
      processor.process();
    } catch (Exception ex) {
      throw new BuildException(ex);
    }

    // 4. Process the files with XSLT
    log("Process with XSLT");
    File container = new File(preprocessed, "META-INF/container.xml");

    // Parse templates
    Templates templates = XSLT.getTemplatesFromResource("com/pageseeder/ant/epub/xslt/import.xsl");
    String outuri = folder.toURI().toString();

    // Initiate parameters
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("_rootfolder", preprocessed.toURI().toString());
    parameters.put("_outputfolder", outuri);
    parameters.put("_epubfilename", this._source.getName());
    if (this._config != null)
      parameters.put("_configfileurl", this._config.toURI().toString());

    // Transform
    XSLT.transform(container, new File(folder, name+".psml"), templates, parameters);

    // 5. copy the media files
    copyMedia(preprocessed, folder);

  }

  // Helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * @return the default working folder.
   */
  private static File getDefaultWorkingFolder() {
    String tmp = "psepub-"+System.currentTimeMillis();
    return new File(System.getProperty("java.io.tmpdir"), tmp);
  }

  /**
   *
   */
  private static void copyMedia(File from, File to) {
    try  {
      File media = new File(from, Config.MEDIA_FOLDER);
      File mediaOut = new File(to, Config.MEDIA_FOLDER);
      Files.ensureDirectoryExists(mediaOut);
      for (File m : media.listFiles()) {
        Files.copy(m, new File(mediaOut, m.getName()));
      }
    } catch (IOException ex) {
      // TODO clean up files
      throw new BuildException(ex);
    }
  }

}
