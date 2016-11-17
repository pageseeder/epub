package org.pageseeder.epub.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.pageseeder.epub.EPubException;

/**
 * A bunch of IO utility functions.
 *
 * @author Christophe Lauret
 * @version 28 February 2013
 */
public class Files {

  /** Utility class */
  private Files() {
  }

  /**
   * Copies the file using NIO
   *
   * @param from File to copy
   * @param to   Target file
   */
  public static void copy(File from, File to) throws IOException {
    Files.ensureDirectoryExists(to.getParentFile());
    if(!to.exists()) {
      to.createNewFile();
    }

    try (FileInputStream in = new FileInputStream(from);
         FileOutputStream out = new FileOutputStream(to)) {
      FileChannel source = in.getChannel();
      FileChannel destination = out.getChannel();
      destination.transferFrom(source, 0, source.size());
    }
  }

  /**
   * Ensures that the specified directory actually exists and creates it if necessary.
   *
   * @param directory The directory to check
   *
   * @throws BuildException if the directory could not created.
   */
  public static void ensureDirectoryExists(File directory) throws EPubException {
    if (!directory.exists()) {
      boolean done = directory.mkdirs();
      if (!done)
        throw new EPubException("Unable to create target directory for preprocessor");
    }
  }

}
