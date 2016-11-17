package org.pageseeder.epub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.pageseeder.epub.util.Files;
import org.pageseeder.epub.util.Paths;
import org.pageseeder.epub.util.XML;
import org.pageseeder.epub.util.XMLCopy;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This class processes in epub before being handed over to an XSLT.
 *
 * <p>This preprocessor does the following:
 * <ul>
 *   <li>all media files and links are normalised</li>
 *   <li>doctype declarations are removed</li>
 * <ul>
 *
 * @author Christophe Lauret
 * @version 28 February 2013
 */
public final class EPubPreProcessor {

  /**
   * The directory containing the unpacked epub (source).
   */
  private final File _unpacked;

  /**
   * The directory containing the pre-processed files (target).
   */
  private final File _preprocessed;

  /**
   *
   * @param from The directory containing the unpacked epub (source).
   * @param to   The directory containing the pre-processed files (target).
   *
   * @throws NullPointerException if either is <code>null</code>.
   */
  public EPubPreProcessor(File from, File to) {
    if (from == null) throw new NullPointerException("from");
    if (to == null) throw new NullPointerException("to");
    this._unpacked = from;
    this._preprocessed = to;
  }

  /**
   * Process the epub in the unpacked directory.
   *
   * @throws IOException
   */
  public void process() throws EPubException, IOException {
    Files.ensureDirectoryExists(this._preprocessed);
    // Identify content.opf files
    List<String> contents = Container.findContentPaths(this._unpacked);
    // Process each of them (there's only one generally)
    for (String p : contents) {
      process(p);
    }
    // copy the container
    File original = new File(this._unpacked, Container.CONTAINER_PATH);
    File copy = new File(this._preprocessed, Container.CONTAINER_PATH);
    Files.copy(original, copy);
  }

  private void process(String path) throws EPubException, IOException {
    File content = new File(this._unpacked, path);
    File copy = new File(this._preprocessed, path);
    Files.ensureDirectoryExists(copy.getParentFile());
    ContentRewriter rewriter = null;
    try {
      // First pass to extract the mapping
      ItemPathMapper mapper = new ItemPathMapper(path);
      XMLReader xmlreader = XML.newXMLReader(mapper);
      xmlreader.parse(new InputSource(new FileInputStream(content)));

      // Second pass to rewrite the paths
      rewriter = new ContentRewriter(this._unpacked, this._preprocessed, path, mapper.getMap());
      xmlreader = XML.newXMLReader(rewriter);
      xmlreader.parse(new InputSource(new FileInputStream(content)));

    } catch (SAXException ex) {
      ex.printStackTrace();
      if (rewriter != null)
        rewriter.endDocument();
      throw new EPubException("An error occurred while parsing "+path, ex);
    }
  }

  /**
   * Rewrites a content.opf file and move media content to a special folder
   *
   * @author Christophe Lauret
   * @version 1 March 2013
   */
  private static class ContentRewriter extends XMLCopy {

    /**
     * The current root of the Epub.
     */
    private final File _from;

    /**
     * The current root of the Epub.
     */
    private final File _to;

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
     * @param from The root
     * @param to
     * @param path
     * @throws IOException
     */
    public ContentRewriter(File from, File to, String path, Map<String, String> map) throws IOException {
      super(new File(to, path));
      this._from = from;
      this._to = to;
      this._base = Paths.toBase(path);
      this._backToRoot = Paths.toBackToRoot(path);
      this._map = map;
      File media = new File(this._to, Config.MEDIA_FOLDER);
      Files.ensureDirectoryExists(media);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // <item href="[href]" id="[id]" media-type="image/jpeg"/>
      if (qName.equals("item")) {
        startItemElement(attributes);
      } else {
        super.startElement(uri, localName, qName, attributes);
      }
    }

    @Override
    public void endDocument() {
      super._out.flush();
      super._out.close();
    }


    public void startItemElement(Attributes attributes) throws SAXException {
      super._out.print("<item");
      String media = attributes.getValue("media-type");
      if (media.startsWith("image/")) {
        handleImage(attributes);
      } else if (media.equals("application/xhtml+xml")) {
        handleHTML(attributes);
      } else {
        handleOther(attributes);
      }
      super._out.print('>');
    }

    /**
     * Move the images
     *
     * @param attributes the attributes of item element
     */
    private void handleImage(Attributes attributes) {
      int len = attributes.getLength();
      for (int i = 0; i < len; i++) {
        String name = attributes.getQName(i);
        String value = attributes.getValue(i);
        if ("href".equals(name)) {
          String oldPath = this._base + value;
          String newPath = this._map.get(oldPath);
          copyFiles(oldPath, newPath);
          writeAttribute(name, Paths.simplify(this._backToRoot+newPath));
        } else {
          writeAttribute(name, value);
        }
      }
    }

    /**
     * Rewrite the paths to reflect changes
     *
     * @param attributes the attributes of item element
     */
    private void handleHTML(Attributes attributes) {
      String href = attributes.getValue("href");
      writeAttributes(attributes);
      HTMLPathRewriter.rewrite(this._from, this._to, this._base+href, this._map);
    }

    /**
     * Simply copy the file
     *
     * @param attributes the attributes of item element
     */
    private void handleOther(Attributes attributes) {
      String href = attributes.getValue("href");
      String path = this._base + href;
      copyFiles(path, path);
      writeAttributes(attributes);
    }

    private void copyFiles(String source, String destination) {
      try {
        Files.copy(new File(this._from, source), new File(this._to, destination));
      } catch (IOException ex) {
        System.err.println("Unable to copy file: "+ source);
      }
    }
  }

}
