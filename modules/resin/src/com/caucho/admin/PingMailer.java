/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import io.baratine.core.Startup;

import javax.annotation.PostConstruct;
import javax.mail.internet.AddressException;

import com.caucho.config.Configurable;
import com.caucho.config.Unbound;
import com.caucho.health.action.SendMail;
import com.caucho.health.predicate.IfHealthCritical;
import com.caucho.health.predicate.IfRechecked;
import com.caucho.util.L10N;

/**
 * @Deprecated
 * @see com.caucho.health.check.HttpStatusHealthCheck
 */
@Unbound
@Startup
@Deprecated
@Configurable
public class PingMailer extends HttpPingThread {
  static final L10N L = new L10N(PingMailer.class);

  // The mail destination
  private String _mailTo;
  
  // The mail sender
  private String _mailFrom;
  
  // The mail subject
  private String _subject = "Resin ping has failed";
  
  @PostConstruct
  public void init() 
  {
    SendMail sendMail = new SendMail();
    
    try {
      if (_mailFrom != null)
        sendMail.setFrom(_mailFrom);
      if (_mailTo != null)
        sendMail.addTo(_mailTo);
    } catch (AddressException e) {
      throw new IllegalArgumentException(e.toString(), e);
    }
    
    sendMail.add(new IfHealthCritical(getDelegate()));
    sendMail.add(new IfRechecked());
    sendMail.init();
    
    super.init();
  }

  /**
   * Gets the mail destination
   */
  public String getMailTo()
  {
    return _mailTo;
  }

  /**
   * Sets the mail destination
   */
  @Deprecated
  @Configurable
  public void setMailTo(String mailTo)
  {
    _mailTo = mailTo;
  }

  /**
   * Gets the mail sender
   */
  public String getMailFrom()
  {
    return _mailFrom;
  }

  /**
   * Sets the mail sender
   */
  @Deprecated
  @Configurable
  public void setMailFrom(String mailFrom)
  {
    _mailFrom = mailFrom;
  }

  /**
   * Gets the mail sender
   */
  public String getMailSubject()
  {
    return _subject;
  }

  /**
   * Sets the mail subject
   */
  @Deprecated
  @Configurable
  public void setMailSubject(String subject)
  {
    _subject = subject;
  }
}
