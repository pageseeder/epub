package org.pageseeder.epub.ant;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Sends XSLT warnings, errors and xsl:message output to the ANT log.
 */
final class AntErrorListener implements ErrorListener {

    private final Task task;

    AntErrorListener(Task task) {
        this.task = task;
    }

    @Override
    public void warning(TransformerException ex) {
        this.task.log("[xslt] " + ex.getMessageAndLocation(), Project.MSG_WARN);
    }

    @Override
    public void error(TransformerException ex) {
        this.task.log("[xslt] " + ex.getMessageAndLocation(), Project.MSG_ERR);
    }

    @Override
    public void fatalError(TransformerException ex) {
        this.task.log("[xslt] " + ex.getMessageAndLocation(), Project.MSG_ERR);
    }
}