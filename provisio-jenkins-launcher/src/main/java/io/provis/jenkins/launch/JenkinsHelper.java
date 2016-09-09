package io.provis.jenkins.launch;

import java.util.Collections;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.util.security.Constraint;

public class JenkinsHelper {
  //
  // http://stackoverflow.com/questions/9111759/http-error-503-accessing-jenkins-reason-service-unavailable
  //
  //<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  //  <Set name="contextPath">/jenkins</Set>
  //  <Set name="war"><SystemProperty name="jetty.home" default="."/>/webapps/jenkins.war</Set>
  //  <Get name="securityHandler">
  //    <Set name="loginService">
  //      <New class="org.eclipse.jetty.security.HashLoginService">
  //            <Set name="name">Jenkins Realm</Set>
  //            <Set name="config"><SystemProperty name="jetty.home" default="."/>/etc/realm.properties</Set>
  //      </New>
  //    </Set>
  //  </Get>
  //</Configure>   
   
  public static SecurityHandler securityHandler() {
    
    Constraint constraint = new Constraint();
    constraint.setName(Constraint.__FORM_AUTH);
    constraint.setRoles(new String[] { "user", "admin" });
    constraint.setAuthenticate(true);

    ConstraintMapping mapping = new ConstraintMapping();
    mapping.setPathSpec("/*");
    mapping.setConstraint(constraint);
        
    ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
    securityHandler.setConstraintMappings(Collections.singletonList(mapping));
    HashLoginService loginService = new HashLoginService("Jenkins Realm");
    securityHandler.setLoginService(loginService);

    //FormAuthenticator authenticator = new FormAuthenticator();
    //securityHandler.setAuthenticator(authenticator);
    //authenticator.setConfiguration(securityHandler);

    return securityHandler;
  }
}
