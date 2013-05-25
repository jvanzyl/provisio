package io.provis.action;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

@Named("testAction0")
public class TestAction0 implements Action {

  private String scalar;
  private String literal;
  private List<String> list;
  private Map<String, String> map;

  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  public String getScalar() {
    return scalar;
  }

  public void setScalar(String scalar) {
    this.scalar = scalar;
  }

  public String getLiteral() {
    return literal;
  }

  public void setLiteral(String literal) {
    this.literal = literal;
  }

  public List<String> getList() {
    return list;
  }

  public void setList(List<String> list) {
    this.list = list;
  }

  public Map<String, String> getMap() {
    return map;
  }

  public void setMap(Map<String, String> map) {
    this.map = map;
  }
  
  
}
