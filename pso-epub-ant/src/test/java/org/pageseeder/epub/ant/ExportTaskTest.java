package org.pageseeder.epub.ant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the export-epub task.
 */
public class ExportTaskTest {

  /** Entries not compared with the expected EPUB. */
  private static final Set<String> IGNORE = Set.of("OEBPS/styles/epub.css");

  @TempDir
  Path tmp;

  /** The expected EPUB for configurations without glossary and bibliography. */
  private static final String BASELINE = "expected/system_report2.epub";

  /**
   * Keeps a copy of the EPUB produced by the test (if any) in the folder specified by the
   * 'epub.test.output' system property, named after the test, e.g. testExportWithFullConfig.epub
   */
  @AfterEach
  public void keepOutput(TestInfo info) throws Exception {
    String folder = System.getProperty("epub.test.output");
    File epub = this.tmp.resolve("out.epub").toFile();
    String name = info.getTestMethod().map(Method::getName).orElse("unknown");
    if (folder == null || !epub.isFile() || name.equals("testValidatorRejectsInvalidEpub")) return;
    Path output = Path.of(folder);
    Files.createDirectories(output);
    Files.copy(epub.toPath(), output.resolve(name + ".epub"), StandardCopyOption.REPLACE_EXISTING);
  }

  @Test
  public void testExportMatchesExpected() throws Exception {
    File epub = export("export-baseline.xml");
    EpubAssert.assertSameEpub(resource(BASELINE), epub, IGNORE);
    EpubXml.load(epub).assertLinksResolve();
    EpubValidator.assertValid(epub);

    // Without an override, the stylesheet is the bundled one
    try (InputStream css = ExportTaskTest.class.getResourceAsStream("/org/pageseeder/epub/export/static/OEBPS/styles/epub.css")) {
      assertArrayEquals(css.readAllBytes(), EpubAssert.read(epub).get("OEBPS/styles/epub.css"));
    }
  }

  @Test
  public void testCssOverride() throws Exception {
    ExportTask task = newTask(resource("process/system_report.psml"));
    task.setConfig(resource("config/export-baseline.xml"));
    task.setCSS(resource("css/override.css"));
    task.execute();
    File epub = this.tmp.resolve("out.epub").toFile();
    assertArrayEquals(Files.readAllBytes(resource("css/override.css").toPath()),
        EpubAssert.read(epub).get("OEBPS/styles/epub.css"));
    EpubAssert.assertSameEpub(resource(BASELINE), epub, IGNORE);
    EpubValidator.assertValid(epub);
  }

  @Test
  public void testComponentsNameOverride() throws Exception {
    // Copy the PSML renaming the components folder
    Path process = this.tmp.resolve("process");
    Path source = resource("process").toPath();
    try (Stream<Path> paths = Files.walk(source)) {
      for (Path p : paths.collect(Collectors.toList())) {
        String relative = source.relativize(p).toString().replace('\\', '/');
        if (relative.equals("components") || relative.startsWith("components/")) {
          relative = "chapters" + relative.substring("components".length());
        }
        Path target = process.resolve(relative);
        if (Files.isDirectory(p)) Files.createDirectories(target);
        else Files.copy(p, target);
      }
    }
    Path root = process.resolve("system_report.psml");
    String psml = new String(Files.readAllBytes(root), StandardCharsets.UTF_8);
    Files.write(root, psml.replace("href=\"components/", "href=\"chapters/").getBytes(StandardCharsets.UTF_8));

    // Default components name: the components referenced in the TOC are not found
    ExportTask defaultTask = newTask(root.toFile());
    defaultTask.setConfig(resource("config/export-baseline.xml"));
    defaultTask.setWorking(this.tmp.resolve("work-default").toFile());
    defaultTask.setDest(this.tmp.resolve("default.epub").toFile());
    assertThrows(BuildException.class, defaultTask::execute);

    // Overridden components name
    ExportTask task = newTask(root.toFile());
    task.setConfig(resource("config/export-baseline.xml"));
    task.setComponentsName("chapters");
    task.execute();
    File epub = this.tmp.resolve("out.epub").toFile();
    EpubAssert.assertSameEpub(resource(BASELINE), epub, IGNORE);
    EpubValidator.assertValid(epub);
  }

