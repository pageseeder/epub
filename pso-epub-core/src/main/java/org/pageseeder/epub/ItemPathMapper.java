/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.pageseeder.epub.util.Paths;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This file generates a mapping between the original file path and the new file paths.
 *
 * @author Christophe Lauret
 * @version 1 March 2013
 */
public final class ItemPathMapper extends DefaultHandler {

  /**
   * The base path of the file being parsed.
   */
  private final String _base;

  /**
   * The mapping from the old path to the new ones.
   */
  private Map<String, String> map = new HashMap<String, String>();

  /**
   * The root path of the

   * @param path
   * @throws IOException
   */
  public ItemPathMapper(String path) throws IOException {
    this._base = Paths.toBase(path);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    // <item href="[href]" id="[id]" media-type="image/jpeg"/>
    if (qName.equals("item")) {
      startItemElement(attributes);
    }
  }


  public void startItemElement(Attributes attributes) throws SAXException {
    String media = attributes.getValue("media-type");
    if (media.startsWith("image/")) {
      String href = attributes.getValue("href");
      String oldPath = Paths.simplify(this._base + href);
      String newPath = Paths.simplify(Config.MEDIA_FOLDER + "/" + attributes.getValue("id") + Paths.getExtension(href));
      System.out.println(oldPath+" -> "+newPath);
      this.map.put(oldPath, newPath);
    }
  }

  /**
   * @return the map
   */
  public Map<String, String> getMap() {
    return this.map;
  }

}
