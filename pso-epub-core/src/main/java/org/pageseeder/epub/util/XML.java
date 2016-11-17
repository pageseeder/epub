/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pageseeder.epub.EPubException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Christophe Lauret
 * @version 01/03/2013
 *
 */
public class XML {

  /**
   * The LexicalHandler property.
   */
  public static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

  /** Utility */
  private XML() {
  }

  public static XMLReader newXMLReader(DefaultHandler handler) throws EPubException{
    SAXParser parser = XML.newParser();
    try {

      XMLReader xmlreader = parser.getXMLReader();
      // configure the reader
      xmlreader.setContentHandler(handler);
      xmlreader.setEntityResolver(handler);
      xmlreader.setErrorHandler(handler);
      xmlreader.setDTDHandler(handler);
      if (handler instanceof LexicalHandler)
        xmlreader.setProperty(LEXICAL_HANDLER_PROPERTY, handler);
      return xmlreader;

    } catch (SAXException ex) {
      throw new EPubException("Unable to configure XML Reader", ex);
    }
  }

  public static SAXParser newParser() throws EPubException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = null;
    try {
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://xml.org/sax/features/namespaces", false);
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      parser = factory.newSAXParser();
    } catch (SAXException ex) {
      throw new EPubException("Unable to configure parser", ex);
    } catch (ParserConfigurationException ex) {
      throw new EPubException("Unable to configure parser", ex);
    }
    return parser;
  }
}
