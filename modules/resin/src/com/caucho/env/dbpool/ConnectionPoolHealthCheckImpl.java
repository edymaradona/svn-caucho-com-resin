/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.env.dbpool;

import com.caucho.env.health.HealthCheckResult;
import com.caucho.env.health.HealthStatus;
import com.caucho.env.health.HealthStatusListener;
import com.caucho.health.check.AbstractHealthCheck;
import com.caucho.util.L10N;

/**
 * Health check for the connection pool.
 */
// TODO: this should be a treated as a triggered health check rather than scheduled check
public class ConnectionPoolHealthCheckImpl 
  extends AbstractHealthCheck
  implements HealthStatusListener
{
  private static final L10N L = new L10N(ConnectionPoolHealthCheckImpl.class);

  private HealthCheckResult _currentResult
    = new HealthCheckResult(HealthStatus.OK);

  @Override
  public HealthCheckResult checkHealth()
  {
    return _currentResult;
  }

  @Override
  public void updateHealthStatus(Object source, 
                                 HealthStatus status, String message)
  {
    if (source instanceof ConnectionPool)
      _currentResult = new HealthCheckResult(status, message);
  }
}
