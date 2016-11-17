/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

import java.io.IOException;
import java.io.StringReader;

import net.sf.saxon.lib.StandardEntityResolver;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entity resolver which will not try to fetch entities from the W3C since the Website may not be responsive
 * and cause the transformer to freeze when using the parsing new documents (e.g. with document function)
 *
 * <p>Implementation note: this implementation relies on Saxon 9.4 whcih includes local copies of the W3C entities.
 *
 * @author Christophe Lauret
 * @version 20 February 2013
 */
public class XHTMLEntityResolver implements EntityResolver {

  /** We need to rely on Saxon 9.4's internal entity resolver for now... */
  private final StandardEntityResolver resolver = new StandardEntityResolver();

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    InputSource in = this.resolver.resolveEntity(publicId, systemId);
    if (in == null) {
//      System.err.println("Unable to find entity: "+publicId);
      // Not included in Saxon, let's return an empty entity since the W3C won't give us the data...
      return new InputSource(new StringReader(""));
    }
    return in;
  }

}
