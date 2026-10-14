/*
 * Copyright (c) 1999-2026 Allette Systems Pty Ltd
 */
package org.pageseeder.epub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;

import org.pageseeder.epub.util.Files;
import org.pageseeder.epub.util.XSLT;
import org.pageseeder.epub.util.ZipUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Converts PageSeeder processed PSML documents to an EPUB.
 *
 * <p>The source is the processed root PSML document; its components are expected in a sibling
 * folder named after {@link #setComponentsName(String)}.
 *
 * <p>The working folder is used as the EPUB root while building (<code>mimetype</code>,
 * <code>META-INF/</code> and <code>OEBPS/</code> are created directly in it) and is assumed to be
 * empty: any other file left under <code>META-INF</code> or <code>OEBPS</code> ends up in the EPUB.
 *
 * @author Philip Rutherford
 */
public final class PSMLProcessor {

  /** Location of the export resources on the classpath. */
  private static final String RESOURCES = "org/pageseeder/epub/export/";

  /** Image extensions included in the manifest. */
  private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "svg", "webp");

  /**
   * The PageSeeder PSML processed root document
   */
  private File source;

  /**
   * The destination file where the epub should be stored.
   */
  private File destination;

  /**
   * The working folder (EPUB root)
   */
  private File working;

  /**
   * The configuration file.
   */
  private File config;

  /**
   * The CSS override file.
   */
  private File css;

  /**
   * The media folder.
   */
  private File media;

  /**
   * The components folder name
   */
  private String componentsName = "components";

  /**
   * Receives progress messages.
   */
  private Consumer<String> logger = message -> {};

  /** Receives XSLT warnings, errors and xsl:message output (optional). */
  private ErrorListener errorListener;

  public void setErrorListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }
  // Set properties
  // ----------------------------------------------------------------------------------------------

  /**
   * @param source The PSML processed root document
   */
  public void setSource(File source) {
    this.source = source;
  }

  /**
   * @param destination The file where the epub should be stored
   */
  public void setDestination(File destination) {
    this.destination = destination;
  }

  /**
   * @param working The working folder used as the EPUB root (assumed empty)
   */
  public void setWorking(File working) {
    this.working = working;
  }

  /**
   * @param config The export configuration file (optional)
   */
  public void setConfig(File config) {
    this.config = config;
  }

  /**
   * @param css The CSS file overriding the default stylesheet (optional)
   */
  public void setCSS(File css) {
    this.css = css;
  }

  /**
   * @param media The folder containing the images to include (optional)
   */
  public void setMedia(File media) {
    this.media = media;
  }

  /**
   * @param componentsName The name of the components folder (default "components")
   */
  public void setComponentsName(String componentsName) {
    this.componentsName = componentsName;
  }

  /**
   * @param logger Receives progress messages
   */
  public void setLogger(Consumer<String> logger) {
    this.logger = logger != null ? logger : message -> {};
  }

  // Process
  // ----------------------------------------------------------------------------------------------

  /**
   * Converts the PSML to an EPUB.
   *
   * @throws EPubException If the conversion failed
   */
  public void process() throws EPubException {
    if (this.source == null) throw new EPubException("Source must be specified");
    if (this.destination == null) throw new EPubException("Destination must be specified");
    if (this.working == null) throw new EPubException("Working folder must be specified");

    File processDir = this.source.getAbsoluteFile().getParentFile();
    File componentsDir = new File(processDir, this.componentsName);
    File metainf = new File(this.working, "META-INF");
    File oebps = new File(this.working, "OEBPS");
    File xhtml = new File(oebps, "xhtml");
    File mediaOut = new File(oebps, Config.MEDIA_FOLDER);
    File styles = new File(oebps, "styles");
    for (File dir : List.of(metainf, xhtml, mediaOut, styles)) {
      Files.ensureDirectoryExists(dir);
    }

    try {
      // Configuration
      String configURL = configURL();
      Map<String, String> configDocumentTypes = configDocumentTypes(this.config);
      Set<String> noteTypes = Stream.of("footnotes", "endnotes")
              .map(configDocumentTypes::get)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      Map<String, String> parameters = new HashMap<>();
      parameters.put("config-url", configURL);

      // 1. PSML to XHTML (flattened)
      if (!componentsDir.isDirectory()) {
        this.logger.accept("Components folder not found: "+componentsDir);
      }
      this.logger.accept("Transforming PSML to XHTML");
      Templates toXHTML = XSLT.getTemplatesFromResource(RESOURCES + "xslt/psml-to-xhtml.xsl");
      Map<String, File> outputs = new TreeMap<>();
      Map<File, String> fileDocumentTypes = listInputs(processDir, componentsDir, noteTypes);
      for (File psml : fileDocumentTypes.keySet()) {
        String basename = psml.getName().substring(0, psml.getName().length() - ".psml".length());
        File previous = outputs.put(basename, psml);
        if (previous != null)
          throw new EPubException("Duplicate file name "+psml.getName()+" in "+previous.getParent()+" and "+psml.getParent());
        XSLT.transform(psml, new File(xhtml, basename + ".xhtml"), toXHTML, parameters, this.errorListener);
      }

      // 2. Media
      if (this.media != null) {
        this.logger.accept("Copying media");
        File[] files = this.media.listFiles(File::isFile);
        if (files != null) {
          for (File f : files) {
            java.nio.file.Files.copy(f.toPath(), new File(mediaOut, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }

      // 3. Package, navigation and NCX
      this.logger.accept("Generating content.opf, nav.xhtml and toc.ncx");
      Map<String, String> opfParameters = new HashMap<>(parameters);
      opfParameters.put("xhtml-folder", xhtml.toURI().toString());
      opfParameters.put("image-list", listImages(mediaOut));
      opfParameters.put("xhtml-list", String.join(",", outputs.keySet()));
      XSLT.transform(this.source, new File(oebps, "content.opf"),
          XSLT.getTemplatesFromResource(RESOURCES + "xslt/root-to-opf.xsl"), opfParameters, this.errorListener);

      Map<String, String> navParameters = new HashMap<>(parameters);
      String glossaryType = configDocumentTypes.get("glossary");
      String bibType = configDocumentTypes.get("bibliography");
      if (glossaryType != null || bibType != null) {
        for (Map.Entry<File, String> entry : fileDocumentTypes.entrySet()) {
          if (entry.getValue().equals(glossaryType)) {
            navParameters.put("glossary-filename", entry.getKey().getName());
            glossaryType = null; // get first file only
          }
          if (entry.getValue().equals(bibType)) {
            navParameters.put("bibliography-filename", entry.getKey().getName());
            bibType = null; // get first file only
          }
          if (bibType == null && glossaryType == null) {
            break;
          }
        }
      }
      XSLT.transform(this.source, new File(oebps, "nav.xhtml"),
          XSLT.getTemplatesFromResource(RESOURCES + "xslt/root-to-nav.xsl"), navParameters, this.errorListener);

      XSLT.transform(this.source, new File(oebps, "toc.ncx"),
          XSLT.getTemplatesFromResource(RESOURCES + "xslt/root-to-ncx.xsl"), parameters, this.errorListener);

      // 4. Static files
      copyResource("static/mimetype", new File(this.working, "mimetype"));
      copyResource("static/META-INF/container.xml", new File(metainf, "container.xml"));
      File stylesheet = new File(styles, "epub.css");
      if (this.css != null) {
        java.nio.file.Files.copy(this.css.toPath(), stylesheet.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } else {
        copyResource("static/OEBPS/styles/epub.css", stylesheet);
      }

    } catch (IOException ex) {
      throw new EPubException("Failed to export epub", ex);
    }

    // 5. Zip
    this.logger.accept("Creating EPUB "+this.destination.getName());
    File parent = this.destination.getAbsoluteFile().getParentFile();
    if (parent != null) Files.ensureDirectoryExists(parent);
    ZipUtils.zipEpub(this.working, this.destination);
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * @return the URL of the configuration or of the empty default configuration.
   */
  private String configURL() {
    if (this.config != null) return this.config.toURI().toString();
    URL url = PSMLProcessor.class.getClassLoader().getResource(RESOURCES + "empty-config.xml");
    if (url == null) throw new EPubException("Missing resource "+RESOURCES + "empty-config.xml");
    return url.toString();
  }

  /**
   * @return a map of element,document type of the footnotes, endnotes,
   * glossary and bibliography specified in the configuration.
   */
  private static Map<String, String> configDocumentTypes(File config) {
    Map<String, String> typesMap = new HashMap<>();
    if (config == null) return typesMap;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Element root = factory.newDocumentBuilder().parse(config).getDocumentElement();

      // Expanded list to include glossary and bibliography
      for (String name : List.of("footnotes", "endnotes", "glossary", "bibliography")) {
        NodeList nodes = root.getElementsByTagName(name);
        for (int i = 0; i < nodes.getLength(); i++) {
          String type = ((Element) nodes.item(i)).getAttribute("documenttype");
          if (!type.isEmpty()) {
            typesMap.put(name, type);
          }
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      throw new EPubException("Unable to read configuration " + config, ex);
    }
    return typesMap;
  }

  /**
   * Lists the PSML files to transform: those directly in the process folder and all those in the
   * components folder, except the footnotes and endnotes documents (configured document types) which
   * are inlined in the documents citing them.
   *
   * @return a map of each PSML file to its document type (empty string if none), in no particular order.
   */
  private static Map<File, String> listInputs(File processDir, File componentsDir, Set<String> noteTypes) throws IOException {
    Map<File, String> inputs = new HashMap<>();
    File[] roots = processDir.listFiles(f -> f.isFile() && f.getName().endsWith(".psml"));
    if (roots != null) {
      for (File root : roots) {
        inputs.put(root, documentType(root));
      }
    }
    if (componentsDir.isDirectory()) {
      try (Stream<Path> paths = java.nio.file.Files.walk(componentsDir.toPath())) {
        paths.filter(p -> p.toString().endsWith(".psml") && java.nio.file.Files.isRegularFile(p))
                .map(Path::toFile)
                .forEach(f -> {
                  String type = documentType(f);
                  if (!noteTypes.contains(type)) inputs.put(f, type);
                });
      }
    }
    return inputs;
  }

  /**
   * Returns the type of the PSML document.
   *
   * <p>Only the start of the document is parsed: parsing stops at the first element.
   *
   * @return the <code>type</code> attribute of the document element or an empty string.
   */
  private static String documentType(File psml) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    try (InputStream in = java.nio.file.Files.newInputStream(psml.toPath())) {
      XMLStreamReader reader = factory.createXMLStreamReader(in);
      try {
        while (reader.hasNext()) {
          if (reader.next() == XMLStreamConstants.START_ELEMENT) {
            String type = reader.getAttributeValue(null, "type");
            return type != null ? type : "";
          }
        }
      } finally {
        reader.close();
      }
    } catch (IOException | XMLStreamException ex) {
      throw new EPubException("Unable to read "+psml, ex);
    }
    return "";
  }

  /**
   * @return the comma-separated sorted list of image filenames in the specified folder.
   */
  private static String listImages(File folder) {
    File[] files = folder.listFiles(File::isFile);
    if (files == null) return "";
    return Stream.of(files)
        .map(File::getName)
        .filter(name -> {
          int dot = name.lastIndexOf('.');
          return dot > 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
        })
        .sorted()
        .collect(Collectors.joining(","));
  }

  /**
   * Copies an export resource from the classpath.
   */
  private static void copyResource(String resource, File to) throws IOException {
    try (InputStream in = PSMLProcessor.class.getClassLoader().getResourceAsStream(RESOURCES + resource)) {
      if (in == null) throw new EPubException("Missing resource "+RESOURCES + resource);
      java.nio.file.Files.copy(in, to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

}
