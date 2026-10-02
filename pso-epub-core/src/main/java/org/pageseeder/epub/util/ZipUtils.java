/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.pageseeder.epub.EPubException;


/**
 * A utility class for common Zip functions.
 *
 * @author Christophe Lauret
 * @version 12 April 2012
 */
public final class ZipUtils {

  /**
   * Size of internal buffer.
   */
  private static final int BUFFER = 2048;

  /** Utility class. */
  private ZipUtils() {
  }

  /**
   * Unzip the the file at the specified location.
   *
   * @param src  The file to unzip
   * @param dest The destination folder
   */
  public static void unzip(File src, File dest) {
    try (ZipFile zip = new ZipFile(src)) {
      ZipEntry entry;
      for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
        entry = e.nextElement();
        String name = entry.getName();
        // Ensure that the folder exists
        if (name.indexOf('/') > 0) {
          String folder = name.substring(0, name.lastIndexOf('/'));
          File dir = new File(dest, folder);
          if (!dir.exists()) {
            dir.mkdirs();
          }
        }
        // Only process files
        if (!entry.isDirectory()) {
          File f = new File(dest, name);
          try (BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
               BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f), BUFFER)) {
            int count;
            byte[] data = new byte[BUFFER];
            while ((count = is.read(data, 0, BUFFER)) != -1) {
              out.write(data, 0, count);
            }
          }
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      throw new EPubException("Failed to unzip", ex);
    }
  }


  /**
   * Zip the specified file or folder.
   *
   * @param src  The folder to zip
   * @param dest The destination zip
   */
  public static void zip(File src, File dest) {
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dest)))){
      if (src.isFile()) {
        // Source is a single file
        addToZip(src, out, null);

      } else {
        // Source is directory
        for (File f : src.listFiles()) {
          addToZip(f, out, null);
        }
      }

    } catch (IOException ex) {
      ex.printStackTrace();
      throw new EPubException("Failed to create zip", ex);
    }
  }

  /**
   * Zip the specified EPUB root folder as an EPUB container.
   *
   * <p>The <code>mimetype</code> file is written first and uncompressed as required by the EPUB
   * specification, followed by the other files and folders (depth-first, sorted by name) deflated.
   * Directory entries are included.
   *
   * @param root The EPUB root folder (containing <code>mimetype</code>, <code>META-INF</code>, ...)
   * @param dest The destination EPUB file
   */
  public static void zipEpub(File root, File dest) {
    File mimetype = new File(root, "mimetype");
    if (!mimetype.isFile())
      throw new EPubException("Missing mimetype file in "+root);
    try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dest)))) {
      out.setLevel(Deflater.BEST_COMPRESSION);
      // mimetype first and stored
      byte[] data = java.nio.file.Files.readAllBytes(mimetype.toPath());
      CRC32 crc = new CRC32();
      crc.update(data);
      ZipEntry entry = new ZipEntry("mimetype");
      entry.setMethod(ZipEntry.STORED);
      entry.setSize(data.length);
      entry.setCompressedSize(data.length);
      entry.setCrc(crc.getValue());
      entry.setTime(mimetype.lastModified());
      out.putNextEntry(entry);
      out.write(data);
      out.closeEntry();
      // Everything else
      for (File f : sortedChildren(root)) {
        if (!f.equals(mimetype)) {
          addToEpub(f, f.getName(), out);
        }
      }
    } catch (IOException ex) {
      throw new EPubException("Failed to create epub", ex);
    }
  }

  /**
   * Add the specified file or folder to the EPUB zip recursively.
   *
   * @param file The file or folder to add
   * @param path The path of the entry within the zip
   * @param out  The destination zip stream
   *
   * @throws IOException If an IO error occurs.
   */
  private static void addToEpub(File file, String path, ZipOutputStream out) throws IOException {
    if (file.isDirectory()) {
      ZipEntry entry = new ZipEntry(path + "/");
      entry.setMethod(ZipEntry.STORED);
      entry.setSize(0);
      entry.setCompressedSize(0);
      entry.setCrc(0);
      entry.setTime(file.lastModified());
      out.putNextEntry(entry);
      out.closeEntry();
      for (File f : sortedChildren(file)) {
        addToEpub(f, path + "/" + f.getName(), out);
      }
    } else {
      ZipEntry entry = new ZipEntry(path);
      entry.setTime(file.lastModified());
      out.putNextEntry(entry);
      java.nio.file.Files.copy(file.toPath(), out);
      out.closeEntry();
    }
  }

  /**
   * @return the children of the specified folder sorted by name.
   */
  private static File[] sortedChildren(File folder) {
    File[] files = folder.listFiles();
    if (files == null) return new File[0];
    Arrays.sort(files, Comparator.comparing(File::getName));
    return files;
  }

  /**
   * Zip the specified file or folder.
   *
   * @param file   The file or folder to zip
   * @param out    The destination zip stream
   * @param folder The current folder
   *
   * @throws IOException If an IO error occurs.
   */
  private static void addToZip(File file, ZipOutputStream out, String folder) throws IOException {
    // Directory
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        addToZip(f, out, file.getName()+"/");
      }

    // File
    } else {
      byte[] data = new byte[BUFFER];
      try (BufferedInputStream origin = new  BufferedInputStream(new FileInputStream(file), BUFFER)) {
        ZipEntry entry = new ZipEntry(folder != null? folder + file.getName() : file.getName());
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
          out.write(data, 0, count);
        }
        origin.close();
      } catch (IOException ex) {
        ex.printStackTrace();
        throw new EPubException("Failed to add to zip", ex);
      }
    }
  }

}
