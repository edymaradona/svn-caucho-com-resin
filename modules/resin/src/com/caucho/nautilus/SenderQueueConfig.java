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

package com.caucho.nautilus;



import java.util.function.Supplier;

import com.caucho.nautilus.encode.ClassSupplier;
import com.caucho.nautilus.encode.StringEncoder;


/**
 * client sender factory
 */
public interface SenderQueueConfig<M>
{
  SettleMode getSettleMode();
  
  Supplier<EncoderMessage<M>> getEncoderSupplier();
  
  public static class Builder<M>
  {
    private SettleMode _settleMode = SettleMode.ALWAYS;
    private Supplier<EncoderMessage<M>> _supplier;
    
    public static <M> Builder<M> create()
    {
      return new Builder<M>();
    }
    
    public SenderQueueConfig<M> build()
    {
      Supplier<EncoderMessage<M>> supplier = _supplier;
      
      if (supplier == null) {
        supplier = new ClassSupplier(StringEncoder.class);
      }
      
      return new ConfigImpl<M>(_settleMode,
                               _supplier);
    }
  }
  
  static class ConfigImpl<M> implements SenderQueueConfig<M>
  {
    private SettleMode _settleMode;
    private Supplier<EncoderMessage<M>> _supplier;
    
    ConfigImpl(SettleMode settleMode,
               Supplier<EncoderMessage<M>> supplier)
    {
      _settleMode = settleMode;
      _supplier = supplier;
    }
    
    @Override
    public SettleMode getSettleMode()
    {
      return _settleMode;
    }

    @Override
    public Supplier<EncoderMessage<M>> getEncoderSupplier()
    {
      return _supplier;
    }
  }
}
