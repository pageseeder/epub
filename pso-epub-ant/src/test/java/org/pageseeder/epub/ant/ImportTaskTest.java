package org.pageseeder.epub.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests for the import-epub task.
 */
public class ImportTaskTest {

  @TempDir
  Path tmp;

  @Test
  public void testImport() throws Exception {
    File out = this.tmp.resolve("out").toFile();
    File working = this.tmp.resolve("work").toFile();
    ImportTask task = new ImportTask();
    task.setProject(new Project());
    task.setSrc(resource("system_report.epub"));
    task.setDest(out);
    task.setWorking(working);
    task.execute();

    // All XHTML content files are pre-processed
    File[] preprocessed = new File(working, "preprocessed/OEBPS/xhtml").listFiles();
    assertTrue(preprocessed != null && preprocessed.length == 26, "Expected 26 pre-processed XHTML files");

    // Root document embeds one document per spine item
    File root = new File(out, "system_report.psml");
    assertTrue(root.isFile(), "Missing root document");
    List<String> embedded = attributes(parse(root), "blockxref", "href");
    assertEquals(27, embedded.size());
    for (String href : embedded) {
      assertTrue(new File(out, href).isFile(), "Missing embedded document "+href);
    }

    // Images are copied and referenced correctly
    File[] media = new File(out, "media").listFiles();
    assertTrue(media != null && media.length == 3, "Expected 3 images");
    File components = new File(out, "system_report");
    List<String> broken = new ArrayList<>();
    int images = 0;
    for (String href : embedded) {
      File component = new File(out, href);
      Document doc = parse(component);
      for (String src : attributes(doc, "image", "src")) {
        images++;
        if (!new File(components, src).isFile()) broken.add(component.getName()+" image -> "+src);
      }
      for (String xref : attributes(doc, "xref", "href")) {
        if (xref.isEmpty() || !new File(components, xref).isFile()) broken.add(component.getName()+" xref -> '"+xref+"'");
      }
    }
    assertEquals(3, images);
    assertTrue(broken.isEmpty(), "Broken references:\n"+String.join("\n", broken));

    // Content
    Document overview = parse(new File(components, "system_report-05.psml"));
    assertTrue(overview.getDocumentElement().getTextContent().contains("Document Overview"));
    assertEquals(2, overview.getElementsByTagName("image").getLength());
    assertFalse(attributes(overview, "xref", "href").isEmpty(), "Expected links to the definitions");
    Document functionality = parse(new File(components, "system_report-11.psml"));
    assertTrue(functionality.getDocumentElement().getTextContent().contains("Object-oriented design"));
  }

  // helpers
  // ----------------------------------------------------------------------------------------------

  private static List<String> attributes(Document doc, String element, String attribute) {
    List<String> values = new ArrayList<>();
    NodeList nodes = doc.getElementsByTagName(element);
    for (int i = 0; i < nodes.getLength(); i++) {
      values.add(((Element) nodes.item(i)).getAttribute(attribute));
    }
    return values;
  }

  private static Document parse(File file) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
  }

  private static File resource(String path) throws Exception {
    URL url = ImportTaskTest.class.getResource("/import/"+path);
    if (url == null) throw new IllegalArgumentException("Missing test resource import/"+path);
    return new File(url.toURI());
  }

}
