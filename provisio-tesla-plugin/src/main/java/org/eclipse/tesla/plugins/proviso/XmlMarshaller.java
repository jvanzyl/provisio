package org.eclipse.tesla.plugins.proviso;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class XmlMarshaller<T> implements Marshaller<T> {

  public void unmarshall(T object, Xpp3Dom dom) {
    unmarshall(object, dom, Thread.currentThread().getContextClassLoader());
  }

  public void unmarshall(T object, Xpp3Dom dom, ClassLoader classLoader) {    
    // setter
    // private field
    ComponentConfigurator configurator = new BasicComponentConfigurator();
    PlexusConfiguration configuration = new XmlPlexusConfiguration(dom);
    try {
      ClassRealm classRealm;
      if (classLoader instanceof ClassRealm) {
        classRealm = (ClassRealm) classLoader;
      } else {
        ClassWorld classWorld = new ClassWorld();
        classRealm = classWorld.newRealm("test", classLoader);
      }
      configurator.configureComponent(object, configuration, classRealm);
    } catch (ComponentConfigurationException e) {
      e.printStackTrace();
    } catch (DuplicateRealmException e) {
      e.printStackTrace();
    }
  }
}
