package io.provis.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class FileUtils {

  public static String readFileContent(final File file, final String encoding) {
    try {
      return readFileContent(new FileInputStream(file), encoding);
    } catch (final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static String readFileContentFromClasspath(final String path) {
    InputStream in = null;
    try {
      in = FileUtils.class.getClassLoader().getResourceAsStream(path);
      if (in == null) {
        throw new RuntimeException("Path " + path + " not found on classpath");
      }
      return readFileContent(in, null);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static String readFileContent(final InputStream is, final String encoding) {
    final StringBuilder content = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(newReader(is, encoding));
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.trim().startsWith("#")) {
          content.append(line).append("\n");
        }
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return content.toString();
  }

  private static Reader newReader(final InputStream is, final String encoding) throws IOException {
    if (encoding == null || encoding.length() <= 0) {
      return new InputStreamReader(is);
    }
    return new InputStreamReader(is, encoding);
  }

  public static String extension(final String name) {
    final int dotIdx = name.lastIndexOf(".");
    if (dotIdx > 0 && dotIdx < name.length() - 1) {
      return name.substring(dotIdx + 1).trim();
    }
    return null;
  }

  public static File safeSearchClasspath(final String name) {
    final File file = searchClasspath(name);
    if (file != null) {
      return file;
    }
    throw new RuntimeException(new FileNotFoundException(name));
  }

  public static File searchClasspath(final String name) {
    final URL resource = FileUtils.class.getClassLoader().getResource(name);
    if (resource != null) {
      final String resourceFile = resource.getFile();
      if (resourceFile != null) {
        return new File(resourceFile);
      }
    }
    return null;
  }

  public static void copy(final File from, final File to) {
    InputStream in = null;
    try {
      in = new FileInputStream(from);
      copy(in, to);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      close(in);
    }
  }

  public static void copy(final URL from, final File to) {
    InputStream in = null;
    try {
      in = from.openStream();
      copy(in, to);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      close(in);
    }
  }

  public static void copy(final InputStream in, final File to) {
    to.getParentFile().mkdirs();

    OutputStream out = null;

    try {
      out = new FileOutputStream(to);

      final byte[] buf = new byte[4096];
      int len = 0;

      while ((len = in.read(buf)) != -1) {
        out.write(buf, 0, len);
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      close(out);
    }
  }

  public static void close(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (final Throwable e) {
        // ignore
      }
    }
  }

  public static void delete(final File file) {
    delete(file, false);
  }

  public static void delete(final File file, final boolean lenient) {
    if (file == null) {
      return;
    }

    final Collection<File> undeletables = new ArrayList<File>();

    delete(file, undeletables);

    if (!undeletables.isEmpty()) {
      if (lenient) {
        // For JARs locked by not-yet-reclaimed class loaders, this is expected so handle it gracefully
        System.err.println("Failed to delete " + undeletables);
      } else {
        throw new IllegalStateException("Failed to delete " + undeletables);
      }
    }
  }

  private static void delete(final File file, final Collection<File> undeletables) {
    final String[] children = file.list();
    if (children != null) {
      for (final String child : children) {
        delete(new File(file, child), undeletables);
      }
    }

    if (!file.delete() && file.exists()) {
      undeletables.add(file.getAbsoluteFile());
    }
  }

  public static Properties loadProperties(final File file, final Properties props) {
    Properties result = props;
    if (result == null) {
      result = new Properties();
    }
    try {
      final FileInputStream fis = new FileInputStream(file);
      try {
        result.load(fis);
      } finally {
        fis.close();
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  public static void saveProperties(final File file, final Properties props) {
    try {
      FileOutputStream fos = new FileOutputStream(file);
      try {
        if (props != null) {
          props.store(fos, "");
        }
      } finally {
        fos.close();
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

}
