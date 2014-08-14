package io.provis.model.io;

import io.provis.model.ActionDescriptor;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.Runtime;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class RuntimeReader {

  private XStream xstream;

  public RuntimeReader() {
    this(Collections.<ActionDescriptor> emptyList());
  }

  public RuntimeReader(List<ActionDescriptor> actions) {
    xstream = new XStream();
    xstream.alias("runtime", Runtime.class);
    xstream.useAttributeFor(Runtime.class, "id");
    xstream.addImplicitCollection(Runtime.class, "artifactSets");

    xstream.alias("artifactSet", ArtifactSet.class);
    xstream.aliasAttribute(ArtifactSet.class, "directory", "to");
    xstream.addImplicitCollection(ArtifactSet.class, "artifacts");

    xstream.alias("artifact", ProvisioArtifact.class);

    xstream.registerConverter(new MapToAttributesConverter());
    xstream.registerConverter(new ArtifactConverter(actions));

    for (ActionDescriptor action : actions) {
      for (String attributeForProperty : action.attributes()) {
        xstream.useAttributeFor(action.getImplementation(), attributeForProperty);
      }
    }
  }

  public Runtime read(InputStream inputStream, Map<String, String> variables) {
    return (Runtime) xstream.fromXML(new InterpolatingInputStream(inputStream, variables));
  }

  public Runtime read(InputStream inputStream) {
    return (Runtime) xstream.fromXML(inputStream);
  }

  public class ArtifactConverter implements Converter {
    private Map<String, ActionDescriptor> actionMap;
    public ArtifactConverter(List<ActionDescriptor> actions) {
      this.actionMap = Maps.newHashMap();
      for (ActionDescriptor actionDescriptor : actions) {
        actionMap.put(actionDescriptor.getName(), actionDescriptor);
      }
    }
    @Override
    public boolean canConvert(Class type) {
      if (ProvisioArtifact.class.isAssignableFrom(type)) {
        return true;
      }
      return false;
    }
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    }
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
      String coordinate = reader.getAttribute("id");
      ProvisioArtifact artifact = new ProvisioArtifact(coordinate);
      while (reader.hasMoreChildren()) {
        reader.moveDown();
        String actionName = reader.getNodeName();
        ActionDescriptor actionDescriptor = actionMap.get(actionName);
        if (actionDescriptor != null) {
          artifact.addAction((ProvisioningAction) context.convertAnother(artifact, actionDescriptor.getImplementation()));
        }
        reader.moveUp();
      }
      return artifact;
    }
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
