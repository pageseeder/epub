/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.util;

/**
 * @author Christophe Lauret
 * @version 01/03/2013
 *
 */
public class Paths {

  private Paths() {
  }

  /**
   *
   * @param path
   * @return
   */
  public static String toBase(String path) {
    String p = path.replace('\\', '/');
    return p.indexOf('/') != 0? p.substring(0, path.lastIndexOf('/')) +'/' : "";
  }

  /**
   *
   * @param path
   * @return
   */
  public static String toBackToRoot(String path) {
    StringBuilder back = new StringBuilder();
    String p = path.replace('\\', '/');
    int i = p.indexOf('/');
    while (i >= 0) {
      back.append("../");
      i = p.indexOf('/', i+1);
    }
    return back.toString();
  }

  /**
   *
   * @param path
   * @return
   */
  public static String getExtension(String path) {
    return path.indexOf('.') != 0? path.substring(path.lastIndexOf('.')) : "";
  }

  /**
   * Returns the lexically cleaned form of the path name, <i>usually</i> (but
   * not always) equivalent to the original. The following heuristics are used:
   *
   * <ul>
   * <li>empty string becomes .
   * <li>fold out ../ when possible
   * <li>fold out ./ when possible
   * <li>collapse multiple slashes
   * <li>delete trailing slashes (unless the path is just "/")
   * </ul>
   *
   * These heuristics do not always match the behavior of the filesystem. In
   * particular, consider the path {@code a/../b}, which {@code simplifyPath}
   * will change to {@code b}. If {@code a} is a symlink to {@code x}, {@code
   * a/../b} may refer to a sibling of {@code x}, rather than the sibling of
   * {@code a} referred to by {@code b}.
   *
   * @since Guava release 10
   */
  public static String simplify(String pathname) {
    if (pathname.length() == 0) {
      return ".";
    }
    char[] name = pathname.toCharArray();
    // In place, rewrite name to compress multiple /, eliminate ., and process ..

    boolean rooted = (name[0] == '/');

    // invariants:
    //  p points at beginning of path element we're considering.
    //  q points just past the last path element we wrote (no slash).
    //  dotdot points just past the point where .. cannot backtrack
    //    any further (no slash).
    int firstNonSlash = rooted ? 1 : 0;
    int p = firstNonSlash;
    int q = firstNonSlash;
    int dotdot = firstNonSlash;
    while (p < name.length) {
      if (name[p] == '/') {
        /* null element */
        p++;
      } else if (name[p] == '.' && sep(name, p + 1)) {
        /* don't count the separator in case it is null */
        p += 1;
      } else if (name[p] == '.'
          && ((p + 1) < name.length && name[p + 1] == '.')
          && sep(name, p + 2)) {
        p += 2;
        if (q > dotdot) {
          /* can backtrack */
          while (--q > dotdot && name[q] != '/') {
          }
        } else if (!rooted) {
          /* /.. is / but ./../ is .. */
          if (name[q] != name[0] ||
              (q != 0 && name.length >= q+3 && name[0] == '.' && name[name.length - 1] != '/')) {
            name[q++] = '/';
          }
          name[q++] = '.';
          name[q++] = '.';
          dotdot = q;
        }
      } else {
        /* real path element */
        if (name[q] != name[firstNonSlash]) {
          name[q++] = '/';
        }
        while ((q < name.length && p < name.length) &&
            (name[q] = name[p]) != '/') {
          p++;
          q++;
        }
      }
    }
    return new String(name, 0, q);
  }

  private static boolean sep(char[] a, int pos) {
    return (pos == a.length) || (a[pos] == '/');
  }
}
