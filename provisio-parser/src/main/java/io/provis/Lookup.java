package io.provis;

import io.provis.model.Action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.eclipse.aether.artifact.Artifact;

@Named
public class Lookup {

  private Map<String,Provider<Action>> actions;
  
  @Inject
  public Lookup(Map<String,Provider<Action>> actions) {
    this.actions = actions;
  }
    
  // ----------------------------------------------------------------------
  // Container lookup methods
  // ----------------------------------------------------------------------

  public Action lookupAction(String key) {   
    return actions.get(key).get();
  }
  
  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  public void setObjectProperty(Object o, String propertyName, Object value) {
            
    Class<?> c = o.getClass();

    // First see if name is a property ala javabeans
    String methodSuffix = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1, propertyName.length());
    String methodName = "set" + methodSuffix;
    
    //
    //TODO Handles bad case where in tests i set the output directory to null 
    //
    if(value == null) {
      return;
    }
    
    Class<? extends Object> type = value.getClass();
    
    if (List.class.isAssignableFrom(type)) {
      type = List.class;
    } else if (Map.class.isAssignableFrom(type)) {
      type = Map.class;
    } else if(Boolean.class.isAssignableFrom(type)) {
      // We need to use an Object but we need to look at the target field
      type = boolean.class;
    } else if (Artifact.class.isAssignableFrom(type)){
      type = Artifact.class;
    }

    Method m = getMethod(c, methodName, new Class[] { type });
    if (m != null) {
      try {
        invokeMethod(m, o, value);
        //System.out.println("for object " + o + " set " + propertyName + " --> " + value);        
      } catch (Exception e) {
        //System.out.println("Can't set property " + propertyName + " using method set" + methodSuffix + " from " + c.getName() + " instance: " + e);
      }
    } else {
      //System.out.println(">>>>>> There is no property " + propertyName + " " + type + " using method set" + methodSuffix + " from " + c.getName() + " instance");      
    }
  }

  protected Object newInstance(String name) {
    Object o = null;
    try {
      o = Class.forName(name).newInstance();
    } catch (Exception e) {
      //System.out.println("can't make instance of " + name);
    }
    return o;
  }

  protected Method getMethod(Class c, String methodName, Class[] args) {
    Method m;
    try {
      m = c.getMethod(methodName, args);
    } catch (NoSuchMethodException nsme) {
      m = null;
    }
    return m;
  }

  protected Object invokeMethod(Method m, Object o) throws IllegalAccessException, InvocationTargetException {
    return invokeMethod(m, o, null);
  }

  protected Object invokeMethod(Method m, Object o, Object value) throws IllegalAccessException, InvocationTargetException {
    Object[] args = null;
    if (value != null) {
      args = new Object[] {
        value
      };
    }
    value = m.invoke(o, args);
    return value;
  }
}
