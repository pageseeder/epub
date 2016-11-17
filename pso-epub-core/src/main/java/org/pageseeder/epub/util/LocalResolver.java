/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Christophe Lauret
 * @version 20 February 2013
 */
public final class LocalResolver implements URIResolver {

  private final URIResolver _resolver;

  /**
   * @param resolver the original resolver to default to.
   */
  public LocalResolver(URIResolver resolver) {
    this._resolver = resolver;
  }

  @Override
  public Source resolve(String href, String base) throws TransformerException {
    if (href != null && href.startsWith("file:")) {
      return this._resolver.resolve(href, base);
    } else {
      return new StreamSource();
    }
  }
}
