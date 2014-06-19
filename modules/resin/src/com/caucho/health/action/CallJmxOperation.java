/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.action;

import io.baratine.core.Startup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.admin.action.CallJmxAction;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.env.health.HealthActionResult;
import com.caucho.env.health.HealthActionResult.ResultStatus;
import com.caucho.health.event.HealthEvent;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.util.L10N;

/**
 * Health action to call a JMX MBean operation.
 * <p>
 * <pre>{@code
 * <health:CallJmxOperation>
 *   <objectName>java.lang:type=Threading</objectName>
 *   <operation>resetPeakThreadCount</operation>
 *   <health:IfNotRecent time='5m'/>
 * </health:CallJmxOperation>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class CallJmxOperation extends HealthActionBase
{
  private static final L10N L = new L10N(CallJmxOperation.class);
  private static final Logger log
    = Logger.getLogger(CallJmxOperation.class.getName());
  
  private String _objectName;
  private String _operation;
  private int _operationIndex = -1;
  private List<String> _paramsList = new ArrayList<String>();
  
  private String[] _params;
  
  @PostConstruct
  public void init()
  {
    if (_objectName == null)
      throw new ConfigException(L.l("<health:{0}> requires 'object-name' attribute",
                                    getClass().getSimpleName()));
    
    if (_operation == null)
      throw new ConfigException(L.l("<health:{0}> requires 'operation' attribute",
                                    getClass().getSimpleName()));
    
    _params = new String[_paramsList.size()];
    _paramsList.toArray(_params);

    super.init();
  }

  public String getObjectName()
  {
    return _objectName;
  }

  @Configurable
  public void setObjectName(String objectName)
  {
    _objectName = objectName;
  }

  public String getOperation()
  {
    return _operation;
  }

  @Configurable
  public void setOperation(String operation)
  {
    _operation = operation;
  }
  
  public int getOperationIndex()
  {
    return _operationIndex;
  }
  
  @Configurable
  public void setOperationIndex(int operationIndex)
  {
    _operationIndex = operationIndex;
  }

  public List<String> getParams()
  {
    return _paramsList;
  }

  @Configurable
  public void addParam(String param)
  {
    _paramsList.add(param);
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    JmxCallQueryReply reply = new CallJmxAction().execute(_objectName, 
                                                          _operation, 
                                                          _operationIndex, 
                                                          _params);

    String message = L.l("method '{0}' called on '{1}' returned '{2}'",
                         reply.getOperation(),
                         reply.getBean(),
                         reply.getReturnValue());

    
    return new HealthActionResult(ResultStatus.OK, message);
  }  
}
