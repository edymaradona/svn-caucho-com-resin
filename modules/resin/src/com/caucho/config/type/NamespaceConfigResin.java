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

package com.caucho.config.type;

import com.caucho.config.core.ImportConfigXml;
import com.caucho.config.core.MessageConfig;
import com.caucho.config.core.ResinChoose;
import com.caucho.config.core.ResinIf;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.core.ResinSet;
import com.caucho.config.core.ResinSystemConfig;
import com.caucho.loader.ClassLoaderConfig;
import com.caucho.log.impl.LogConfig;
import com.caucho.log.impl.LogHandlerConfig;
import com.caucho.log.impl.LoggerConfig;
import com.caucho.sql.DBPool;
import com.caucho.sql.DatabaseConfig;

// configuration types
class NamespaceConfigResin extends NamespaceConfig
{
  static final NamespaceConfig NS_RESIN;
  static final NamespaceConfig NS_RESIN_CORE;
  static final NamespaceConfig URN_RESIN;
  
  NamespaceConfigResin(String ns, boolean isDefault)
  {
    super(ns, isDefault);
  }

  @Override
  protected void initCore()
  {
    super.initCore();
    
    addFlow("choose", ResinChoose.class);
    addFlow("if", ResinIf.class);
    addFlow("properties", ResinProperties.class);
    
    addBean("import", ImportConfigXml.class);
    addBean("log", MessageConfig.class);
    addBean("message", MessageConfig.class);
    addBean("set", ResinSet.class);
    addBean("system", ResinSystemConfig.class);
    
    addBean("value", ValueType.class);
  }
  
