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

import java.util.EventListener;

/**
 * A listener for connection events, implemented by the application
 * server.
 */
public interface ConnectionEventListener extends EventListener {
  /**
   * Notifies that an application has closed the connection.
   */
  public void connectionClosed(ConnectionEvent event);
  
  /**
   * Notifies that a local transaction has started.
   */
  public void localTransactionStarted(ConnectionEvent event);
  
  /**
   * Notifies that a local transaction has committed.
   */
  public void localTransactionCommitted(ConnectionEvent event);
  
  /**
   * Notifies that a local transaction has rolled back.
   */
  public void localTransactionRolledback(ConnectionEvent event);
  
  /**
   * Notifies that a connection error has occurred.
   */
  public void connectionErrorOccurred(ConnectionEvent event);
}