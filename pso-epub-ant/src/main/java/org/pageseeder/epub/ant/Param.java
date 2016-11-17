/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.pageseeder.epub.ant;


/**
 * Used to store a Parameter value.
 *
 * <p>The parameter value is specified for this task in form of the following nested element:
 * <pre>{@code
 *   <param name="cell" value="D2"/>
 *   <param name="title" value="My Document"/>
 * }</pre>
 *
 * @author Jean-Baptiste Reure
 */
public class Param {

  /**
   * The name of the parameter.
   */
  private String name = null;

  /**
   * The value of the parameter.
   */
  private String expression = null;

  public Param() {
    // do nothing
  }

  /**
   * Sets the name of the parameter.
   *
   * @param name The name of the parameter
   */
  public void setName(String n) {
    this.name = n;
  }

  /**
   * Sets the value of the parameter.
   *
   * @param e The value of the parameter
   */
  public void setExpression(String e) {
    this.expression = e;
  }

  /**
   * get the name of the parameter.
   *
   * @return the name of the parameter
   */
  public String getName() {
    return this.name;
  }

  /**
   * get the value of the parameter.
   *
   * @return the value of the parameter
   */
  public String getValue() {
    return this.expression;
  }
}
