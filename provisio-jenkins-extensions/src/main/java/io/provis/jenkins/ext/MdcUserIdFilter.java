package io.provis.jenkins.ext;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.init.Terminator;
import hudson.model.User;
import hudson.util.PluginServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

// FIXME: this would be *much* better done by modifying the web.xml but for now use the extension-point even though has a window where the value may be incorrect

/**
 * Sets the MDC {@code userId} key for current user.
 *
 * @since 0.1
 */
public class MdcUserIdFilter extends PluginServletFilter {
  private static final Logger log = LoggerFactory.getLogger(MdcUserIdFilter.class);

  private static final String KEY = "userId";

  private static final String SYSTEM = "*SYSTEM";

  /**
   * Attempt to set the MDC "userId" key to the current user's id or "*SYSTEM" if unknown.
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    User user = User.current();
    log.trace("Current user: {}", user);

    // install current user's id or use *SYSTEM if unknown
    if (user != null) {
      MDC.put(KEY, user.getId());
    }
    else {
      MDC.put(KEY, SYSTEM);
    }

    try {
      super.doFilter(request, response, chain);
    }
    finally {
      MDC.remove(KEY);
    }
  }

  private static final MdcUserIdFilter filter = new MdcUserIdFilter();

  @Initializer(after = InitMilestone.STARTED)
  public static void install() throws Exception {
    PluginServletFilter.addFilter(filter);
    log.info("Installed");
  }

  @Terminator
  public static void uninstall() throws Exception {
    PluginServletFilter.removeFilter(filter);
    log.info("Removed");
  }
}
