/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.SimpleTag;

import com.caucho.util.*;
import com.caucho.jsp.*;

import com.caucho.make.PersistentDependency;

import com.caucho.log.Log;

import com.caucho.xml.QName;

/**
 * Generates the nodes for JSP code.
 */
public class JavaJspBuilder extends JspBuilder {
  public static final String JSTL_CORE_URI = "http://java.sun.com/jsp/jstl/core";
  public static final String JSTL_EL_CORE_URI = "http://java.sun.com/jstl/core";
  public static final String JSTL_RT_CORE_URI = "http://java.sun.com/jstl/core_rt";
  
  public static final String JSTL_FMT_URI = "http://java.sun.com/jsp/jstl/fmt";
  
  public static final String JSTL_EL_FMT_URI = "http://java.sun.com/jstl/fmt";
  public static final String JSTL_RT_FMT_URI = "http://java.sun.com/jstl/fmt_rt";
  
  public static final String JSTL_XML_URI = "http://java.sun.com/jsp/jstl/xml";
  
  public static final String JSTL_EL_XML_URI = "http://java.sun.com/jstl/xml";
  public static final String JSTL_RT_XML_URI = "http://java.sun.com/jstl/xml_rt";
  
  private static L10N L = new L10N(JavaJspBuilder.class);
  private static Logger log = Log.open(JavaJspBuilder.class);
  
  static final int IGNORE = 0;
  static final int DIRECTIVE_PAGE = IGNORE + 1;
  static final int TEXT = DIRECTIVE_PAGE + 1;
  static final int EXPR = TEXT + 1;
  static final int SCRIPTLET = EXPR + 1;
  static final int DECLARATION = SCRIPTLET + 1;
  static final int FUNCTION = DECLARATION + 1;
  static final int SET = FUNCTION + 1;
  static final int INCLUDE = SET + 1;
  static final int FORWARD = INCLUDE + 1;
  static final int REQUEST = INCLUDE + 1;
  static final int USE_BEAN = REQUEST + 1;
  static final int GET_PROPERTY = USE_BEAN + 1;
  static final int SET_PROPERTY = GET_PROPERTY + 1;
  static final int TAG = GET_PROPERTY + 1;
  static final int PLUGIN = TAG + 1;
  static final int ROOT = PLUGIN + 1;
  static final int JSP_TEXT = ROOT + 1;
  static final int JSP_ATTRIBUTE = JSP_TEXT + 1;
  static final int JSP_BODY = JSP_ATTRIBUTE + 1;
  
  static final int IF = JSP_BODY + 1;
  static final int FOREACH = IF + 1;
  static final int EXPR_EL = FOREACH + 1;

  static HashMap<QName,Class> _tagMap;
  static HashMap<QName,Class> _fastTagMap;
  
  private JavaJspGenerator _gen;
  private JspNode _rootNode;
  private JspNode _currentNode;
  private JspNode _openNode;

  private boolean _isPrototype;

  /**
   * Creates the JSP builder.
   */
  public JavaJspBuilder()
  {
  }

  /**
   * Returns the generator.
   */
  public JspGenerator getGenerator()
  {
    return _gen;
  }

  /**
   * Returns the root node.
   */
  public JspNode getRootNode()
  {
    return _rootNode;
  }

  /**
   * Sets the prototype mode.
   */
  public void setPrototype(boolean prototype)
  {
    _isPrototype = prototype;
  }

  /**
   * Gets the prototype mode.
   */
  public boolean isPrototype()
  {
    return _isPrototype;
  }
  
  /**
   * Starts the document
   */
  public void startDocument()
    throws JspParseException
  {
    try {
      // jsp/031a
      if (_rootNode != null)
	return;
      
      if (_parseState.isTag())
        _gen = new JavaTagGenerator(_tagManager);
      else
        _gen = new JavaJspGenerator(_tagManager);
      _gen.setParseState(getParseState());
      _gen.setJspCompiler(getJspCompiler());
      _gen.setJspParser(getJspParser());
      _gen.setRequireSource(getRequireSource());
    
      _rootNode = new JspTop();
      _rootNode.setParseState(getParseState());
      _rootNode.setGenerator(_gen);
      _rootNode.setStartLocation(_filename, _line);
      _gen.setRootNode(_rootNode);
      _gen.init();
      
      _currentNode = _rootNode;
    } catch (Exception e) {
      throw JspParseException.create(e);
    }
  }
  
  /**
   * Starts the document
   */
  public void endDocument()
    throws JspParseException
  {
  }

