package io.provis.action.artifact.filter;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import io.provis.model.io.InterpolatingInputStream;
import io.tesla.proviso.archive.Selector;
import io.tesla.proviso.archive.UnarchivingEntryProcessor;

public class StandardFilteringProcessor implements UnarchivingEntryProcessor {

  Selector selector;
  Map<String, String> variables;

  public StandardFilteringProcessor(Map<String, String> variables) {
    this.variables = variables;
  }

  @Override
  public String processName(String name) {
    return name;
  }

  @Override
  public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
    ByteStreams.copy(new InterpolatingInputStream(inputStream, variables), outputStream);
  }
}