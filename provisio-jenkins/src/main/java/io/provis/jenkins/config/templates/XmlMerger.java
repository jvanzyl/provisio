package io.provis.jenkins.config.templates;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import de.pdark.decentxml.Attribute;
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

  public void merge(String s){
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
      mergeDoc(doc, base);
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

  private void mergeDoc(Document from, Document to) {
    Element rootFrom = from.getRootElement();
    Element rootTo = to.getRootElement();
    if (!rootFrom.getName().equals(rootTo.getName())) {
      throw new IllegalStateException("Cannot merge " + name + ", <from> and <to> root elements differ: " + rootFrom.getName() + " != " + rootTo.getName());
    }
    merge(rootFrom, rootTo);
  }

  private void processElem(Element from, Parent to) {
    Attribute merge = from.getAttribute("merge");
    if (merge != null) {
      String value = merge.getValue();
      if (Boolean.parseBoolean(value)) {

        // find first element by the same name and merge into that one
        Element other = to.getChild(from.getName());
        if (other != null) {
          merge(from, other);
          return;
        }

      } else if ("replace".equals(value)) {

        // find first element by the same name and merge into that one
        Element other = to.getChild(from.getName());
        if (other != null) {
          int idx = to.nodeIndexOf(other);
          other.remove();
          to.addNode(idx, cleanup(from.copy()));
          return;
        }

      }
    }

    to.addNode(cleanup(from.copy()));
  }

  private Element cleanup(Element elem) {
    elem.removeAttribute("merge");
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