  /**
   * Starts an element.
   *
   * @param qname the name of the element to start
   */
  public void startElement(QName qname)
    throws JspParseException
  {
    Class cl;

    if (isFastJstl())
      cl = _fastTagMap.get(qname);
    else
      cl = _tagMap.get(qname);
    
    if (cl != null) {
      try {
        JspNode node = (JspNode) cl.newInstance();

        node.setGenerator(_gen);
        node.setParseState(_parseState);
        node.setQName(qname);
        node.setParent(_currentNode);
        node.setStartLocation(_filename, _line);

        _openNode = node;

        return;
      } catch (Exception e) {
        throw new JspParseException(e);
      }
    }

    if (isPrototype()) {
      JspXmlElement elt = new JspXmlElement();
      elt.setGenerator(_gen);
      elt.setParseState(_parseState);
      elt.setQName(qname);
      elt.setParent(_currentNode);
      elt.setStartLocation(_filename, _line);

      _openNode = elt;

      return;
    }
      
    if (JspNode.JSP_NS.equals(qname.getNamespace()))
      throw new JspParseException(L.l("unknown JSP <{0}>", qname.getName()));

    TagInfo tagInfo;

    try {
      tagInfo = _gen.getTag(qname);
    } catch (JspParseException e) {
      throw error(e);
    }

    if (tagInfo == null) {
      JspXmlElement elt = new JspXmlElement();
      elt.setGenerator(_gen);
      elt.setParseState(_parseState);
      elt.setQName(qname);
      elt.setParent(_currentNode);
      elt.setStartLocation(_filename, _line);

      _openNode = elt;

      return;
    }

    Class tagClass = null;
    
    try {
      tagClass = _gen.getTagClass(qname);
    } catch (ClassNotFoundException e) {
      tagClass = JspTagFileSupport.class;
      log.log(Level.FINE, e.toString(), e);
    } catch (Exception e) {
      throw error(e);
    }

    if (tagInfo == null)
      throw _gen.error(L.l("<{0}> is an unknown tag.", qname.getName()));

    if (tagInfo instanceof TagInfoExt) {
      TagInfoExt tagInfoExt = (TagInfoExt) tagInfo;

      ArrayList<PersistentDependency> dependList = tagInfoExt.getDependList();

      for (int i = 0; i < dependList.size(); i++) {
	_gen.addDepend(dependList.get(i));
      }
    }

    if (JspTagFileSupport.class.isAssignableFrom(tagClass)) {
      TagFileTag customTag = new TagFileTag();
      customTag.setGenerator(_gen);
      customTag.setParseState(_parseState);
      customTag.setQName(qname);
      customTag.setParent(_currentNode);
      customTag.setTagInfo(tagInfo);
      customTag.setTagClass(tagClass);

      _openNode = customTag;
      _openNode.setStartLocation(_filename, _line);
    }
    else if (Tag.class.isAssignableFrom(tagClass)) {
      CustomTag customTag = new CustomTag();
      customTag.setGenerator(_gen);
      customTag.setParseState(_parseState);
      customTag.setQName(qname);
      customTag.setParent(_currentNode);
      customTag.setTagInfo(tagInfo);
      customTag.setTagClass(tagClass);

      _openNode = customTag;
      _openNode.setStartLocation(_filename, _line);
    }
    else if (SimpleTag.class.isAssignableFrom(tagClass)) {
      CustomSimpleTag customTag = new CustomSimpleTag();
      customTag.setGenerator(_gen);
      customTag.setParseState(_parseState);
      customTag.setQName(qname);
      customTag.setParent(_currentNode);
      customTag.setTagInfo(tagInfo);
      customTag.setTagClass(tagClass);

      _openNode = customTag;
      _openNode.setStartLocation(_filename, _line);
    }
    else
      throw _gen.error(L.l("<{0}>: tag class {0} must either implement Tag or SimpleTag.", qname.getName(), tagClass.getName()));
  }

  /**
   * Starts a prefix mapping.
   *
   * @param prefix the xml prefix
   * @param uri the namespace uri
   */
  public void startPrefixMapping(String prefix, String uri)
    throws JspParseException
  {
    getParseState().pushNamespace(prefix, uri);

    _gen.addOptionalTaglib(prefix, uri);
  }

