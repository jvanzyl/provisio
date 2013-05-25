package com.jcraft.jsch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.inject.EagerSingleton;

@Named
@Singleton
@EagerSingleton
public class JSchLog implements Logger {

  private final org.slf4j.Logger log;

  @Inject
  public JSchLog(org.slf4j.Logger log) {
    this.log = log;
    JSch.setLogger(this);
  }

  public boolean isEnabled(int level) {
    switch (level) {
    case Logger.DEBUG:
      return log.isDebugEnabled();
    case Logger.INFO:
      return log.isInfoEnabled();
    case Logger.WARN:
      return log.isWarnEnabled();
    case Logger.FATAL:
    case Logger.ERROR:
      return log.isErrorEnabled();
    default:
      return true;
    }
  }

  public void log(int level, String message) {
    switch (level) {
    case Logger.DEBUG:
      log.debug(message);
      break;
    case Logger.WARN:
      log.warn(message);
      break;
    case Logger.ERROR:
    case Logger.FATAL:
      log.error(message);
      break;
    default:
      log.info(message);
    }
  }

}
