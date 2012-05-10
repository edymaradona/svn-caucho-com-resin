/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
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

package com.caucho.iiop.orb;

import com.caucho.vfs.*;
import com.caucho.iiop.*;

import java.util.logging.Logger;
import java.lang.reflect.Proxy;
import java.rmi.*;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.PortableRemoteObjectDelegate;
import java.io.IOException;

public class PortableRemoteObjectDelegateImpl
  implements PortableRemoteObjectDelegate
{
  public void exportObject(Remote obj)
    throws RemoteException
  {
    throw new UnsupportedOperationException();
  }
  
  public Remote toStub(Remote obj)
    throws NoSuchObjectException
  {
    throw new UnsupportedOperationException();
  }
  
  public void unexportObject(Remote obj)
    throws NoSuchObjectException
  {
    throw new UnsupportedOperationException();
  }
  
  public Object narrow(Object narrowFrom,
		       Class narrowTo)
    throws ClassCastException
  {
    try {
      if (narrowTo.isAssignableFrom(narrowFrom.getClass()))
	return narrowFrom;

      if (narrowFrom instanceof StubImpl) {
	StubImpl stub = (StubImpl) narrowFrom;

	if (! narrowTo.getName().startsWith("org.omg.")) {
	  org.omg.CORBA_2_3.portable.InputStream is = null;
    
	  org.omg.CORBA_2_3.portable.OutputStream os
	    = ((org.omg.CORBA_2_3.portable.OutputStream) stub._request("_is_a", true));

	  os.write_string("RMI:" + narrowTo.getName() + ":0");

	  is = ((org.omg.CORBA_2_3.portable.InputStream) stub._invoke(os));

	  boolean isA = is.read_boolean();
	  is.close();

	  if (! isA)
	    throw new ClassCastException(narrowFrom.getClass().getName());
	}

	StubMarshal stubMarshal = new StubMarshal(narrowTo);

	IiopProxyHandler handler = 
	  new IiopProxyHandler(stub.getORBImpl(),
			       stub,
			       stubMarshal);

	return Proxy.newProxyInstance(narrowTo.getClassLoader(),
				      new Class[] { narrowTo, IiopProxy.class },
				      handler);
      }
    
      throw new ClassCastException(narrowFrom.getClass().getName());
    } catch (ClassCastException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void connect(Remote target,
		      Remote source)
    throws RemoteException
  {
    throw new UnsupportedOperationException();
  }
}