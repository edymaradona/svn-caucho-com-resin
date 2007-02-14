/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;

import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.Map;

/**
 * a Property that multiplexes among several child properties.
 */
public class MultiProperty extends Property {
  private static final L10N L = new L10N(MultiProperty.class);
  private Map<QName,Property> _qnameMap;
  private Map<Class,Property> _classMap;

  public MultiProperty(Map<QName,Property> qnameMap,
                       Map<Class,Property> classMap)
  {
    _qnameMap = qnameMap;
    _classMap = classMap;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    Property property = _qnameMap.get(in.getName());

    if (property == null)
      throw new JAXBException(L.l("Unexpected element {0}", in.getName()));

    return property.read(u, in, previous);
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    XMLEvent event = in.peek();

    QName readQname = event.asStartElement().getName();

    Property property = _qnameMap.get(readQname);

    if (property == null)
      throw new JAXBException(L.l("Unexpected element {0}", readQname));

    return property.read(u, in, previous);
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws JAXBException
  {
    QName nodeQname = JAXBUtil.qnameFromNode(node.getNode());

    Property property = _qnameMap.get(nodeQname);

    if (property == null)
      throw new JAXBException(L.l("Unexpected element {0}", nodeQname));

    return property.bindFrom(binder, node, previous);
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Property property = _classMap.get(obj.getClass());

      if (property == null)
        throw new JAXBException(L.l("Unexpected object {0}", obj));

      property.write(m, out, obj, qname);
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Property property = _classMap.get(obj.getClass());

      if (property == null)
        throw new JAXBException(L.l("Unexpected object {0}", obj));

      property.write(m, out, obj, qname);
    }
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws JAXBException
  {
    if (obj != null) {
      Property property = _classMap.get(obj.getClass());

      if (property == null)
        throw new JAXBException(L.l("Unexpected object {0}", obj));

      return property.bindTo(binder, node, obj, qname);
    }

    return null;
  }

  public String getSchemaType()
  {
    // XXX
    return "";
  }

  public boolean isXmlPrimitiveType()
  {
    // XXX
    return false;
  }

  public String getMaxOccurs()
  {
    return "unbounded";
  }
}
