package io.provis.model;


public interface ActionDescriptor {
  String getName();
  Class<?> getImplementation();
  String[] attributes();
}
