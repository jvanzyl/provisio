package io.tesla.proviso.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Entry {    
  String getName();  
  InputStream getInputStream() throws IOException;  
  long getSize();
  void writeEntry(OutputStream outputStream) throws IOException;
  // this should be general filemode
  int getFileMode();
}
