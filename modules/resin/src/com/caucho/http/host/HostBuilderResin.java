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

package com.caucho.http.host;

import com.caucho.config.program.ConfigProgram;
import com.caucho.http.rewrite.DispatchRule;
import com.caucho.http.rewrite.RewriteFilter;
import com.caucho.http.webapp.WebAppContainerResin;

/**
 * Builder for the webapp to encapsulate the configuration process.
 */
public class HostBuilderResin extends HostBuilder
{
  // List of default ear configurations
  /*
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();
    */
  
  public HostBuilderResin(HostContainerResin parent, 
                          HostController controller, 
                          String hostName)
  {
    super(parent, controller, hostName);
  }
  
  @Override
  protected WebAppContainerResin getWebAppContainer()
  {
    return (WebAppContainerResin) super.getWebAppContainer();
  }
  
  @Override
  protected HostContainerResin getHostContainer()
  {
    return (HostContainerResin) super.getHostContainer();
  }

  /**
   * Adds an ear default
   */
  /*
  public void addEarDefault(EarConfig init)
  {
    _earDefaultList.add(init);
  }
  */

  /**
   * Returns the list of ear defaults
   */
  /*
  public ArrayList<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }
  */

  /**
   * Sets the ear-expansion
   */
  /*
  @Configurable
  public EarDeployGenerator createEarDeploy()
    throws Exception
  {
    EarDeployGenerator gen = getWebAppContainer().createEarDeploy();
    
    for (EarConfig config : getHostContainer().getEarDefaultList()) {
      gen.addEarDefault(config);
    }
    
    for (EarConfig config : getEarDefaultList()) {
      gen.addEarDefault(config);
    }
    
    return gen;
  }
  */

  /**
   * Adds the ear-expansion
   */
  /*
  //@ConfigurableDepreca
  public void addEarDeploy(EarDeployGenerator earDeploy)
    throws Exception
  {
    getWebAppContainer().addEarDeploy(earDeploy);
  }
  */

  /*
  @Configurable
  public void addEarDefault(EarConfig config)
  {
    getWebAppContainer().addEarDefault(config);
  }
  */

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRule dispatchRule)
  {
    getWebAppContainer().add(dispatchRule);
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(RewriteFilter dispatchAction)
  {
    //_webAppContainer.add(dispatchAction);
  }

  /**
   * Adds rewrite-dispatch (backward compat).
   */
  /*
  public RewriteDispatch createRewriteDispatch()
  {
    //return _webAppContainer.createRewriteDispatch();
    return null;
  }
  */
  public void addRewriteDispatch(ConfigProgram program)
  {
  }

  
  @Override
  protected Host createHost()
  {
    return new HostResin(this);
  }
}
