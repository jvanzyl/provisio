package io.provis.model;

public interface Action {
  void execute(ProvisioContext context) throws Exception;
}