  /**
   * Adds an attribute to the element.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void attribute(QName name, String value)
    throws JspParseException
  {
    _openNode.addAttribute(name, value);
  }

  /**
   * Called when the attributes end.
   */
  public void endAttributes()
    throws JspParseException
  {
    _currentNode.addChild(_openNode);

    _currentNode = _openNode;
    _currentNode.setNamespace(_parseState.getNamespaces());
    _currentNode.endAttributes();
    _currentNode.setEndAttributeLocation(_filename, _line);
  }
  
  /**
   * Ends an element.
   *
   * @param qname the name of the element to end
   */
  public void endElement(String name)
    throws JspParseException
  {
    if (! _currentNode.getTagName().equals(name)) {
      throw error(L.l("Close tag </{0}> does not match the current tag, <{1}>.", name, _currentNode.getTagName()));
    }

    try {
      JspNode node = _currentNode;
      
      _currentNode = node.getParent();
      
      node.setEndLocation(_filename, _line);
      node.endElement();

      _currentNode.addChildEnd(node);
    } catch (JspLineParseException e) {
      throw e;
    } catch (JspParseException e) {
      throw error(e);
    } catch (Exception e) {
      throw new JspParseException(e);
    }
  }
  
  /**
   * Adds text.
   *
   * @param text the text to add
   */
  public void text(String text)
    throws JspParseException
  {
    if (_currentNode != null) {
      JspNode node = _currentNode.addText(text);

      if (node != null) {
	node.setStartLocation(_filename, _line);
      }
    }
  }
  
  /**
   * Adds text.
   *
   * @param text the text to add
   */
  public void text(String text, String srcFilename, int startLine, int endLine)
    throws JspParseException
  {
    if (_currentNode != null) {
      JspNode node = _currentNode.addText(text);

      if (node != null) {
	node.setStartLocation(srcFilename, startLine);
	node.setEndLocation(srcFilename, endLine);
      }
    }
  }
  
  /**
   * Returns the current node.
   */
  public JspNode getCurrentNode()
  {
    return _currentNode;
  }

  public JspParseException error(String message)
  {
    return new JspParseException(_filename + ":" + _line + ": " + message);
  }

  public JspParseException error(Throwable e)
  {
    if (e instanceof LineCompileException)
      return new JspLineParseException(e);
    else if (e instanceof CompileException)
      return new JspLineParseException(_filename + ":" + _line + ": " + e.getMessage(), e);
    else
      return new JspLineParseException(_filename + ":" + _line + ": " +
				   String.valueOf(e), e);
  }

  private static void addMap(HashMap<QName,Class> map,
			     String prefix,
			     String localName,
			     String uri,
			     Class cl)
  {
    map.put(new QName(prefix, localName, uri), cl);
    map.put(new QName(prefix, localName, "urn:jsptld:" + uri), cl);
  }
  
