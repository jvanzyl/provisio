package io.tesla.proviso.archive.source;

import io.tesla.proviso.archive.Entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

public class FileEntry implements Entry {
  
  private final String name;
  private final File file;
  
  public FileEntry(String name, File file) {
    this.name = name;
    this.file = file;
  }
  
  public String getName() {
    return name;
  }
  
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }
  
  public long getSize() {
    return file.length();
  }

  @Override
  public void writeEntry(OutputStream outputStream) throws IOException {
    Closer closer = Closer.create();
    try {
      InputStream entryInputStream = closer.register(getInputStream());
      ByteStreams.copy(entryInputStream, outputStream);
    } finally {
      closer.close();
    }    
  }

  @Override
  public int getFileMode() {
    return -1;
  }
}
