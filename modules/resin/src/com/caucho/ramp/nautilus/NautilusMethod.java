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

package com.caucho.ramp.nautilus;

import com.caucho.amp.actor.MethodAmpBase;
import com.caucho.amp.spi.ActorAmp;
import com.caucho.amp.spi.HeadersAmp;

/**
 * Method ref for a nautilus queue.
 */
class NautilusMethod extends MethodAmpBase
{
  private final NautilusActor _actor;
  private final String _methodName;
  
  NautilusMethod(NautilusActor actor,
                 String methodName)
  {
    _actor = actor;
    _methodName = methodName;
  }

  @Override
  public String getName()
  {
    return _methodName;
  }

  @Override
  public void send(HeadersAmp headers,
                   ActorAmp actor,
                   Object []args)
  {
    _actor.send(_methodName, args);
  }
}