  static {
    _tagMap = new HashMap<QName,Class>();

    addMap(_tagMap, "jsp", "root", JspNode.JSP_NS, JspRoot.class);

    addMap(_tagMap, "jsp", "directive.page", JspNode.JSP_NS,
	   JspDirectivePage.class);
    addMap(_tagMap, "jsp", "directive.include", JspNode.JSP_NS,
	   JspDirectiveInclude.class);
    addMap(_tagMap, "jsp", "directive.cache", JspNode.JSP_NS,
	   NullTag.class);
    addMap(_tagMap, "jsp", "directive.taglib", JspNode.JSP_NS,
	   JspDirectiveTaglib.class);
    addMap(_tagMap, "jsp", "directive.attribute", JspNode.JSP_NS,
	   JspDirectiveAttribute.class);
    addMap(_tagMap, "jsp", "directive.variable", JspNode.JSP_NS,
	   JspDirectiveVariable.class);
    addMap(_tagMap, "jsp", "directive.tag", JspNode.JSP_NS,
	   JspDirectiveTag.class);
    
    addMap(_tagMap, "jsp", "expression", JspNode.JSP_NS,
	   JspExpression.class);
    addMap(_tagMap, "jsp", "scriptlet", JspNode.JSP_NS,
	   JspScriptlet.class);
    addMap(_tagMap, "jsp", "declaration", JspNode.JSP_NS,
	   JspDeclaration.class);
    addMap(_tagMap, "jsp", "useBean", JspNode.JSP_NS,
	   JspUseBean.class);
    addMap(_tagMap, "jsp", "getProperty", JspNode.JSP_NS,
	   JspGetProperty.class);
    addMap(_tagMap, "jsp", "setProperty", JspNode.JSP_NS,
	   JspSetProperty.class);
    addMap(_tagMap, "jsp", "include", JspNode.JSP_NS,
	   JspInclude.class);
    addMap(_tagMap, "jsp", "forward", JspNode.JSP_NS,
	   JspForward.class);
    addMap(_tagMap, "jsp", "param", JspNode.JSP_NS,
	   JspParam.class);
    addMap(_tagMap, "jsp", "plugin", JspNode.JSP_NS,
	   JspPlugin.class);
    addMap(_tagMap, "jsp", "params", JspNode.JSP_NS,
	   JspParams.class);
    addMap(_tagMap, "jsp", "fallback", JspNode.JSP_NS,
	   JspFallback.class);
    
    addMap(_tagMap, "jsp", "attribute", JspNode.JSP_NS,
	   JspAttribute.class);
    addMap(_tagMap, "jsp", "doBody", JspNode.JSP_NS,
	   JspDoBody.class);
    addMap(_tagMap, "jsp", "invoke", JspNode.JSP_NS,
	   JspInvoke.class);
    addMap(_tagMap, "jsp", "body", JspNode.JSP_NS,
	   JspBody.class);
    addMap(_tagMap, "jsp", "text", JspNode.JSP_NS,
	   JspText.class);
    addMap(_tagMap, "jsp", "element", JspNode.JSP_NS,
	   JspElement.class);
    addMap(_tagMap, "jsp", "output", JspNode.JSP_NS,
	   JspOutput.class);

    _fastTagMap = new HashMap<QName,Class>(_tagMap);

    // shortcut
    addMap(_fastTagMap, "resin-c", "out", JSTL_CORE_URI, JstlCoreOut.class);
    addMap(_fastTagMap, "resin-c", "out", JSTL_RT_CORE_URI, JstlCoreOut.class);
    addMap(_fastTagMap, "resin-c", "out", JSTL_EL_CORE_URI, JstlCoreOut.class);
    
    addMap(_fastTagMap, "resin-c", "set", JSTL_CORE_URI, JstlCoreSet.class);
    addMap(_fastTagMap, "resin-c", "set", JSTL_EL_CORE_URI, JstlCoreSet.class);
    addMap(_fastTagMap, "resin-c", "set", JSTL_RT_CORE_URI, JstlCoreSet.class);
    
    addMap(_fastTagMap, "resin-c", "remove", JSTL_CORE_URI,
	   JstlCoreRemove.class);
    addMap(_fastTagMap, "resin-c", "remove", JSTL_EL_CORE_URI,
	   JstlCoreRemove.class);
    addMap(_fastTagMap, "resin-c", "remove", JSTL_RT_CORE_URI,
	   JstlCoreRemove.class);
    
    addMap(_fastTagMap, "resin-c", "catch", JSTL_CORE_URI,
	   JstlCoreCatch.class);
    addMap(_fastTagMap, "resin-c", "catch", JSTL_EL_CORE_URI,
	   JstlCoreCatch.class);
    addMap(_fastTagMap, "resin-c", "catch", JSTL_RT_CORE_URI,
	   JstlCoreCatch.class);
    
    addMap(_fastTagMap, "resin-c", "if", JSTL_CORE_URI, JstlCoreIf.class);
    addMap(_fastTagMap, "resin-c", "if", JSTL_EL_CORE_URI, JstlCoreIf.class);
    addMap(_fastTagMap, "resin-c", "if", JSTL_RT_CORE_URI, JstlCoreIf.class);
    
    addMap(_fastTagMap, "resin-c", "choose", JSTL_CORE_URI,
	   JstlCoreChoose.class);
    addMap(_fastTagMap, "resin-c", "choose", JSTL_EL_CORE_URI,
	   JstlCoreChoose.class);
    addMap(_fastTagMap, "resin-c", "choose", JSTL_RT_CORE_URI,
	   JstlCoreChoose.class);
    
    addMap(_fastTagMap, "resin-c", "when", JSTL_CORE_URI,
	   JstlCoreWhen.class);
    addMap(_fastTagMap, "resin-c", "when", JSTL_EL_CORE_URI,
	   JstlCoreWhen.class);
    addMap(_fastTagMap, "resin-c", "when", JSTL_RT_CORE_URI,
	   JstlCoreWhen.class);
    
    addMap(_fastTagMap, "resin-c", "otherwise", JSTL_CORE_URI,
	   JstlCoreOtherwise.class);
    addMap(_fastTagMap, "resin-c", "otherwise", JSTL_EL_CORE_URI,
	   JstlCoreOtherwise.class);
    addMap(_fastTagMap, "resin-c", "otherwise", JSTL_RT_CORE_URI,
	   JstlCoreOtherwise.class);
    
    addMap(_fastTagMap, "resin-c", "forEach", JSTL_CORE_URI,
	   JstlCoreForEach.class);
    addMap(_fastTagMap, "resin-c", "forEach", JSTL_EL_CORE_URI,
	   JstlCoreForEach.class);
    addMap(_fastTagMap, "resin-c", "forEach", JSTL_RT_CORE_URI,
	   JstlCoreForEach.class);
    
    addMap(_fastTagMap, "resin-fmt", "message", JSTL_FMT_URI,
	   JstlFmtMessage.class);
    addMap(_fastTagMap, "resin-fmt", "message", JSTL_EL_FMT_URI,
	   JstlFmtMessage.class);
    addMap(_fastTagMap, "resin-fmt", "message", JSTL_RT_FMT_URI,
	   JstlFmtMessage.class);
    
    addMap(_fastTagMap, "resin-fmt", "setBundle", JSTL_FMT_URI,
	   JstlFmtSetBundle.class);
    addMap(_fastTagMap, "resin-fmt", "setBundle", JSTL_EL_FMT_URI,
	   JstlFmtSetBundle.class);
    addMap(_fastTagMap, "resin-fmt", "setBundle", JSTL_RT_FMT_URI,
	   JstlFmtSetBundle.class);
    
    addMap(_fastTagMap, "resin-fmt", "bundle", JSTL_FMT_URI,
	   JstlFmtBundle.class);
    addMap(_fastTagMap, "resin-fmt", "bundle", JSTL_EL_FMT_URI,
	   JstlFmtBundle.class);
    addMap(_fastTagMap, "resin-fmt", "bundle", JSTL_RT_FMT_URI,
	   JstlFmtBundle.class);
    
    addMap(_fastTagMap, "resin-fmt", "param", JSTL_FMT_URI,
	   JstlFmtParam.class);
    addMap(_fastTagMap, "resin-fmt", "param", JSTL_EL_FMT_URI,
	   JstlFmtParam.class);
    addMap(_fastTagMap, "resin-fmt", "param", JSTL_RT_FMT_URI,
	   JstlFmtParam.class);

    addMap(_fastTagMap, "resin-xml", "out", JSTL_XML_URI, JstlXmlOut.class);
    addMap(_fastTagMap, "resin-xml", "out", JSTL_RT_XML_URI, JstlXmlOut.class);
    addMap(_fastTagMap, "resin-xml", "out", JSTL_EL_XML_URI, JstlXmlOut.class);
    
    addMap(_fastTagMap, "resin-xml", "set", JSTL_XML_URI, JstlXmlSet.class);
    addMap(_fastTagMap, "resin-xml", "set", JSTL_RT_XML_URI, JstlXmlSet.class);
    addMap(_fastTagMap, "resin-xml", "set", JSTL_EL_XML_URI, JstlXmlSet.class);
    
    addMap(_fastTagMap, "resin-xml", "if", JSTL_XML_URI, JstlXmlIf.class);
    addMap(_fastTagMap, "resin-xml", "if", JSTL_RT_XML_URI, JstlXmlIf.class);
    addMap(_fastTagMap, "resin-xml", "if", JSTL_EL_XML_URI, JstlXmlIf.class);
    
    addMap(_fastTagMap, "resin-xml", "choose", JSTL_XML_URI,
	   JstlCoreChoose.class);
    addMap(_fastTagMap, "resin-xml", "choose", JSTL_RT_XML_URI,
	   JstlCoreChoose.class);
    addMap(_fastTagMap, "resin-xml", "choose", JSTL_EL_XML_URI,
	   JstlCoreChoose.class);
    
    addMap(_fastTagMap, "resin-xml", "when", JSTL_XML_URI,
	   JstlXmlWhen.class);
    addMap(_fastTagMap, "resin-xml", "when", JSTL_RT_XML_URI,
	   JstlXmlWhen.class);
    addMap(_fastTagMap, "resin-xml", "when", JSTL_EL_XML_URI,
	   JstlXmlWhen.class);
    
    addMap(_fastTagMap, "resin-xml", "otherwise", JSTL_XML_URI,
	   JstlCoreOtherwise.class);
    addMap(_fastTagMap, "resin-xml", "otherwise", JSTL_RT_XML_URI,
	   JstlCoreOtherwise.class);
    addMap(_fastTagMap, "resin-xml", "otherwise", JSTL_EL_XML_URI,
	   JstlCoreOtherwise.class);
  }
}
