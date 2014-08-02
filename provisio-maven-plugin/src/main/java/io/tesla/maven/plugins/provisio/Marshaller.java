package io.tesla.maven.plugins.provisio;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public interface Marshaller<T> {
  void unmarshall(T object, Xpp3Dom dom);
  void unmarshall(T object, Xpp3Dom dom, ClassLoader classLoader);
}
