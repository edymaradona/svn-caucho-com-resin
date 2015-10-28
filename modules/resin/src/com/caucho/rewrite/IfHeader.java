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
 * @author Sam
 */

package com.caucho.rewrite;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.http.rewrite.RequestPredicateVary;
import com.caucho.v5.util.L10N;


/**
 * Passes if the named header exists and has a value
 * that matches a regular expression.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:Forbidden regexp="^/local/">
 *     &lt;resin:IfHeader name="foo" regexp="bar"/>
 *   &lt;/resin:Forbidden>
 *
 * &lt;/web-app>
 * </pre>
 *
 * <p>RequestPredicates may be used for both security and rewrite conditions.
 */
@Configurable
public class IfHeader implements RequestPredicateVary
{
  private static final L10N L = new L10N(IfHeader.class);
  
  private String _name;
  private Pattern _regexp;
  
  private boolean _isSendVary = true;

  public IfHeader()
  {
  }

  /**
   * Sets the HTTP header name to test.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the HTTP header regexp to compare against.
   */
  @Configurable
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }
  
  public void setValue(Pattern regexp)
  {
    setRegexp(regexp);
  }

  @Configurable
  public void setSendVary(boolean isVary)
  {
    _isSendVary = isVary;
  }
  
  public boolean isSendVary()
  {
    return _isSendVary;
  }
  
  @PostConstruct
  public void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("'name' is a required attribute for {0}",
                                    getClass().getSimpleName()));
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    String value = request.getHeader(_name);

    if (value != null)
      return _regexp == null || _regexp.matcher(value).find();
    else
      return false;
  }
  
  @Override
  public void addVaryHeader(HttpServletResponse response)
  {
    if (_isSendVary) {
      response.addHeader("Vary", _name);
    }
  }
}
