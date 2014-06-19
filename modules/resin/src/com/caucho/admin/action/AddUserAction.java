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
 * @author Alex Rojkov
 */

package com.caucho.admin.action;

import java.util.logging.Logger;

import com.caucho.http.security.AuthenticatorRole;
import com.caucho.http.security.PasswordUser2;
import com.caucho.util.L10N;

public class AddUserAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(AddUserAction.class.getName());

  private static final L10N L = new L10N(AddUserAction.class);

  private AuthenticatorRole _adminAuth;
  private String _user;
  private char[] _password;
  private String[] _roles;

  public AddUserAction(AuthenticatorRole adminAuth,
                       String user,
                       char[] password,
                       String[] roles)
  {
    _adminAuth = adminAuth;
    _user = user;
    _password = password;
    _roles = roles;
  }

  public PasswordUser2 execute()
  {
    /*
    _adminAuth.addUser(_user, _password, _roles);

    PasswordUser user = _adminAuth.getUserMap().get(_user);

    return user;
    */
    return null;
  }
}
