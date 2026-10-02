package org.pageseeder.epub.ant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Assertions for comparing EPUB files.
 */
final class EpubAssert {

  private EpubAssert() {
  }

  /**
   * Asserts that both EPUBs have the same entries in the same order, that <code>mimetype</code>
   * is first and stored, and that each entry has identical content (except the ignored ones
   * which only need to be present).
   *
   * @param expected The expected EPUB
   * @param actual   The actual EPUB
   * @param ignore   Entries whose content is not compared
   */
  static void assertSameEpub(File expected, File actual, Set<String> ignore) throws IOException {
    Map<String, byte[]> exp = read(expected);
    Map<String, byte[]> act = read(actual);
    assertMimetypeFirst(actual);
    assertEquals(new ArrayList<>(exp.keySet()), new ArrayList<>(act.keySet()), "EPUB entries differ");
    for (Map.Entry<String, byte[]> e : exp.entrySet()) {
      if (!ignore.contains(e.getKey())) {
        assertSameContent(e.getKey(), e.getValue(), act.get(e.getKey()));
      }
    }
  }

  /**
   * Asserts that the specified entries are identical in both EPUBs.
   */
  static void assertSameEntries(File expected, File actual, Set<String> names) throws IOException {
    Map<String, byte[]> exp = read(expected);
    Map<String, byte[]> act = read(actual);
    for (String name : names) {
      assertTrue(exp.containsKey(name), "Missing expected entry "+name);
      assertTrue(act.containsKey(name), "Missing actual entry "+name);
      assertSameContent(name, exp.get(name), act.get(name));
    }
  }

  /**
   * Asserts that the first entry is an uncompressed <code>mimetype</code>.
   */
  static void assertMimetypeFirst(File epub) throws IOException {
    try (ZipFile zip = new ZipFile(epub)) {
      ZipEntry first = Collections.list(zip.entries()).get(0);
      assertEquals("mimetype", first.getName(), "mimetype must be the first entry");
      assertEquals(ZipEntry.STORED, first.getMethod(), "mimetype must be stored");
    }
  }

  /**
   * @return the entries of the EPUB in order mapped to their content.
   */
  static Map<String, byte[]> read(File epub) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (ZipFile zip = new ZipFile(epub)) {
      for (ZipEntry entry : Collections.list(zip.entries())) {
        try (InputStream in = zip.getInputStream(entry)) {
          entries.put(entry.getName(), in.readAllBytes());
        }
      }
    }
    return entries;
  }

  /**
   * @return the content of the entry as a UTF-8 string
   */
  static String entry(File epub, String name) throws IOException {
    byte[] data = read(epub).get(name);
    if (data == null) fail("Missing entry "+name);
    return new String(data, StandardCharsets.UTF_8);
  }

  private static void assertSameContent(String name, byte[] expected, byte[] actual) {
    if (!java.util.Arrays.equals(expected, actual)) {
      List<String> exp = List.of(new String(expected, StandardCharsets.UTF_8).split("\n", -1));
      List<String> act = List.of(new String(actual, StandardCharsets.UTF_8).split("\n", -1));
      int i = 0;
      while (i < exp.size() && i < act.size() && exp.get(i).equals(act.get(i))) i++;
      String message = "Entry "+name+" differs at line "+(i+1)
          +"\n  expected: "+(i < exp.size() ? exp.get(i) : "<EOF>")
          +"\n  actual:   "+(i < act.size() ? act.get(i) : "<EOF>");
      assertArrayEquals(expected, actual, message);
    }
  }

}
