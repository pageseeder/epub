/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.pageseeder.epub.util.Paths;
import org.pageseeder.epub.util.XML;
import org.pageseeder.epub.util.XMLCopy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Rewrites the paths within HTML documents.
 *
 * <p>Note: This version only rewrites the paths to images.
 *
 * @author Christophe Lauret
 * @version 1 March 2013
 */
public final class HTMLPathRewriter extends XMLCopy {

  /**
   * The base path of the file being parsed.
   */
  private final String _base;

  /**
   * The path back to the root from the current file.
   */
  private final String _backToRoot;

  /**
   * The mapping from the old path to the new ones.
   */
  private final Map<String, String> _map;

  /**
   * The root path of the
   *
   * @param to   The target directory.
   * @param path The path of the file being processed.
   * @param map  The path mapping.
   *
   * @throws IOException If thrown by super class.
   */
  public HTMLPathRewriter(File to, String path, Map<String, String> map) throws IOException {
    super(new File(to, path));
    this._base = Paths.toBase(path);
    this._backToRoot = Paths.toBackToRoot(path);
    this._map = map;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    // <img src="[href]" />
    if (qName.equals("img")) {
      handleImg(attributes);
    } else {
      super.startElement(uri, localName, qName, attributes);
    }
  }

  @Override
  public void endDocument() {
    super._out.flush();
    super._out.close();
  }

  /**
   * Writes the 'img' tag and update the 'src' attribute based on the map.
   *
   * @param attributes All the attributes of the image
   */
  public void handleImg(Attributes attributes) {
    super._out.print("<img");
    final int len = attributes.getLength();
    for (int i = 0; i < len; i++) {
      String name = attributes.getQName(i);
      String value = attributes.getValue(i);
      if ("src".equals(name)) {
        String src = Paths.simplify(this._base + value);
        String newSrc = Paths.simplify(this._backToRoot+this._map.get(src));
        writeAttribute(name, newSrc);
      } else {
        writeAttribute(name, value);
      }
    }
    super._out.print('>');
  }

  /**
   * Rewrites the paths using the specified mapping of paths.
   *
   * @param from The root of the source directory
   * @param to   The root of the target directory
   * @param path The path from the root (common to both directories)
   * @param map  The mapping
   */
  public static void rewrite(File from, File to, String path, Map<String, String> map) {
    HTMLPathRewriter rewriter = null;
    try {
      rewriter = new HTMLPathRewriter(to, path, map);
      File html = new File(from, path);
      XMLReader xmlreader = XML.newXMLReader(rewriter);
      xmlreader.parse(new InputSource(new FileInputStream(html)));

    } catch (SAXException ex) {
      ex.printStackTrace();
      if (rewriter != null) rewriter.endDocument();
    } catch (EPubException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
