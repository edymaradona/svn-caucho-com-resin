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

package com.caucho.ramp.nautilus;

import io.baratine.core.ServiceRef;

import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amp.actor.ActorAmpBase;
import com.caucho.amp.jamp.OutJamp;
import com.caucho.amp.message.HeadersNull;
import com.caucho.amp.spi.ActorAmp;
import com.caucho.amp.spi.ServiceRefAmp;
import com.caucho.nautilus.Nautilus;
import com.caucho.nautilus.ReceiverBuilder;
import com.caucho.nautilus.SenderBuilder;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.impl.BrokerNautilusImpl;
import com.caucho.nautilus.impl.NautilusSystem;
import com.caucho.util.L10N;

/**
 * Implementation of the nautilus actor.
 */
public class NautilusActor extends ActorAmpBase
{
  private static final Logger log
    = Logger.getLogger(NautilusActor.class.getName());
  
  private static final L10N L = new L10N(NautilusActor.class);
  
  private final String _address;
  private SenderQueue<String> _sender;

  private NautilusSystem _nautilus;
  
  public NautilusActor()
  {
    _nautilus = NautilusSystem.getCurrent();
    Objects.requireNonNull(_nautilus);
    
    _address = "queue:";
  }
  
  public NautilusActor(String address)
  {
    _address = address;
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  @Override
  public NautilusMethod getMethod(String methodName)
  {
    return new NautilusMethod(this, methodName);
  }
  
  @Override
  public ActorAmp onLookup(String path, ServiceRefAmp parentRef)
  {
    BrokerNautilusImpl.getCurrent();
    
    return new NautilusActor(getAddress() + path);
  }
  
  @Override
  public void consume(ServiceRef service)
  {
    Objects.requireNonNull(service);
    
    NautilusListener listener = new NautilusListener(service);
    
    ReceiverBuilder<String> builder = Nautilus.receiver();
    // XXX: save so it can be removed later
    
    builder.address(getAddress())
           .prefetch(64)
           .consume()
           .build(listener);
  }
  
  @Override
  public void subscribe(ServiceRef service)
  {
    Objects.requireNonNull(service);
    
    NautilusListener listener = new NautilusListener(service);
    
    ReceiverBuilder<String> builder = Nautilus.receiver();
    
    // XXX: save so it can be removed later
    builder.address(getAddress())
           .prefetch(64)
           .subscribe()
           .build(listener);
  }

  void send(String methodName, Object[] args)
  {
    try {
      OutJamp jOut = new OutJamp();
    
      StringWriter sOut = new StringWriter();
      
      jOut.init(sOut);
    
      jOut.send(HeadersNull.NULL, "", methodName, args);
      
      String value = sOut.toString();

      getSender().offer(value, 10, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  private SenderQueue<String> getSender()
  {
    if (_sender == null) {
      SenderBuilder<String> builder = Nautilus.sender();
      
      _sender = builder.address(_address).build();
    }
    
    return _sender;
  }
}
