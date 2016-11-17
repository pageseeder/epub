/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub;

/**
 * Encapsulate the import configuration for this class.
 *
 * @author Christophe Lauret
 * @version 18 February 2013
 */
public final class Config {

  /**
   * Where the images go.
   */
  public static String MEDIA_FOLDER = "media";

  /**
   * Creates a new configuration.
   */
  public Config() {
  }

  /**
   * EPUB metadata specifications.
   */
  private Metadata _metadata;

  /**
   * Fonts used in an pub.
   */
  private Fonts _fonts;

  /**
   * @return the metadata
   */
  public Metadata getMetadata() {
    return this._metadata;
  }

  /**
   * @return the fonts
   */
  public Fonts getFonts() {
    return this._fonts;
  }
}
