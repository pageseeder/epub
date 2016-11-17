package org.pageseeder.epub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Container {

  public static final String CONTAINER_PATH = "META-INF/container.xml";

  public static List<String> findContentPaths(File root) throws EPubException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    ContentIdentifier identifier = new ContentIdentifier(root);
    try {
      SAXParser parser = factory.newSAXParser();
      File container = new File(root, CONTAINER_PATH);
      parser.parse(container, identifier);
    } catch (ParserConfigurationException ex) {
      throw new EPubException("Unable to configure parser", ex);
    } catch (SAXException ex) {
      throw new EPubException("An error occurred while parsing META-INF/container.xml", ex);
    }
    return identifier.getContentPaths();
  }


  private static class ContentIdentifier extends DefaultHandler {

    private final File _root;

    private final List<String> _paths;

    public ContentIdentifier(File root) {
      this._root = root;
      this._paths = new ArrayList<String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if ("rootfile".equals(qName)) {
        String mediaType = attributes.getValue("media-type");
        if ("application/oebps-package+xml".equals(mediaType)) {
          String path = attributes.getValue("full-path");
          this._paths.add(path);
        }
      }
    }

    private List<String> getContentPaths() {
      return this._paths;
    }
  }
}