  protected void initExtensions()
  {
    addBean("authenticator", "com.caucho.security.Authenticator");
    
    addBean("bean", "com.caucho.config.cfg.BeanConfig");
    
    addBean("case-insensitive", "com.caucho.vfs.CaseInsensitive");
    addBean("character-encoding", "com.caucho.i18n.CharacterEncoding");
    addBean("choose", ResinChoose.class);
    addBean("class-loader", ClassLoaderConfig.class);
    addBean("classpath", "com.caucho.loader.ClasspathConfig");
    addBean("class-update-interval", "com.caucho.loader.DependencyCheckInterval");
    addBean("component", "com.caucho.config.cfg.WbComponentConfig");
    addBean("connection-factory", "com.caucho.jca.cfg.ConnectionFactoryConfig");
    addBean("connector", "com.caucho.jca.ra.ConnectorConfig");
    
    addBean("database", DBPool.class);
    addBean("database-default", DatabaseConfig.class);
    addBean("data-source", "com.caucho.config.types.DataSourceRef");
    addBean("dependency", "com.caucho.make.DependencyConfig");
    addBean("dependency-check-interval", "com.caucho.loader.DependencyCheckInterval");
    
    addBean("ejb-local-ref", "com.caucho.config.types.EjbLocalRef");
    addBean("ejb-message-bean", "com.caucho.ejb.cfg.MessageBeanConfig");
    addBean("ejb-ref", "com.caucho.config.types.EjbRef");
    addBean("ejb-server", "com.caucho.ejb.EJBServer");
    addBean("ejb-stateless-bean", "com.caucho.ejb.cfg.StatelessBeanConfig");
    addBean("ejb-stateful-bean", "com.caucho.ejb.cfg.StatefulBeanConfig");
    addBean("env-entry", "com.caucho.config.types.EnvEntry");
    
    addBean("if", "com.caucho.config.core.ResinIf");
    addBean("import", ImportConfigXml.class);
    addBean("include", "com.caucho.config.core.ResinInclude");
    addBean("interceptor", "com.caucho.config.cfg.InterceptorConfig");
    
    addBean("java", "com.caucho.java.JavacConfig");
    addBean("javac", "com.caucho.java.JavacConfig");
    addBean("jndi-link", "com.caucho.naming.LinkProxy");
    //addBean("jms-connection-factory", "com.caucho.jms.cfg.JmsConnectionFactoryConfig");
    //addBean("jms-queue", "com.caucho.jms.cfg.JmsQueueConfig");
    //addBean("jms-topic", "com.caucho.jms.cfg.JmsTopicConfig");
    addBean("jpa-persistence", "com.caucho.amber.cfg.PersistenceManager");
    addBean("jpa-persistence-unit", "com.caucho.env.jpa.ConfigJpaPersistenceUnit");
    addBean("jpa-persistence-unit-default", "com.caucho.env.jpa.ConfigJpaPersistenceUnitDefault");
    
    addBean("list", "com.caucho.config.type.ListType");
    addBean("log", LogConfig.class);
    addBean("log-handler", LogHandlerConfig.class);
    addBean("logger", LoggerConfig.class);
    
    addBean("mail", "com.caucho.jca.cfg.JavaMailConfig");
    addBean("map", "com.caucho.config.type.MapType");
    addBean("mbean", "com.caucho.jmx.MBeanConfig");
    // addBean("message", ResinLog.class);
    addBean("message-destination-ref", "com.caucho.config.types.MessageDestinationRef");
    
    addBean("null", "com.caucho.config.type.NullType");
    
    // addBean("persistent-store", PersistentStoreConfig.class);
    addBean("persistence-unit-ref", "com.caucho.config.types.PersistenceUnitRef");
    
    //addBean("rar-deploy", "com.caucho.jca.ra.ResourceDeploy");
    addBean("reference", "com.caucho.config.types.ReferenceConfig");
    addBean("remote-client", "com.caucho.remote.client.RemoteClient");
    //addBean("resource", "com.caucho.jca.cfg.Resource");
    //addBean("resource-adapter", "com.caucho.jca.cfg.ResourceAdapterBeanConfig");
    //addBean("resource-default", "com.caucho.jca.ra.ResourceDefault");
    //addBean("resource-deploy", "com.caucho.jca.ra.ResourceDeploy");
    //addBean("resource-env-ref", "com.caucho.config.types.ResourceEnvRef");
    //addBean("resource-manager", "com.caucho.jca.ra.ResourceManagerConfig");
    //addBean("resource-ref", "com.caucho.config.types.ResourceRef");
    addBean("role-map", "com.caucho.security.RoleMap");
    
    addBean("scheduled-task", "com.caucho.resources.ScheduledTaskConfig");
    addBean("security-role-ref", "com.caucho.config.types.SecurityRoleRef");
    addBean("servlet-classloader-hack", "com.caucho.loader.ServletClassloaderHack");
    addBean("set", "com.caucho.config.core.ResinSet");
    addBean("stderr-log", "com.caucho.log.impl.StderrLog");
    addBean("stdout-log", "com.caucho.log.impl.StdoutLog");
    addBean("system-property", "com.caucho.loader.SystemProperty");
    
    addBean("temp-dir", "com.caucho.java.TempDir");
    addBean("temporary-directory", "com.caucho.java.TempDir");
    
    addBean("value", "com.caucho.config.type.ValueType");
    
    addBean("web-service-client", "com.caucho.remote.client.RemoteClient");
    addBean("work-dir", "com.caucho.java.WorkDir");
    addBean("work-directory", "com.caucho.java.WorkDir");
  }
  
  static {
    NS_RESIN = new NamespaceConfigResin("http://caucho.com/ns/resin", true);
    NS_RESIN.initCore();
    NS_RESIN.initExtensions();
    
    NS_RESIN_CORE = new NamespaceConfigResin("http://caucho.com/ns/resin/core", false);
    NS_RESIN_CORE.initCore();
    
    URN_RESIN = new NamespaceConfigResin("urn:java:com.caucho.resin", false);
    URN_RESIN.initCore();
    URN_RESIN.initExtensions();
  }
}
