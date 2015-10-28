/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.log;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.caucho.v5.amp.misc.MailService;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.log.impl.LogHandlerBase;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.AlarmListener;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Sends formatted messages to mail
 */
public class MailHandler extends LogHandlerBase implements AlarmListener
{
  private static final Logger log
    = Logger.getLogger(MailHandler.class.getName());
  private static final L10N L = new L10N(MailHandler.class);

  private long _delayTime = 60000L;
  private long _timeIntervalMin = 3 * 60 * 60000L;

  private long _lastMailTime;

  private StringBuilder _text;
  private Alarm _alarm;

  private MailService _mailService = new MailService();

  public MailHandler()
  {
    _alarm = new Alarm(this);
  }

  /**
   * Sets the delay time, i.e. how long the service should accumulate
   * messages before sending them.
   */
  public void setDelayTime(Period period)
  {
    _delayTime = period.getPeriod();
  }

  /**
   * Sets the delay time, i.e. how long the service should accumulate
   * messages before sending them.
   */
  public void setMailIntervalMin(Period period)
  {
    _timeIntervalMin = period.getPeriod();
  }

  /**
   * Sets the mail session
   */
  public void setMailSession(Session session)
  {
    _mailService.setSession(session);
  }

  /**
   * Sets a property
   */
  public void setProperty(String key, String value)
  {
    _mailService.setProperty(key, value);
  }

  /**
   * Sets properties
   */
  public void setProperties(Properties props)
  {
    _mailService.setProperties(props);
  }

  /**
   * Adds a 'to'
   */
  public void addTo(String to)
    throws AddressException
  {
    _mailService.addTo(new InternetAddress(to));
  }

  /**
   * Initialize the handler
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    _mailService.init();
  }

  @Override
  protected void deliverLog(String log)
  {
    boolean isStartAlarm = false;
    synchronized (this) {
      if (_text == null) {
        isStartAlarm = true;
        _text = new StringBuilder();
      }

      _text.append(log);
    }

    if (isStartAlarm) {
      long delta = _lastMailTime + _timeIntervalMin - CurrentTime.getCurrentTime();

      if (delta < _delayTime)
        delta = _delayTime;

      _alarm.queue(delta);
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.log.AbstractLogHandler#processFlush()
   */
  @Override
  protected void processFlush()
  {
    // TODO Auto-generated method stub
    
  }

  /**
   * Flushes the buffer.
   */
  public void flush()
  {
  }

  public void handleAlarm(Alarm alarm)
  {
    String text = null;

    synchronized (this) {
      if (_text != null)
        text = _text.toString();
      _text = null;
    }

    _lastMailTime = CurrentTime.getCurrentTime();

    if (text != null)
      _mailService.send(text);
  }

  @PreDestroy()
  public void close()
  {
    String text = null;

    synchronized (this) {
      if (_text != null)
        text = _text.toString();
      _text = null;
    }

    if (text != null)
      _mailService.send(text);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
