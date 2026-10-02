/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entity resolver which will not try to fetch entities from the W3C since the Website may not be responsive
 * and cause the transformer to freeze when using the parsing new documents (e.g. with document function)
 *
 * <p>XHTML DTDs and entity sets are resolved to the bundled <code>ent/xhtml.ent</code> which declares all
 * the XHTML character entities. Any other external entity resolves to an empty entity.
 *
 * @author Christophe Lauret
 * @version 20 February 2013
 */
public class XHTMLEntityResolver implements EntityResolver {

  /** Location of the bundled XHTML entity declarations. */
  private static final String XHTML_ENTITIES = "/org/pageseeder/epub/ent/xhtml.ent";

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    if (isXHTML(publicId, systemId)) {
      URL url = XHTMLEntityResolver.class.getResource(XHTML_ENTITIES);
      if (url != null) {
        InputStream stream = url.openStream();
        InputSource in = new InputSource(stream);
        in.setPublicId(publicId);
        in.setSystemId(url.toString());
        return in;
      }
    }
    // Let's return an empty entity since the W3C won't give us the data...
    return new InputSource(new StringReader(""));
  }

  /**
   * @return <code>true</code> if the public or system ID identifies an XHTML DTD or entity set.
   */
  private static boolean isXHTML(String publicId, String systemId) {
    if (publicId != null && (publicId.startsWith("-//W3C//DTD XHTML") || publicId.startsWith("-//W3C//ENTITIES")))
      return true;
    return systemId != null && systemId.contains("w3.org/") && systemId.matches(".*xhtml.*\\.(dtd|ent)$");
  }

}
