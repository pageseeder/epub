package org.pageseeder.epub.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple SAX handler copying the XML on to a writer.
 *
 * <p>This implementation
 *
 * @author Christophe Lauret
 * @version 1 March 2013
 */
public class XMLCopy extends DefaultHandler implements ContentHandler, LexicalHandler, EntityResolver {

  /**
   * Where the copied XML goes.
   */
  protected final PrintWriter _out;

  private boolean inDTD = false;

  public XMLCopy(File out) throws FileNotFoundException {
    OutputStream os = new FileOutputStream(out);
    this._out = new PrintWriter(new OutputStreamWriter(os, Charset.forName("utf8")));
  }

  public XMLCopy(PrintWriter out) {
    this._out = out;
  }

  @Override
  public void startDocument() throws SAXException {
    this._out.println("<?xml version='1.0' encoding='UTF-8'?>");
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    this._out.print("xmlns");
    if (prefix != null && prefix.length() > 0) {
      this._out.print(":");
      this._out.print(prefix);
    }
    this._out.print('"');
    this._out.print(uri); // TODO escape XML?
    this._out.print('"');
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    this._out.print('<');
    this._out.print(qName);
    writeAttributes(attributes);
    this._out.print('>');
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    this._out.print("</");
    this._out.print(qName);
    this._out.print('>');
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    escape(ch, start, length, false);
  }

  @Override
  public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    escape(ch, start, length, false);
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {
    writePI(target, data);
  }

  // Entity Resolver
  // ----------------------------------------------------------------------------------------------

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
    if (publicId.startsWith("-//W3C//")) {
      try {
        ClassLoader loader = XMLCopy.class.getClassLoader();
        URL url = loader.getResource("com/pageseeder/ant/epub/ent/xhtml.ent");
        return new InputSource(url.openStream());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return new InputSource(new StringReader(""));
  }

  // Lexical Handler
  // ----------------------------------------------------------------------------------------------

  @Override
  public void comment(char[] ch, int start, int length) throws SAXException {
    // Do not write comments in the DTD
    if (!this.inDTD)
      writeComment(new String(ch, start, length));
  }

  @Override
  public void startEntity(String name) throws SAXException {
  }

  @Override
  public void endEntity(String name) throws SAXException {
  }

  @Override
  public void startDTD(String name, String publicId, String systemId) throws SAXException {
    this.inDTD = true;
  }

  @Override
  public void endDTD() throws SAXException {
    this.inDTD = false;
  }

  @Override
  public void startCDATA() throws SAXException {
  }

  @Override
  public void endCDATA() throws SAXException {
  }

  // Write methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes the comment verbatim (it does not check for '--' strings)
   *
   * @param comment The comment to write.
   */
  protected final void writeComment(String comment) {
    this._out.print("<!--");
    this._out.print(comment);
    this._out.print("-->");
  }

  /**
   *
   * @param target
   * @param data
   */
  protected final void writePI(String target, String data) {
    this._out.print("<?");
    this._out.print(target);
    this._out.print(" ");
    this._out.print(data);
    this._out.print("?>");
  }

  /**
   *
   * @param atts
   */
  protected final void writeAttributes(Attributes atts) {
    int len = atts.getLength();
    for (int i = 0; i < len; i++) {
      writeAttribute(atts, i);
    }
  }

  /**
   *
   * @param atts
   * @param i
   */
  protected final void writeAttribute(Attributes atts, int i) {
    writeAttribute(atts.getQName(i), atts.getValue(i));
  }

  /**
   *
   * @param name
   * @param value
   */
  protected final void writeAttribute(String name, String value) {
    char ch[] = value.toCharArray();
    this._out.print(' ');
    this._out.print(name);
    this._out.print("=\"");
    escape(ch, 0, ch.length, true);
    this._out.print('"');
  }

  /**
   *
   * @param ch
   * @param start
   * @param length
   * @param isAttributeValue
   */
  protected final void escape(char ch[], int start, int length, boolean isAttributeValue) {
    for (int i = start; i < start + length; i++) {
      switch (ch[i]) {
        case '&':
           this._out.print("&amp;");
           break;
        case '<':
          this._out.print("&lt;");
          break;
        case '>':
          this._out.print("&gt;");
          break;
        case '\"':
          if (isAttributeValue) {
            this._out.print("&quot;");
          } else {
            this._out.print('\"');
          }
          break;
        default:
          if (ch[i] > '\u007f') {
            this._out.print("&#");
            this._out.print(Integer.toString(ch[i]));
            this._out.print(';');
          } else {
            this._out.print(ch[i]);
          }
      }
    }
  }

}
