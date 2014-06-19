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

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import javax.inject.Qualifier;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.ParamType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Internal implementation for a Bean
 */
@Module
public class EventManagerJavaee extends EventManager
{
  public EventManagerJavaee(InjectManager cdiManager)
  {
    super(cdiManager);
  }
  
  protected <X,Z> ObserverMethodImpl 
  createObserverMethodJavaee(Observes observes,
                             Bean<X> bean,
                             AnnotatedMethod<Z> beanMethod,
                             Type eventType,
                             HashSet<Annotation> bindingSet)
  {
    ObserverMethodImpl<X,Z> observerMethod;
    
    switch (observes.during()) {
    case BEFORE_COMPLETION:
      observerMethod 
        = new ObserverMethodBeforeCompletionImpl(getCdiManager(), 
                                                 bean, 
                                                 beanMethod,
                                                 eventType, 
                                                 bindingSet);
      break;
      
    case AFTER_COMPLETION:
      observerMethod 
        = new ObserverMethodAfterCompletionImpl(getCdiManager(), 
                                                bean, 
                                                beanMethod,
                                                eventType, 
                                                bindingSet);
      break;
      
    case AFTER_SUCCESS:
      observerMethod 
        = new ObserverMethodAfterSuccessImpl(getCdiManager(), 
                                             bean, 
                                             beanMethod,
                                             eventType, 
                                             bindingSet);
      break;
      
    case AFTER_FAILURE:
      observerMethod 
        = new ObserverMethodAfterFailureImpl(getCdiManager(), 
                                             bean, 
                                             beanMethod,
                                             eventType, 
                                             bindingSet);
      break;
      
    default:
      observerMethod = super.createObserverMethod(observes, bean, beanMethod, 
                                                  eventType, bindingSet);
      break;
    }
    
    return observerMethod;
  }
}
