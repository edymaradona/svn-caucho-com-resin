/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.java.gen;

import com.caucho.v5.java.JavaWriter;
import com.caucho.v5.util.L10N;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Generates the skeleton for a method call.
 */
public class MethodCallChain extends CallChain {
  private static L10N L = new L10N(MethodCallChain.class);

  private Method _method;
  private String _methodName;
  private Class []_parameterTypes;
  private Class _returnType;

  /**
   * Creates the chain.
   */
  public MethodCallChain()
  {
  }

  /**
   * Creates the chain with the method.
   */
  public MethodCallChain(Method method)
  {
    _method = method;
    _methodName = method.getName();
    _parameterTypes = method.getParameterTypes();
    _returnType = method.getReturnType();
  }

  /**
   * Creates the chain with the method.
   */
  public MethodCallChain(String methodName, Class []params, Class returnType)
  {
    _methodName = methodName;
    _parameterTypes = params;
    _returnType = returnType;
  }

  /**
   * Returns the method.
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Returns the method's parameter types.
   */
  public Class []getParameterTypes()
  {
    return _parameterTypes;
  }

  /**
   * Returns the method's return type.
   */
  public Class getReturnType()
  {
    return _returnType;
  }

  /**
   * Generates the code for the method call.
   *
   * @param out the writer to the output stream.
   * @param retVar the variable to hold the return value
   * @param var the object to be called
   * @param args the method arguments
   */
  public void generateCall(JavaWriter out, String retVar,
                           String var, String []args)
    throws IOException
  {
    if (getReturnType().getName().equals("void")) {
    }
    else if (retVar != null)
      out.print(retVar + " = ");
    else
      out.print("return ");

    if (var != null)
      out.print(var + ".");
    else
      out.print("super.");

    out.print(_methodName + "(");

    for (int i = 0; i < args.length; i++) {
      if (i != 0)
        out.print(", ");

      out.print(args[i]);
    }

    out.println(");");
  }
}
