package ca.vanzyl.provisio.model;

public class ProvisioArchive {

  private final java.io.File file;
  private final String extension;

  public ProvisioArchive(java.io.File file, String extension) {
    this.file = file;
    this.extension = extension;
  }

  public java.io.File file() {
    return file;
  }

  public String extension() {
      return extension;
  }
}