  @Test
  public void testValidatorRejectsInvalidEpub() throws Exception {
    // Make sure epubcheck actually detects problems: remove a content file listed in the manifest
    File epub = export("export-baseline.xml");
    File broken = this.tmp.resolve("broken.epub").toFile();
    try (ZipFile in = new ZipFile(epub);
         ZipOutputStream out = new ZipOutputStream(new FileOutputStream(broken))) {
      for (ZipEntry entry : Collections.list(in.entries())) {
        if (entry.getName().equals("OEBPS/xhtml/system_report-03.xhtml")) continue;
        ZipEntry copy = new ZipEntry(entry.getName());
        if (entry.getMethod() == ZipEntry.STORED) {
          copy.setMethod(ZipEntry.STORED);
          copy.setSize(entry.getSize());
          copy.setCompressedSize(entry.getSize());
          copy.setCrc(entry.getCrc());
        }
        out.putNextEntry(copy);
        try (InputStream data = in.getInputStream(entry)) {
          data.transferTo(out);
        }
        out.closeEntry();
      }
    }
    assertThrows(AssertionError.class, () -> EpubValidator.assertValid(broken));
  }

  @Test
  public void testExportWithEmptyConfig() throws Exception {
    File epub = export(null);
    EpubXml xml = EpubXml.load(epub);

    // No footnote popups: footnotes and endnotes are ordinary pages
    for (String name : xml.xhtmlEntries()) {
      assertEquals(0, xml.count(name, "//h:aside"), "Unexpected aside in "+name);
      assertEquals(0, xml.count(name, "//h:a[.//h:div or .//h:p or .//h:section or .//h:aside or .//h:dl"
          + " or .//h:ul or .//h:ol or .//h:table]"), "Block content in link in "+name);
    }
    String opf = "OEBPS/content.opf";
    for (String page : List.of("footnotes", "endnotes")) {
      assertTrue(xml.has("OEBPS/xhtml/"+page+".xhtml"), "Missing "+page+".xhtml");
      assertEquals(1, xml.count(opf, "//opf:manifest/opf:item[@id='xhtml-"+page+"'][@href='xhtml/"+page+".xhtml']"));
      assertEquals(1, xml.count(opf, "//opf:spine/opf:itemref[@idref='xhtml-"+page+"'][@linear='no']"));
    }

    // Footnote, endnote, glossary and citation references are plain xrefs
    assertEquals("f3", xml.value("OEBPS/xhtml/system_report-10.xhtml", "//h:a[@class='xref'][@href='footnotes.xhtml#166991-3']"));
    assertEquals("e1", xml.value("OEBPS/xhtml/system_report-05.xhtml", "//h:a[@class='xref'][@href='endnotes.xhtml#167021-1']"));
    assertEquals("(Wikipedia)", xml.value("OEBPS/xhtml/system_report-10.xhtml", "//h:a[@class='xref'][@href='bibliography.xhtml#167034-2']"));
    assertEquals("SR", xml.value("OEBPS/xhtml/system_report-04.xhtml", "//h:a[@class='xref'][@href='definitions.xhtml#167002-default']"));
    xml.assertLinksResolve();

    // No glossary/bibliography landmarks
    assertNoLandmark(xml, "glossary");
    assertNoLandmark(xml, "bibliography");

    // Fallback metadata
    assertEquals("urn:pageseeder:ebl1-syst-port-3u4v", xml.value(opf, "//dc:identifier"));
    assertEquals("System Report", xml.value(opf, "//dc:title"));
    assertEquals("en", xml.value(opf, "//dc:language"));
    assertEquals(0, xml.count(opf, "//dc:creator | //dc:publisher | //dc:subject | //dc:rights | //dc:date"));
    assertEquals(0, xml.count(opf, "//opf:meta[starts-with(@property, 'schema:') or starts-with(@property, 'a11y:')"
        + " or @property='dcterms:conformsTo']"));
    assertEquals(1, xml.count(opf, "//opf:meta[@property='dcterms:modified']"));
    EpubValidator.assertValid(epub);
  }

