package io.provis.model.v2;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class RuntimeReader {

  private XStream xstream;

  public RuntimeReader() {
    xstream = new XStream();
    xstream.alias("runtime", Runtime.class);
    xstream.useAttributeFor(Runtime.class, "id");
    xstream.addImplicitCollection(Runtime.class, "artifactSets");

    xstream.alias("artifactSet", ArtifactSet.class);
    xstream.aliasAttribute(ArtifactSet.class, "directory", "to");
    xstream.addImplicitCollection(ArtifactSet.class, "artifacts");

    xstream.alias("artifact", Artifact.class);
    xstream.useAttributeFor(Artifact.class, "id");
    xstream.addImplicitCollection(Artifact.class, "actions");

    xstream.alias("action", Action.class);
    xstream.useAttributeFor(Action.class, "id");
    xstream.registerConverter(new MapToAttributesConverter());
  }

  public Runtime read(InputStream inputStream, Map<String, String> variables) {
    return (Runtime) xstream.fromXML(new InterpolatingInputStream(inputStream, variables));
  }

  public Runtime read(InputStream inputStream) {
    return (Runtime) xstream.fromXML(inputStream);
  }

  public class MapToAttributesConverter implements Converter {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean canConvert(Class type) {
      return Map.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
      Map<String, String> map = (Map<String, String>) source;
      for (Map.Entry<String, String> entry : map.entrySet()) {
        writer.addAttribute(entry.getKey(), entry.getValue().toString());
      }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      Map<String, String> map = new HashMap<String, String>();
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        String key = reader.getAttributeName(i);
        String value = reader.getAttribute(key);
        map.put(key, value);
      }
      return map;
    }
  }
}
