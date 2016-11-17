package org.pageseeder.epub;

public final class EPubException extends RuntimeException {

  /** For Serializable. */
  private static final long serialVersionUID = 7626137888772450875L;

  public EPubException(String m) {
    super(m);
  }

  public EPubException(String m, Exception ex) {
    super(m, ex);
  }
}
