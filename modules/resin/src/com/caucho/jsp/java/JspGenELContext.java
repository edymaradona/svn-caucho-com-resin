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

package com.caucho.jsp.java;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import java.lang.reflect.Method;

/**
 * ELContext a context for EL parsing.
 */
public class JspGenELContext extends ELContext 
{
  private final JavaJspGenerator _gen;
  
  public JspGenELContext(JavaJspGenerator gen)
  {
    _gen = gen;
  }

  public ELResolver getELResolver()
  {
    return null;
  }

  public FunctionMapper getFunctionMapper()
  {
    return _funMapper;
  }

  public VariableMapper getVariableMapper()
  {
    return null;
  }

  private final FunctionMapper _funMapper = new FunctionMapper()
  {
    public Method resolveFunction(String prefix, String localName)
    {
      return _gen.resolveFunction(prefix, localName);
    }

    @Override
    public void mapFunction(String prefix, String localName, Method method)
    {
      _gen.mapFunction(prefix, localName, method);
    }
  };
}
