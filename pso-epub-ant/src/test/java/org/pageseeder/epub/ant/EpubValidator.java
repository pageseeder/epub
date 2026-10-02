package org.pageseeder.epub.ant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.api.EpubCheck;
import com.adobe.epubcheck.messages.Message;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.util.DefaultReportImpl;

/**
 * Validates EPUB files using epubcheck.
 */
final class EpubValidator {

  private EpubValidator() {
  }

  /**
   * Asserts that epubcheck reports no fatal errors and no errors for the EPUB.
   *
   * <p>All messages (including warnings) are included in the failure message.
   *
   * @param epub The EPUB to validate
   */
  static void assertValid(File epub) {
    CollectingReport report = new CollectingReport(epub.getName());
    new EpubCheck(epub, report).doValidate();
    int errors = report.getFatalErrorCount() + report.getErrorCount();
    assertTrue(errors == 0, "epubcheck reported "+errors+" error(s):\n"+String.join("\n", report.messages));
    if (!report.messages.isEmpty()) {
      System.out.println("epubcheck messages for "+epub.getName()+":\n"+String.join("\n", report.messages));
    }
  }

  /**
   * A report which collects the messages as strings.
   */
  private static final class CollectingReport extends DefaultReportImpl {

    private final List<String> messages = new ArrayList<>();

    CollectingReport(String name) {
      super(name);
    }

    @Override
    public void message(Message message, EPUBLocation location, Object... args) {
      super.message(message, location, args);
      Severity severity = message.getSeverity();
      if (severity != Severity.SUPPRESSED && severity != Severity.USAGE && severity != Severity.INFO) {
        this.messages.add(severity+" "+message.getID()+" "+location+": "+message.getMessage(args));
      }
    }
  }

}
