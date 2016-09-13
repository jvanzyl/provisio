package io.provis.jenkins.config.templates;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Parent;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLWriter;

public class XmlMerger {

  private String name;
  private Document base;

  public XmlMerger(String name) {
    this.name = name;
  }

  public void merge(String s) {
    merge(XMLParser.parse(s));
  }

  public void merge(File f) throws IOException {
    merge(XMLParser.parse(f));
  }

  public void merge(Reader r) throws IOException {
    merge(new XMLParser().parse(new XMLIOSource(r)));
  }

  public void merge(InputStream in) throws IOException {
    merge(new XMLParser().parse(new XMLIOSource(in)));
  }

  private void merge(Document doc) {
    if (base == null) {
      base = doc;
      base.setEncoding("UTF-8");
    } else {
      if (!mergeDoc(doc, base)) {
        throw new IllegalStateException("Cannot merge " + name + ", root elements differ");
      }
    }
  }

  public void finish(Writer w) throws IOException {
    base.toXML(new XMLWriter(w));
    w.flush();
  }

  public void finish(OutputStream out) throws IOException {
    Writer w = new OutputStreamWriter(out, "UTF-8");
    finish(w);
  }

  public String finish() {
    return base.toXML();
  }

  private boolean mergeDoc(Document from, Document to) {
    Element rootFrom = from.getRootElement();
    String appendPath = rootFrom.getAttributeValue("appendPath");
    if (appendPath != null) {
      return appendPath(rootFrom, to, appendPath);
    }

    String replacePath = rootFrom.getAttributeValue("replacePath");
    if (replacePath != null) {
      return replacePath(rootFrom, to, replacePath);
    }

    Element rootTo;
    String mergePath = rootFrom.getAttributeValue("mergePath");
    if (mergePath != null) {

      Element child = to.getChild(mergePath);
      if (child == null) {
        throw new IllegalStateException("Cannot merge " + name + " to path " + mergePath);
      }
      rootTo = child;

    } else if (rootFrom.getName().equals(to.getRootElement().getName())) {

      rootTo = to.getRootElement();

    } else {

      return false;

    }

    merge(rootFrom, rootTo);
    return true;
  }

  private boolean appendPath(Element from, Parent to, String appendPath) {
    Element append = to.getChild(appendPath);
    if (append == null) {
      throw new IllegalStateException("Cannot append " + name + " to path " + appendPath);
    }
    append.addNode(cleanup(from.copy()));
    return true;
  }

  private boolean replacePath(Element from, Parent to, String replacePath) {
    Element replace = to.getChild(replacePath);
    if (replace == null) {

      // if nothing to replace, then append it to parent
      int lastSlash = replacePath.lastIndexOf('/');
      if (lastSlash == -1) {
        throw new IllegalStateException("Cannot replace path " + replacePath + " with " + name);
      }
      Parent p = to.getChild(replacePath.substring(0, lastSlash));
      if (p == null) {
        throw new IllegalStateException("Cannot append/replace path " + replacePath + " with " + name);
      }
      p.addNode(cleanup(from.copy()));

    } else {

      replace(replace, cleanup(from.copy()));

    }
    return true;
  }

  private void processElem(Element from, Parent to) {
    String merge = from.getAttributeValue("merge");
    if ("replace".equals(merge)) {

      // find first element by the same name and merge into that one
      Element other = to.getChild(from.getName());
      if (other != null) {
        replace(other, cleanup(from.copy()));
        return;
      }

    } else if (Boolean.parseBoolean(merge)) {

      // find first element by the same name and merge into that one
      Element other = to.getChild(from.getName());
      if (other != null) {
        merge(from, other);
        return;
      }

    }

    to.addNode(cleanup(from.copy()));
  }

  private void replace(Element what, Element with) {
    Parent parent = what.getParent();
    int idx = parent.nodeIndexOf(what);
    what.remove();
    parent.addNode(idx, with);
  }

  private Element cleanup(Element elem) {
    elem.removeAttribute("merge");
    elem.removeAttribute("mergePath");
    elem.removeAttribute("appendPath");
    elem.removeAttribute("replacePath");
    for (Element ch : elem.getChildren()) {
      cleanup(ch);
    }
    return elem;
  }

  private void merge(Element from, Element to) {
    for (Element ch : from.getChildren()) {
      processElem(ch, to);
    }
  }

}