  @Test
  public void testExportWithGlossary() throws Exception {
    File epub = export("export-glossary.xml");
    EpubXml xml = EpubXml.load(epub);

    // Definitions as a dl in the glossary document
    String definitions = "OEBPS/xhtml/definitions.xhtml";
    assertEquals(1, xml.count(definitions, "//h:section[@epub:type='glossary'][@role='doc-glossary']/h:dl[@class='glossary']"));
    assertEquals(5, xml.count(definitions, "//h:dl[@class='glossary']/h:dt[@epub:type='glossterm']/h:dfn"));
    assertEquals(5, xml.count(definitions, "//h:dl[@class='glossary']/h:dd[@epub:type='glossdef']"));
    assertEquals("Berlioz", xml.value(definitions, "//h:dt[@id='167029-default']/h:dfn"));
    assertTrue(xml.value(definitions, "//h:dd[@id='167029-def']").contains("Berlioz is an open source Java library"));
    assertEquals(0, xml.count(definitions, "//h:dd//h:h1"), "Terms should not be repeated in definitions");

    // References to terms are noterefs to hidden copies of the definitions used in the same file
    String chapter = "OEBPS/xhtml/system_report-04.xhtml";
    assertEquals(2, glossrefs(xml, chapter));
    assertEquals(1, glossrefs(xml, "OEBPS/xhtml/system_report-12.xhtml"));
    assertEquals(1, glossrefs(xml, definitions));
    assertEquals("#gl-167002", xml.value(chapter, "//h:a[@class='glossref'][.='SR']/@href"));
    assertEquals("#gl-167029", xml.value(definitions, "//h:a[@class='glossref']/@href"));
    assertGlossaryNotes(xml, chapter, "167002", "166978");
    assertGlossaryNotes(xml, "OEBPS/xhtml/system_report-12.xhtml", "166983");
    assertGlossaryNotes(xml, definitions, "167029");
    String note = "//h:section[@class='popup-notes']/h:aside[@id='gl-166978']";
    assertEquals("Non-functional", xml.value(chapter, note+"/h:p[@class='glossary-term']/h:dfn"));
    assertTrue(xml.value(chapter, note).contains("Anything not related to the visible functionality"));
    assertEquals(0, xml.count(chapter, note+"//h:a"), "Links in popup notes");
    assertEquals(0, xml.count(chapter, note+"//*[@id]"), "IDs in popup notes");
    for (String name : xml.xhtmlEntries()) {
      if (!name.endsWith("nav.xhtml"))
        assertEquals(0, xml.count(name, "//h:a[contains(@href, '-default')][not(@class='glossref')]"), "Plain term link in "+name);
    }
    xml.assertLinksResolve();

    // Other entries unchanged
    // Landmarks
    assertLandmark(xml, "glossary", "xhtml/definitions.xhtml#166979-1-1-1", "Definitions");
    assertNoLandmark(xml, "bibliography");

    // Other entries unchanged
    Set<String> changed = Set.of("OEBPS/xhtml/definitions.xhtml", "OEBPS/xhtml/system_report-04.xhtml",
        "OEBPS/xhtml/system_report-12.xhtml", "OEBPS/nav.xhtml", "OEBPS/styles/epub.css");
    EpubAssert.assertSameEntries(resource(BASELINE), epub, unchanged(epub, changed));
    EpubValidator.assertValid(epub);
  }

  @Test
  public void testExportWithBibliography() throws Exception {
    File epub = export("export-bibliography.xml");
    EpubXml xml = EpubXml.load(epub);
    assertBibliography(xml);
    xml.assertLinksResolve();

    // Other entries unchanged
    assertLandmark(xml, "bibliography", "xhtml/bibliography.xhtml#167034-1-0-1", "Bibliography");
    assertNoLandmark(xml, "glossary");

    // Other entries unchanged
    Set<String> changed = Set.of("OEBPS/xhtml/bibliography.xhtml", "OEBPS/xhtml/system_report-10.xhtml",
        "OEBPS/nav.xhtml", "OEBPS/styles/epub.css");
    EpubAssert.assertSameEntries(resource(BASELINE), epub, unchanged(epub, changed));
    EpubValidator.assertValid(epub);
  }

  @Test
  public void testExportWithFullConfig() throws Exception {
    File epub = export("export-full.xml");
    EpubXml xml = EpubXml.load(epub);
    assertBibliography(xml);
    assertEquals(5, xml.count("OEBPS/xhtml/definitions.xhtml", "//h:dl[@class='glossary']/h:dd[@epub:type='glossdef']"));
    assertEquals(2, glossrefs(xml, "OEBPS/xhtml/system_report-04.xhtml"));
    assertLandmark(xml, "glossary", "xhtml/definitions.xhtml#166979-1-1-1", "Definitions");
    assertLandmark(xml, "bibliography", "xhtml/bibliography.xhtml#167034-1-0-1", "Bibliography");
    assertEquals(1, xml.count("OEBPS/xhtml/system_report-10.xhtml", "//h:section[@class='footnotes']/h:aside[@epub:type='footnote']"));
    xml.assertLinksResolve();
    EpubValidator.assertValid(epub);
  }

  // helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Asserts the bibliography entries and the citations.
   */
  private static void assertBibliography(EpubXml xml) {
    String bibliography = "OEBPS/xhtml/bibliography.xhtml";
    assertEquals(1, xml.count(bibliography, "//h:section[@epub:type='bibliography'][@role='doc-bibliography']"
        + "/h:ol[@class='bibliography'][@role='list']"));
    assertEquals(3, xml.count(bibliography, "//h:ol/h:li[@epub:type='biblioentry']"));
    assertEquals(0, xml.count(bibliography, "//h:dl"), "Entries should not be rendered as properties");
    assertTrue(xml.value(bibliography, "//h:li[@id='167034-1']").contains("Jacobson, I. (1992)."));
    assertEquals(1, xml.count(bibliography, "//h:li[@id='167034-1']/h:p/h:em"));
    assertEquals("MathML", xml.value(bibliography, "normalize-space(//h:li[@id='167034-3'])"));

    String chapter = "OEBPS/xhtml/system_report-10.xhtml";
    String citation = "//h:a[@class='biblioref'][@epub:type='noteref'][@role='doc-biblioref']";
    assertEquals(3, xml.count(chapter, citation));
    assertEquals("(Wikipedia Inc, 2017)", xml.value(chapter, citation+"[@href='#bib-167034-2']"));
    assertEquals("(Jacobson, 1992)", xml.value(chapter, citation+"[@href='#bib-167034-1']"));
    assertEquals("(W3C)", xml.value(chapter, citation+"[@href='#bib-167034-3']"));

    // Hidden copies of the cited entries only in the file citing them
    String notes = "//h:section[@class='popup-notes']/h:aside[@epub:type='footnote'][@class='biblio-note']";
    assertEquals(3, xml.count(chapter, notes));
    assertTrue(xml.value(chapter, notes+"[@id='bib-167034-1']").contains("Jacobson, I. (1992)."));
    for (String name : xml.xhtmlEntries()) {
      if (!name.equals(chapter)) assertEquals(0, xml.count(name, notes), "Unexpected citation notes in "+name);
    }
  }

  /**
   * Asserts that the landmarks in nav.xhtml contain exactly one link of the specified type.
   */
  private static void assertLandmark(EpubXml xml, String type, String href, String title) {
    String landmark = "//h:nav[@epub:type='landmarks']//h:a[@epub:type='"+type+"']";
    assertEquals(1, xml.count("OEBPS/nav.xhtml", landmark), "Landmark "+type);
    assertEquals(href, xml.value("OEBPS/nav.xhtml", landmark+"/@href"));
    assertEquals(title, xml.value("OEBPS/nav.xhtml", landmark));
  }

  /**
   * Asserts that the landmarks in nav.xhtml contain no link of the specified type.
   */
  private static void assertNoLandmark(EpubXml xml, String type) {
    assertEquals(0, xml.count("OEBPS/nav.xhtml", "//h:nav[@epub:type='landmarks']//h:a[@epub:type='"+type+"']"),
        "Unexpected landmark "+type);
  }

  /**
   * Asserts that the file contains exactly one hidden popup copy of each specified definition.
   */
  private static void assertGlossaryNotes(EpubXml xml, String name, String... uriids) {
    String notes = "//h:section[@class='popup-notes']/h:aside[@epub:type='footnote'][@class='glossary-note']";
    assertEquals(uriids.length, xml.count(name, notes), "Glossary notes in "+name);
    for (String uriid : uriids) {
      assertEquals(1, xml.count(name, notes+"[@id='gl-"+uriid+"']"), "Missing note gl-"+uriid+" in "+name);
    }
  }

  /**
   * @return the number of glossary term references in the specified entry
   */
  private static int glossrefs(EpubXml xml, String name) {
    return xml.count(name, "//h:a[@class='glossref'][@epub:type='noteref'][@role='doc-glossref']");
  }

  /**
   * @return the names of the entries in the EPUB except the specified ones
   */
  private static Set<String> unchanged(File epub, Set<String> changed) throws java.io.IOException {
    Set<String> names = new java.util.TreeSet<>(EpubAssert.read(epub).keySet());
    names.removeAll(changed);
    return names;
  }

  /**
   * Exports the sample PSML with the specified config.
   *
   * @param config The config file name in the export/config test resources (may be null)
   */
  private File export(String config) throws URISyntaxException {
    ExportTask task = newTask(resource("process/system_report.psml"));
    if (config != null) task.setConfig(resource("config/"+config));
    task.execute();
    return this.tmp.resolve("out.epub").toFile();
  }

  private ExportTask newTask(File source) throws URISyntaxException {
    ExportTask task = new ExportTask();
    task.setProject(new Project());
    task.setSrc(source);
    task.setDest(this.tmp.resolve("out.epub").toFile());
    task.setWorking(this.tmp.resolve("work").toFile());
    task.setMedia(resource("images"));
    return task;
  }

  /**
   * @return the test resource under export/
   */
  static File resource(String path) throws URISyntaxException {
    URL url = ExportTaskTest.class.getResource("/export/"+path);
    if (url == null) throw new IllegalArgumentException("Missing test resource export/"+path);
    return new File(url.toURI());
  }

}
