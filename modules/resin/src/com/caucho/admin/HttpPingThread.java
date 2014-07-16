/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */
package com.caucho.admin;

import io.baratine.core.Startup;

import javax.annotation.PostConstruct;

import com.caucho.config.Configurable;
import com.caucho.config.Unbound;
import com.caucho.config.types.Period;
import com.caucho.env.health.HealthSubSystem;
import com.caucho.health.action.Restart;
import com.caucho.health.check.HealthSystemHealthCheck;
import com.caucho.health.check.HttpStatusHealthCheck;
import com.caucho.health.predicate.IfCriticalRechecked;
import com.caucho.util.L10N;

/**
 * @Deprecated
 * @see com.caucho.health.check.HttpStatusHealthCheck
 */
@Unbound
@Startup
@Deprecated
@Configurable
public class HttpPingThread extends HttpStatusHealthCheck
{
  private static final L10N L = new L10N(HttpPingThread.class);
  
  private HealthSubSystem _healthService;

  public HttpPingThread()
  {
    _healthService = HealthSubSystem.getCurrent();
    if (_healthService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          HttpPingThread.class.getSimpleName(),
                                          HealthSubSystem.class.getSimpleName()));
    }
  }
  
  @PostConstruct
  @Override
  public void init()
  {
    super.init();
    
    // for backwards compatability
    if (getDelegate() != null) {
      Restart restart = new Restart();
      restart.add(new IfCriticalRechecked(getDelegate()));
      restart.init();
    }
  }
  
  /**
   * Sets the response time to detect a frozen jvm
   * @deprecated use HealthSystemHealthCheck.setFreezeTimeout
   */
  @Deprecated
  @Configurable
  public void setFreezeTimeout(Period freezeTimeout)
  {
    HealthSystemHealthCheck check = new HealthSystemHealthCheck();
    check.setFreezeTimeout(freezeTimeout);
  }
  
  /**
   * Sets the sleep time between pings
   * @deprecated use HealthSystem.setPeriod
   */
  @Deprecated
  @Configurable
  public void setSleepTime(Period sleepTime)
  {
    _healthService.setPeriod(sleepTime.getPeriod());
  }

  /**
   * Sets the sleep time on startup
   * @deprecated use HealthSystem.setDelay
   */
  @Deprecated
  @Configurable
  public void setInitialSleepTime(Period initialSleepTime)
  {
    _healthService.setDelay(initialSleepTime.getPeriod());
  }

  /**
   * Sets the retry time between failing pings
   * @deprecated use HealthSystem.setRetryPeriod
   */
  @Deprecated
  @Configurable
  public void setRetryTime(Period retryTime)
  {
    _healthService.setRecheckPeriod(retryTime.getPeriod());
  }

  /**
   * Sets the number of times to retry before giving up
   * @deprecated use HealthSystem.setRecheckMax
   */
  @Deprecated
  @Configurable
  public void setTryCount(int tryCount)
  {
    _healthService.setRecheckMax(tryCount);
  }
}