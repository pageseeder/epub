package org.pageseeder.epub.ant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XML helpers to inspect the content of an EPUB.
 */
final class EpubXml {

  /** Namespaces used in XPath expressions. */
  private static final Map<String, String> NAMESPACES = Map.of(
      "h", "http://www.w3.org/1999/xhtml",
      "epub", "http://www.idpf.org/2007/ops",
      "opf", "http://www.idpf.org/2007/opf",
      "dc", "http://purl.org/dc/elements/1.1/");

  private final Map<String, byte[]> entries;

  private EpubXml(Map<String, byte[]> entries) {
    this.entries = entries;
  }

  static EpubXml load(File epub) throws IOException {
    return new EpubXml(EpubAssert.read(epub));
  }

  boolean has(String name) {
    return this.entries.containsKey(name);
  }

  /**
   * @return the parsed entry.
   */
  Document document(String name) {
    byte[] data = this.entries.get(name);
    if (data == null) fail("Missing entry "+name);
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory.newDocumentBuilder().parse(new ByteArrayInputStream(data));
    } catch (Exception ex) {
      throw new AssertionError("Unable to parse "+name, ex);
    }
  }

  /**
   * @return the nodes matching the XPath in the specified entry.
   */
  NodeList nodes(String name, String xpath) {
    try {
      return (NodeList) xpath().evaluate(xpath, document(name), XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      throw new AssertionError("Invalid XPath "+xpath, ex);
    }
  }

  /**
   * @return the number of nodes matching the XPath in the specified entry.
   */
  int count(String name, String xpath) {
    return nodes(name, xpath).getLength();
  }

  /**
   * @return the string value of the XPath in the specified entry.
   */
  String value(String name, String xpath) {
    try {
      return xpath().evaluate(xpath, document(name));
    } catch (XPathExpressionException ex) {
      throw new AssertionError("Invalid XPath "+xpath, ex);
    }
  }

  /**
   * @return the names of the XHTML entries.
   */
  List<String> xhtmlEntries() {
    List<String> names = new ArrayList<>();
    for (String name : this.entries.keySet()) {
      if (name.endsWith(".xhtml")) names.add(name);
    }
    return names;
  }

  /**
   * Asserts that every relative link in the XHTML files points to an existing file and ID.
   */
  void assertLinksResolve() {
    List<String> broken = new ArrayList<>();
    for (String name : xhtmlEntries()) {
      String folder = name.substring(0, name.lastIndexOf('/') + 1);
      NodeList links = nodes(name, "//h:a[@href]");
      for (int i = 0; i < links.getLength(); i++) {
        String href = ((Element) links.item(i)).getAttribute("href");
        if (href.matches("^[a-z]+:.*")) continue;
        String file = href.contains("#") ? href.substring(0, href.indexOf('#')) : href;
        String id = href.contains("#") ? href.substring(href.indexOf('#') + 1) : "";
        String target = file.isEmpty() ? name : folder + file;
        if (!has(target)) {
          broken.add(name+" -> "+href+" (missing file)");
        } else if (!id.isEmpty() && count(target, "//*[@id='"+id+"']") == 0) {
          broken.add(name+" -> "+href+" (missing id)");
        }
      }
    }
    assertTrue(broken.isEmpty(), "Broken links:\n"+String.join("\n", broken));
  }

  private static XPath xpath() {
    XPath xpath = XPathFactory.newInstance().newXPath();
    xpath.setNamespaceContext(new NamespaceContext() {
      @Override
      public String getNamespaceURI(String prefix) {
        return NAMESPACES.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
      }
      @Override
      public String getPrefix(String uri) {
        return null;
      }
      @Override
      public Iterator<String> getPrefixes(String uri) {
        return null;
      }
    });
    return xpath;
  }

}
