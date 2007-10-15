/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.UrlModule;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Information and actions for about files
 */
public class FileModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(FileModule.class);
  private static final Logger log
    = Logger.getLogger(FileModule.class.getName());

  public static final String DIRECTORY_SEPARATOR = "" + Path.getFileSeparatorChar();
  public static final String PATH_SEPARATOR = "" + Path.getPathSeparatorChar();

  public static final int UPLOAD_ERR_OK = 0;
  public static final int UPLOAD_ERR_INI_SIZE = 1;
  public static final int UPLOAD_ERR_FORM_SIZE = 2;
  public static final int UPLOAD_ERR_PARTIAL = 3;
  public static final int UPLOAD_ERR_NO_FILE = 4;
  public static final int UPLOAD_ERR_NO_TMP_DIR = 6;
  public static final int UPLOAD_ERR_CANT_WRITE = 7;
  public static final int UPLOAD_ERR_EXTENSION = 8;

  public static final int FILE_USE_INCLUDE_PATH = 1;
  public static final int FILE_APPEND = 8;

  public static final int LOCK_SH = 1;
  public static final int LOCK_EX = 2;
  public static final int LOCK_UN = 3;
  public static final int LOCK_NB = 4;

  public static final int FNM_PATHNAME = 1;
  public static final int FNM_NOESCAPE = 2;
  public static final int FNM_PERIOD = 4;
  public static final int FNM_CASEFOLD = 16;

  public static final int GLOB_MARK = 1;
  public static final int GLOB_NOSORT = 2;
  public static final int GLOB_NOCHECK = 4;
  public static final int GLOB_NOESCAPE = 8;
  public static final int GLOB_BRACE = 16;
  public static final int GLOB_ONLYDIR = 32;
  public static final int GLOB_ERR = 64;

  public static final int PATHINFO_DIRNAME = 1;
  public static final int PATHINFO_BASENAME = 2;
  public static final int PATHINFO_EXTENSION = 4;
  public static final int PATHINFO_FILENAME = 8;

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  private static final HashMap<String,Value> _constMap
    = new HashMap<String,Value>();

  /**
   * Returns the default quercus.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }
  
  /**
   * Returns the constants defined by this module.
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }
  
  /**
   * Returns the base name of a string.
   */
  @ReturnNullAsFalse
  public static String basename(String path, @Optional String suffix)
  {
    if (path == null)
      return null;

    int len = path.length();

    if (len == 0)
      return "";
    else if (path.endsWith("/"))
      len -= 1;
    else if (path.endsWith("\\"))
      len -= 1;

    int p = path.lastIndexOf('/', len - 1);

    if (p < 0)
      p = path.lastIndexOf('\\', len - 1);

    String file;

    if (p < 0)
      file = path.substring(0, len);
    else
      file = path.substring(p + 1, len);

    if (suffix != null && file.endsWith(suffix))
      file = file.substring(0, file.length() - suffix.length());

    return file;
  }

  /**
   * Changes the working directory
   *
   * @param path the path to change to
   */
  public static boolean chdir(Env env, Path path)
  {
    if (path.isDirectory()) {
      env.setPwd(path);
      return true;
    }
    else {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return false;
    }
  }

  /**
   * Changes the working directory, forming a virtual root
   *
   * @param path the path to change to
   */
  public static boolean chroot(Env env, Path path)
  {
    if (path.isDirectory()) {
      
      env.setPwd(path.createRoot());

      return true;
    }
    else {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return false;
    }
  }

  /**
   * Changes the group of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param group the group id to change to
   */
  public static boolean chgrp(Env env, Path file, Value group)
  {
    if (!file.canRead()) {
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    // quercus/160i

    try {
      // XXX: safe_mode

      if (group instanceof LongValue)
        file.changeGroup(group.toInt());
      else
        file.changeGroup(group.toString());

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Changes the permissions of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param mode the mode id to change to
   */
  public static boolean chmod(Env env, Path file, int mode)
  {
    if (! file.canRead()) {
      // XXX: gallery?
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    // quercus/160j
    file.chmod(mode);

    return true;
  }

  /**
   * Changes the ownership of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param user the user id to change to
   */
  public static boolean chown(Env env, Path file, Value user)
  {
    if (!file.canRead()) {
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    try {
      // XXX: safe_mode

      if (user instanceof LongValue)
        file.changeOwner(user.toInt());
      else
        file.changeOwner(user.toString());

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Clears the stat cache for the file
   *
   * @param env the PHP executing environment
   */
  public static Value clearstatcache(Env env)
  {
    // quercus/160l

    // XXX: stubbed

    return NullValue.NULL;
  }

  /**
   * Copies a file to the destination.
   *
   * @param src the source path
   * @param dst the destination path
   */
  public static boolean copy(Env env, Path src, Path dst)
  {
    // quercus/1603

    try {
      if (! src.canRead() || ! src.isFile()) {
        env.warning(L.l("{0} cannot be read", src.getFullPath()));

        return false;
      }

      WriteStream os = dst.openWrite();

      try {
        src.writeToStream(os);
      } finally {
        os.close();
      }

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Opens a directory
   *
   * @param path the path to change to
   */
  @ReturnNullAsFalse
  public static Directory dir(Env env, Path path)
  {
    try {
      if (! path.isDirectory()) {
        env.warning(L.l("{0} is not a directory", path.getFullPath()));

        return null;
      }

      return new Directory(env, path);

/*
      DirectoryValue dir = new DirectoryValue(path);

      env.addClose(dir);

      return dir;
*/
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the directory name of a string.
   */
  public StringValue dirname(Env env, StringValue path)
  {
    if (path == null)
      return env.createString(".");
    
    int len = path.length();

    if (len == 0)
      return env.createString(".");
    else if (path.length() == 1 && path.charAt(0) == '/')
      return path;
    
    int p = path.lastIndexOf('/', len - 2);
    
    // php/1601 (for Windows)
    p = Math.max(p, path.lastIndexOf('\\', len - 2));
    
    if (p == 0)
      return env.createString("/");
    else if (p > 0)
      return path.substring(0, p);
    
    p = path.lastIndexOf('\\', len - 2);
    
    if (p == 0)
      return env.createString("\\");
    else if (p > 0)
      return path.substring(0, p);
    
    return env.createString(".");
  }

  /**
   * Returns the free space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value disk_free_space(Env env, Path directory)
  {
    // quercus/160m

    if (!directory.canRead()) {
      env.warning(L.l("{0} cannot be read", directory.getFullPath()));

      return BooleanValue.FALSE;
    }

    return new DoubleValue(directory.getDiskSpaceFree());
  }

  /**
   * Returns the total space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value disk_total_space(Env env, Path directory)
  {
    // quercus/160n

    if (!directory.canRead()) {
      env.warning(L.l("{0} cannot be read", directory.getFullPath()));

      return BooleanValue.FALSE;
    }

    return new DoubleValue(directory.getDiskSpaceTotal());
  }

  /**
   * Returns the total space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value diskfreespace(Env env, Path directory)
  {
    return disk_free_space(env, directory);
  }

  /**
   * Closes a file.
   */
  public static boolean fclose(Env env, @NotNull BinaryStream s)
  {
    if (s == null)
      return false;

    s.close();

    return true;
  }

  /**
   * Checks for the end of file.
   */
  public static boolean feof(Env env, @NotNull BinaryInput is)
  {
    if (is == null)
      return false;

    return is.isEOF();
  }

  /**
   * Flushes a file.
   */
  public static boolean fflush(Env env, @NotNull BinaryOutput os)
  {
    if (os == null)
      return false;

    try {
      os.flush();

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Returns the next character as a byte
   */
  public static Value fgetc(Env env, @NotNull BinaryInput is)
  {
    try {
      if (is == null)
	return BooleanValue.FALSE;

      // XXX: char for i18n and mode = "t"

      // php/1612
      int ch = is.read();

      if (ch >= 0) {
	StringValue v = env.createBinaryBuilder(1);
	
	v.append((char) ch);
	
	return v;
      }
      else
	return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses a comma-separated-value line from a file.
   *
   * @param file the file to read
   * @param length the maximum line length
   * @param delimiter optional comma replacement
   * @param enclosure optional quote replacement
   */
  public Value fgetcsv(Env env,
                       @NotNull BinaryInput is,
                       @Optional int length,
                       @Optional String delimiter,
                       @Optional String enclosure)
  {
    // php/1619

    try {
      if (is == null)
	return BooleanValue.FALSE;

      // XXX: length is never used
      if (length <= 0)
	length = Integer.MAX_VALUE;

      int comma = ',';

      if (delimiter != null && delimiter.length() > 0)
	comma = delimiter.charAt(0);

      int quote = '"';

      if (enclosure != null && enclosure.length() > 0)
	quote = enclosure.charAt(0);

      ArrayValue array = new ArrayValueImpl();

      int ch;

      while (true) {
	// scan whitespace
	while (true) {
	  ch = is.read();

	  if (ch < 0 || ch == '\n')
	    return array;
	  else if (ch == '\r') {
	    is.readOptionalLinefeed();
	    return array;
	  }
	  else if (ch == ' ' || ch == '\t')
	    continue;
	  else
	    break;
	}

	StringValue sb = env.createBinaryBuilder();

	if (ch == quote) {
	  for (ch = is.read(); ch >= 0; ch = is.read()) {
	    if (ch == quote) {
	      ch = is.read();

	      if (ch == quote)
		sb.append((char) ch);
	      else
		break;
	    }
	    else
	      sb.append((char) ch);
	  }

	  array.append(sb);

	  for (; ch >= 0 && ch == ' ' || ch == '\t'; ch = is.read()) {
	  }
	}
	else {
	  for (;
	       ch >= 0 && ch != comma && ch != '\r' && ch != '\n';
	       ch = is.read()) {
	    sb.append((char) ch);
	  }

	  array.append(sb);
	}

	if (ch < 0)
	  return array;
	else if (ch == '\n')
	  return array;
	else if (ch == '\r') {
	  is.readOptionalLinefeed();
	  return array;
	}
	else if (ch == comma) {
	}
	else {
	  env.warning("expected comma");
	}
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the next line
   */
  public static Value fgets(Env env,
			    @NotNull BinaryInput is,
			    @Optional("0x7fffffff") int length)
  {
    // php/1615

    try {
      if (is == null)
        return BooleanValue.FALSE;

      StringValue value = is.readLine(length);

      if (value != null)
        return value;
      else
        return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the next line stripping tags
   */
  public static Value fgetss(Env env,
			     BinaryInput is,
			     @Optional("0x7fffffff") int length,
			     @Optional String allowedTags)
  {
    // php/161a

    try {
      if (is == null) {
        env.warning(L.l("{0} is null", "handle"));
        return BooleanValue.FALSE;
      }

      StringValue value = is.readLine(length);

      if (value != null)
        return StringModule.strip_tags(value, allowedTags);
      else
        return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses the file, returning it in an array.  Binary-safe.
   *
   * @param filename the file's name
   * @param useIncludePath if 1, use the include path
   * @param context the resource context
   */
  public static Value file(Env env,
                           StringValue filename,
                           @Optional boolean useIncludePath,
                           @Optional Value context)
  {
    if (filename == null)
      return BooleanValue.FALSE;    

    try {
      BinaryStream stream = fopen(env, filename, "r", useIncludePath, context);

      if (stream == null)
        return BooleanValue.FALSE;

      BinaryInput is = (BinaryInput) stream;

      ArrayValue result = new ArrayValueImpl();

      try {
        while (true) {
          StringValue bb = env.createBinaryBuilder();

          for (int ch = is.read(); ch >= 0; ch = is.read()) {
            if (ch == '\n') {
              bb.appendByte(ch);
              break;
            }
            else if (ch == '\r') {
              bb.appendByte('\r');

              int ch2 = is.read();

              if (ch2 == '\n')
                bb.appendByte('\n');
              else
                is.unread();

              break;
            }
            else
              bb.appendByte(ch);
          }

          if (bb.length() > 0)
            result.append(bb);
          else
            return result;
        }
      } finally {
        is.close();
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the file access time
   *
   * @param path the path to check
   */
  public static Value fileatime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getLastAccessTime();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file create time
   *
   * @param path the path to check
   */
  public static Value filectime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getCreateTime();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file's group
   *
   * @param path the path to check
   */
  public static Value filegroup(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getGroup());
  }

  /**
   * Returns the file's inocde
   *
   * @param path the path to check
   */
  public static Value fileinode(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getInode());
  }

  /**
   * Returns the file modified time
   *
   * @param path the path to check
   */
  public static Value filemtime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getLastModified();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file's owner
   *
   * @param path the path to check
   */
  public static Value fileowner(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getOwner());
  }

  /**
   * Returns the file's permissions
   *
   * @param path the path to check
   */
  public static Value fileperms(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getMode());
  }

  /**
   * Returns the file's size
   *
   * @param path the path to check
   */
  public static Value filesize(Env env, Path path)
  {
    if (! path.exists() || ! path.isFile()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long length = path.getLength();

    if (length < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(length);
  }

  /**
   * Returns the file's type
   *
   * @param path the path to check
   */
  public static Value filetype(Env env, @NotNull Path path)
  {
    if (path == null)
      return BooleanValue.FALSE;
    else if (! path.exists()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }
    else if (path.isDirectory())
      return env.createString("dir");
    else if (path.isFile())
      return env.createString("file");
    else if (path.isFIFO())
      return env.createString("fifo");
    else if (path.isLink())
      return env.createString("link");
    else if (path.isBlockDevice())
      return env.createString("block");
    else if (path.isCharacterDevice())
      return env.createString("char");
    else
      return env.createString("unknown");
  }

  /**
   * Returns true if file exists
   *
   * @param path the path to check
   */
  public static boolean file_exists(@NotNull Path path)
  {
    if (path != null)
      return path.exists();
    else
      return false;
  }

  /**
   * Parses the file, returning it as a string array.
   *
   * @param filename the file's name
   * @param useIncludePath if true, use the include path
   * @param context the resource context
   */
  @ReturnNullAsFalse
  public static StringValue
    file_get_contents(Env env,
		      StringValue filename,
		      @Optional boolean useIncludePath,
		      @Optional Value context,
		      @Optional long offset,
		      @Optional("4294967296") long maxLen)
  {
    if (filename == null) {
      env.warning(L.l("file name must not be null"));
      return null;
    }

    BinaryStream s = fopen(env, filename, "r", useIncludePath, context);

    if (! (s instanceof BinaryInput))
      return null;

    try {
      BinaryInput is = (BinaryInput) s;

      StringValue bb = env.createBinaryBuilder();

      is.appendTo(bb);

      return bb;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Writes data to a file.
   */
  public Value file_put_contents(Env env,
                                 StringValue filename,
                                 Value data,
                                 @Optional int flags,
                                 @Optional Value context)
  {
    if (filename == null) {
      env.warning(L.l("file name must not be null"));
      return BooleanValue.FALSE;
    }

    // php/1634

    BinaryStream s = null;

    try {
      boolean useIncludePath = (flags & FILE_USE_INCLUDE_PATH) != 0;
      String mode = (flags & FILE_APPEND) != 0 ? "a" : "w";

      s = fopen(env, filename, mode, useIncludePath, context);

      if (! (s instanceof BinaryOutput))
        return BooleanValue.FALSE;

      if ((flags & LOCK_EX) != 0) {
        if (s instanceof LockableStream) {
          if (! flock(env, (LockableStream) s, LOCK_EX, null))
            return BooleanValue.FALSE;
        } else {
          return BooleanValue.FALSE;
        }
      }

      BinaryOutput os = (BinaryOutput) s;

      try {
        long dataWritten = 0;

        if (data instanceof ArrayValue) {
          for (Value item : ((ArrayValue) data).values()) {
            InputStream is = item.toInputStream();

            dataWritten += os.write(is, Integer.MAX_VALUE);

            is.close();
          }
        }
        else {
          InputStream is = data.toInputStream();

          dataWritten += os.write(is, Integer.MAX_VALUE);

          is.close();
        }

        return new LongValue(dataWritten);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      if (s != null && (s instanceof LockableStream) && 
          ((flags & LOCK_EX) != 0))
        flock(env, (LockableStream) s, LOCK_UN, null);
    } 
  }

  /**
   * Advisory locking
   *
   * @param fileV the file handle
   * @param operation the locking operation
   * @param wouldBlock the resource context
   */
  public static boolean flock(Env env,
                              LockableStream fileV,
                              int operation,
                              @Optional Value wouldBlock)
  {
    // XXX: also wouldblock is a ref

    if (fileV == null) {
      env.warning(L.l("flock: file is null"));
      return false;
    }

    boolean shared = false;
    boolean block = false;

    if (operation > LOCK_NB) {
      block = true;
      operation -= LOCK_NB;
    }

    switch (operation) {
      case LOCK_SH:
        shared = true;
        break;
      case LOCK_EX:
        shared = false;
        break;
      case LOCK_UN:
        return fileV.unlock();
      default:
        // This is PHP's behavior... 
        return true;
    }

    return fileV.lock(shared, block);
  }


  /**
   * Converts a glob pattern to a regular expression.
   */
  private static String globToRegex(String pattern, int flags, boolean brace) 
  {
    StringBuilder globRegex = new StringBuilder();

    int bracketCount = 0;
    boolean inSquareBrackets = false;
    boolean inCurlyBrackets = false;
    char lastCh = ' ';

    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);

      switch (ch) {
        case '*':
          if (inSquareBrackets || inCurlyBrackets) {
            globRegex.append("*");

            if (inSquareBrackets)
              bracketCount++;
          } else {
            if ((flags & FNM_PATHNAME) != 0)
              globRegex.append("[^/]*");
            else
              globRegex.append(".*");
          }

          break;

        case '?':
          if (inSquareBrackets || inCurlyBrackets) {
            globRegex.append("*");

            if (inSquareBrackets)
              bracketCount++;
          } else {
            if ((flags & FNM_PATHNAME) != 0)
              globRegex.append("[^/]");
            else
              globRegex.append(".");
          }

          break;

        case '^':
          if (lastCh == '[')
            globRegex.append(ch);
          else {
            globRegex.append("\\" + ch);

            if (inSquareBrackets)
              bracketCount++;
          }
          
          break;

        case '!':
          if (lastCh == '[')
            globRegex.append('^');
          else {
            globRegex.append(ch);

            if (inSquareBrackets)
              bracketCount++;
          }
          
          break;

        case '/':
          if (! ((inSquareBrackets || inCurlyBrackets) && 
                 ((flags & FNM_PATHNAME) != 0))) {
            globRegex.append(ch);

            if (inSquareBrackets)
              bracketCount++;
          }

          // don't include '/' in the brackets when FNM_PATHNAME is specified
          break;

        case '+':
        case '(':
        case ')':
        case '$':
        case '.':
        case '|':
          // escape regex special characters that are not glob 
          // special characters
          globRegex.append('\\');
          globRegex.append(ch);

          if (inSquareBrackets)
            bracketCount++;

          break;

        case '\\':
          if ((flags & FNM_NOESCAPE) != 0)
            globRegex.append('\\');

          globRegex.append(ch);

          if (inSquareBrackets)
            bracketCount++;

          break;

        case '[':
          inSquareBrackets = true;

          globRegex.append(ch);

          break;

        case ']':
          inSquareBrackets = false;

          if (bracketCount == 0)
            return null;

          globRegex.append(ch);

          break;

        case '{':
          if (inSquareBrackets || inCurlyBrackets) {
            globRegex.append(ch);

            if (inSquareBrackets)
              bracketCount++;
          } else if (brace) {
            globRegex.append('(');

            inCurlyBrackets = true;
          } else {
            globRegex.append('\\');
            globRegex.append(ch);
          }

          break;

        case '}':
          if (inSquareBrackets) {
            globRegex.append(ch);

            bracketCount++;
          } else if (brace && inCurlyBrackets) {
            globRegex.append(')');

            inCurlyBrackets = false;
          } else {
            globRegex.append('\\');
            globRegex.append(ch);
          }

          break;

        case ',':
          if (brace && inCurlyBrackets)
            globRegex.append('|');
          else
            globRegex.append(ch);

          break;

        default:
          globRegex.append(ch);

          if (inSquareBrackets)
            bracketCount++;

          break;
      }

      lastCh = ch;
    }

    return globRegex.toString();
  }
    
  /**
   * Returns true if the given string matches the given glob pattern.
   */
  public static boolean fnmatch(Env env, String pattern, String string, 
                                @Optional int flags)
  {
    if (pattern == null || string == null)
      return false;

    if ((flags & FNM_CASEFOLD) != 0) {
      string = string.toLowerCase();
      pattern = pattern.toLowerCase();
    }

    // match "leading" periods exactly (i.e. no wildcards)
    if ((flags & FNM_PERIOD) != 0) { 
      if (string.length() > 0 && string.charAt(0) == '.'){
        if (! (pattern.length() > 0 && pattern.charAt(0) == '.'))
          return false;

        string = string.substring(1);
        pattern = pattern.substring(1);
      } else if ((flags & FNM_PATHNAME) != 0) {
        // special case: if the string starts with '/.', then the pattern
        // must also start with exactly that.
        if ((string.length() >= 2) &&
           (string.charAt(0) == '/') && (string.charAt(1) == '.')) {
          if (! ((pattern.length() >= 2) &&
                 (pattern.charAt(0) == '/') && (pattern.charAt(1) == '.')))
            return false;

          string = string.substring(2);
          pattern = pattern.substring(2);
        }
      } 
    }

    String globRegex = globToRegex(pattern, flags, false);

    if (globRegex == null)
      return false;

    return string.matches(globRegex.toString());
  }

  private static ProtocolWrapper getProtocolWrapper(Env env,
						    StringValue pathName)
  {
    ArrayValue url = (ArrayValue) UrlModule.parse_url(env, pathName);

    Value scheme = url.get(env.createString("scheme"));

    if (scheme == UnsetValue.UNSET)
      return null;

    return StreamModule.getWrapper(scheme.toString());
  }

  /**
   * Opens a file.
   *
   * @param filename the path to the file to open
   * @param mode the mode the file should be opened as.
   * @param useIncludePath if true, search the current include path
   */
  @ReturnNullAsFalse
  public static BinaryStream fopen(Env env,
				   StringValue filename,
				   String mode,
				   @Optional boolean useIncludePath,
				   @Optional Value context)
  {
    if (filename == null) {
      env.warning(L.l("file name must not be null"));
      return null;
    }
    
    if (mode == null) {
      env.warning(L.l("fopen mode must not be null"));
      return null;
    }

    // XXX: context
    try {
      ProtocolWrapper wrapper = getProtocolWrapper(env, filename);

      if (wrapper != null) {
        long options = 0;

        if (useIncludePath)
          options = StreamModule.STREAM_USE_PATH;
           
        return wrapper.fopen(env, filename,
                                  env.createString(mode),
                                  LongValue.create(options));
      }

      Path path;

      path = env.getPwd().lookup(filename.toString());

      if (mode.startsWith("r")) {
        if (useIncludePath)
          path = env.lookupInclude(filename.toString());

        if (path == null) {
          env.warning(L.l("{0} cannot be read", filename));

          return null;
        } else if (! path.exists()) {
          env.warning(L.l("{0} cannot be read", path.getFullPath()));

          return null;
        }

        try {
          BinaryInput input;

          if (mode.startsWith("r+"))
            input = new FileInputOutput(env, path);
          else
            input = new FileInput(env, path);

          return input;
        } catch (IOException e) {
          
          log.log(Level.FINE, e.toString(), e);
          env.warning(L.l("{0} cannot be read", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("w")) {
        try {
          if (mode.startsWith("w+")) 
            return new FileInputOutput(env, path, false, true);
          else
            return new FileOutput(env, path);
        } catch (IOException e) {
          
          log.log(Level.FINE, e.toString(), e);
          env.warning(L.l("{0} cannot be written", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("a")) {
        try {
          if (mode.startsWith("a+"))
            return new FileInputOutput(env, path, true, false);
          else
            return new FileOutput(env, path, true);
        } catch (IOException e) {
          
          log.log(Level.FINE, e.toString(), e);
          env.warning(L.l("{0} cannot be written", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("x") && ! path.exists()) {
        if (mode.startsWith("x+"))
          return new FileInputOutput(env, path);
        else 
          return new FileOutput(env, path);
      }

      env.warning(L.l("bad mode `{0}'", mode));

      return null;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      env.warning(L.l("{0} can't be opened.\n{1}",
            filename, e.toString()));

      return null;
    }
  }

  /**
   * Output the filepointer data to the output stream.
   */
  public Value fpassthru(Env env, @NotNull BinaryInput is)
  {
    // php/1635

    try {
      if (is == null)
	return BooleanValue.FALSE;

      WriteStream out = env.getOut();

      long writeLength = out.writeStream(is.getInputStream());

      return LongValue.create(writeLength);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses a comma-separated-value line from a file.
   *
   * @param file the file to read
   * @param delimiter optional comma replacement
   * @param enclosure optional quote replacement
   */
  public Value fputcsv(Env env,
                       @NotNull BinaryOutput os,
                       @NotNull ArrayValue value,
                       @Optional StringValue delimiter,
                       @Optional StringValue enclosure)
  {
    // php/1636

    try {
      if (os == null)
	return BooleanValue.FALSE;

      if (value == null)
	return BooleanValue.FALSE;

      char comma = ',';
      char quote = '\"';

      if (delimiter != null && delimiter.length() > 0)
	comma = delimiter.charAt(0);

      if (enclosure != null && enclosure.length() > 0)
	quote = enclosure.charAt(0);

      int writeLength = 0;
      boolean isFirst = true;

      for (Value data : value.values()) {
	if (! isFirst) {
	  os.print(comma);
	  writeLength++;
	}
	isFirst = false;

	StringValue s = data.toStringValue();
	int strlen = s.length();

	writeLength++;
	os.print(quote);

	for (int i = 0; i < strlen; i++) {
	  char ch = s.charAt(i);

	  if (ch != quote) {
	    os.print(ch);
	    writeLength++;
	  }
	  else {
	    os.print(quote);
	    os.print(quote);
	    writeLength += 2;
	  }
	}

	os.print(quote);
	writeLength++;
      }

      os.print("\n");
      writeLength++;

      return LongValue.create(writeLength);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Writes a string to the file.
   */
  public static Value fputs(Env env,
			    BinaryOutput os,
			    InputStream value,
			    @Optional("0x7fffffff") int length)
  {
    return fwrite(env, os, value, length);
  }

  /**
   * Reads content from a file.
   *
   * @param is the file
   */
  public static Value fread(Env env,
			    @NotNull BinaryInput is,
			    int length)
  {
    if (is == null)
      return BooleanValue.FALSE;

    if (length < 0)
      length = Integer.MAX_VALUE;

    StringValue sb = env.createBinaryBuilder();

    sb.appendRead(is, length);

    return sb;
  }

  /**
   * Reads and parses a line.
   */
  public static Value fscanf(Env env,
                             @NotNull BinaryInput is,
                             StringValue format,
                             @Optional Value []args)
  {
    try {
      if (is == null)
        return BooleanValue.FALSE;

      StringValue value = is.readLine(Integer.MAX_VALUE);

      if (value == null)
        return BooleanValue.FALSE;

      return StringModule.sscanf(env, value, format, args);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the current position.
   *
   * @param is the stream to test
   * @return 0 on success, -1 on error.
   */
  public static Value fseek(Env env,
			    @NotNull BinaryInput is,
			    long offset, @Optional int whence)
  {
    if (is == null)
      return LongValue.MINUS_ONE;

    long position = is.seek(offset, whence);

    if (position < 0)
      return LongValue.MINUS_ONE;
    else
      return LongValue.ZERO;
  }

  /**
   * Returns the status of the given file pointer.
   */
  public static Value fstat(Env env, @NotNull BinaryStream stream)
  {
    return stream.stat();
  }

  /**
   * Returns the current position.
   *
   * @param file the stream to test
   * @return position in file or FALSE on error.
   */
  public static Value ftell(Env env,
			    @NotNull BinaryInput is)
  {
    if (is == null)
      return BooleanValue.FALSE;

    long pos = is.getPosition();

    if (pos < 0)
      return BooleanValue.FALSE;

    return LongValue.create(pos);
  }

  /**
   * Truncates a file.
   */
  public static boolean ftruncate(Env env, 
                                  @NotNull BinaryOutput handle, 
                                  long size)
  {
    if (handle instanceof FileOutput) {
      Path path = ((FileOutput) handle).getPath();

      try {
        return path.truncate(size);
      } catch (IOException e) {
        return false;
      }
    }

    return false;
  }

  /**
   * Writes a string to the file.
   */
  public static Value fwrite(Env env,
			     @NotNull BinaryOutput os,
			     InputStream value,
			     @Optional("0x7fffffff") int length)
  {
    try {
      if (os == null)
        return BooleanValue.FALSE;

      return LongValue.create(os.write(value, length));
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  private static ArrayValue globImpl(Env env, String pattern, int flags, 
                                     Path path, String prefix,
                                     ArrayValue result)
  {
    String cwdPattern;
    String subPattern = null;

    int firstSlash = pattern.indexOf('/');

    if (firstSlash < 0)
      cwdPattern = pattern;
    else {
      cwdPattern = pattern.substring(0, firstSlash);

      // strip off any extra slashes
      for (; firstSlash < pattern.length(); firstSlash++) {
        if (pattern.charAt(firstSlash) != '/')
          break;
      }

      subPattern = pattern.substring(firstSlash);
    }

    int fnmatchFlags = 0;

    if ((flags & GLOB_NOESCAPE) != 0)
      fnmatchFlags = FNM_NOESCAPE;

    boolean doBraces = (flags & GLOB_BRACE) != 0;

    String globRegex = globToRegex(cwdPattern, fnmatchFlags, doBraces);

    if (globRegex == null)
      return null;

    Pattern compiledGlobRegex;

    try {
      compiledGlobRegex = Pattern.compile(globRegex);
    } catch (PatternSyntaxException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }

    String [] list;

    try {
      list = path.list();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }

    for (String entry : list) {
      Matcher matcher = compiledGlobRegex.matcher(entry);

      if (matcher.matches()) {
        UnicodeBuilderValue sb = new UnicodeBuilderValue();

        sb.append(prefix);

        if (prefix.length() > 0)
          sb.append("/");

        sb.append(entry);

        Path entryPath = path.lookup(entry);

        if (entryPath != null && entryPath.isDirectory()) {
          if (firstSlash >= 0 && subPattern.length() > 0) {
            // ArrayValue.add only adds values when the argument is an
            // actual array
            
            boolean isNull = null == globImpl(env,
                                              subPattern,
                                              flags,
                                              entryPath,
                                              sb.toString(),
                                              result);
            
            if ((flags & GLOB_ERR) != 0 && isNull)
              return null;
          } else if ((flags & GLOB_MARK) != 0) {
            sb.append("/");
          }
        }

        if ((firstSlash < 0 || subPattern.length() == 0) && 
            (((flags & GLOB_ONLYDIR) == 0) ||
             (((flags & GLOB_ONLYDIR) != 0) && 
              (entryPath != null && entryPath.isDirectory()))))
          result.put(sb);
      }
    }

    return result;
  }

  /**
   * Matches all files with the given pattern.
   */
  public static Value glob(Env env, StringValue pattern, @Optional int flags)
  {
    Path path = env.getPwd();

    String trimmedPattern = pattern.toString();

    if (pattern.length() > 0 && pattern.charAt(0) == '/') {
      int i;

      // strip off any leading slashes
      for (i = 0; i < pattern.length(); i++) {
        if (pattern.charAt(i) != '/')
          break;
      }

      path = path.lookup("/");

      trimmedPattern = pattern.substring(i).toString();
    }

    ArrayValue result = new ArrayValueImpl();
    
    result = globImpl(env, trimmedPattern, flags, path, "", result);

    if (result == null)
      return BooleanValue.FALSE;
    else if (result.getSize() == 0 && (flags & GLOB_NOCHECK) != 0)
      result.put(pattern);

    if ((flags & GLOB_NOSORT) == 0)
      result.sort(ArrayValue.ValueComparator.CMP, true, true);

    return result;
  }

  /**
   * Returns the current working directory.
   *
   * @return the current directory
   */
  public static String getcwd(Env env)
  {
    return env.getPwd().getNativePath();
  }

  /**
   * Returns true if the path is a directory.
   *
   * @param path the path to check
   */
  public static boolean is_dir(@NotNull Path path)
  {
    if (path == null)
      return false;
    
    return path.isDirectory();
  }

  /**
   * Returns true if the path is an executable file
   *
   * @param path the path to check
   */
  public static boolean is_executable(@NotNull Path path)
  {
    if (path == null)
      return false;
    
    return path.isExecutable();
  }

  /**
   * Returns true if the path is a file.
   *
   * @param path the path to check
   */
  public static boolean is_file(@NotNull Path path)
  {
    if (path == null)
      return false;
    
    return path.isFile();
  }

  /**
   * Returns true if the path is a symbolic link
   *
   * @param path the path to check
   */
  public static boolean is_link(Env env, @NotNull Path path)
  {
    if (path == null)
      return false;
    
    return path.isLink();
  }

  /**
   * Returns true if the path is readable
   *
   * @param path the path to check
   */
  public static boolean is_readable(Path path)
  {
    return path.canRead();
  }

  /**
   * Returns true for an uploaded file.
   *
   * @param path the temp name of the uploaded file
   */
  public static boolean is_uploaded_file(Env env, @NotNull Path path)
  {
    // php/1663, php/1664

    if (path == null)
      return false;

    String tail = path.getTail();

    return env.getUploadDirectory().lookup(tail).canRead();
  }

  /**
   * Returns true if the path is writable
   *
   * @param path the path to check
   */
  public static boolean is_writable(Path path)
  {
    if (path == null)
      return false;

    return path.canWrite();
  }

  /**
   * Returns true if the path is writable
   *
   * @param path the path to check
   */
  public static boolean is_writeable(Path path)
  {
    return is_writable(path);
  }

  /**
   * Creates a hard link
   */
  public boolean link(Env env, Path source, Path destination)
  {
    try {
      return destination.createLink(source, true);
    } catch (Exception e) {
      env.warning(e);

      return false;
    }
  }

  public static long linkinfo(Env env, Path path)
  {
    // XXX: Hack to trigger lstat() in JNI code
    if (path.isLink())
      return path.getDevice();
    else
      return 0;
  }

  /**
   * Returns file statistics
   */
  public static Value lstat(Env env, StringValue filename)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, filename);

    if (wrapper != null)
      // XXX flags?
      return wrapper.url_stat(env, filename, 
          LongValue.create(StreamModule.STREAM_URL_STAT_LINK));

    Path path = env.getPwd().lookup(filename.toString());

    // XXX: Hack to trigger lstat() in JNI code
    path.isLink();

    return statImpl(env, path);
  }

  /**
   * Makes the directory
   *
   * @param path the directory to make
   */
  public static boolean mkdir(Env env, StringValue dirname,
			      @Optional int mode,
			      @Optional boolean recursive,
			      @Optional Value context)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, dirname);

    if (wrapper != null)
      // XXX options?
      return wrapper.mkdir(env, dirname, 
                           LongValue.create(mode), LongValue.ZERO);

    Path path = env.getPwd().lookup(dirname.toString());

    try {
      if (recursive)
        return path.mkdirs();
      else
        return path.mkdir();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Moves the uploaded file.
   *
   * @param path the temp name of the uploaded file
   * @param dst the destination path
   */
  public static boolean move_uploaded_file(Env env, @NotNull Path src, @NotNull Path dst)
  {
    // php/1665, php/1666

    if (src == null)
      return false;

    if (dst == null)
      return false;

    String tail = src.getTail();

    src = env.getUploadDirectory().lookup(tail);

    try {
      if (src.canRead()) {
        src.renameTo(dst);
        return true;
      }
      else
        return false;
    } catch (IOException e) {
      env.warning(e);

      return false;
    }
  }

  /**
   * Opens a directory
   *
   * @param pathName the directory to open
   */
  public static Value opendir(Env env, StringValue pathName,
                              @Optional Value context)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, pathName);

    if (wrapper != null)
      /// XXX options?
      return wrapper.opendir(env, pathName, LongValue.ZERO);
 
    try {
      Path path = env.getPwd().lookup(pathName.toString());

      if (path.isDirectory())
        return new DirectoryValue(env, path);
      else {
        env.warning(L.l("{0} is not a directory", path.getFullPath()));

        return BooleanValue.FALSE;
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses the ini file.
   */
  public static Value parse_ini_file(Env env,
				     Path path,
				     @Optional boolean processSections)
  {
    ReadStream is = null;
    
    try {
      is = path.openRead();
      is.setEncoding(env.getScriptEncoding());

      return parseIni(env, is, processSections);
    } catch (IOException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      if (is != null)
	is.close();
    }
  }

  private static ArrayValue parseIni(Env env,
				     ReadStream is,
				     boolean processSections)
    throws IOException
  {
    ArrayValue top = new ArrayValueImpl();
    ArrayValue section = top;
    
    int ch;

    while ((ch = is.read()) >= 0) {
      if (Character.isWhitespace(ch)) {
      }
      else if (ch == ';') {
	for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
	}
      }
      else if (ch == '[') {
	StringBuilder sb = new StringBuilder();

	for (ch = is.read(); ch >= 0 && ch != ']'; ch = is.read()) {
	  sb.append((char) ch);
	}

	String name = sb.toString().trim();

	if (processSections) {
	  section = new ArrayValueImpl();
	  top.put(env.createString(name), section);
	}
      }
      else if (isValidIniKeyChar((char) ch)) {
	StringBuilder sb = new StringBuilder();

	for (; isValidIniKeyChar((char) ch); ch = is.read()) {
	  sb.append((char) ch);
	}

	String key = sb.toString().trim();

	for (; ch >= 0 && ch != '='; ch = is.read()) {
	}

	for (ch = is.read(); ch == ' ' || ch == '\t'; ch = is.read()) {
	}

	Value value = parseIniValue(env, ch, is);

	section.put(env.createString(key), value);
      }
    }

    return top;
  }

  private static Value parseIniValue(Env env, int ch, ReadStream is)
    throws IOException
  {
    if (ch == '\r' || ch == '\n')
      return NullValue.NULL;

    if (ch == '"') {
      StringValue sb = env.createUnicodeBuilder();
      
      for (ch = is.read(); ch >= 0 && ch != '"'; ch = is.read()) {
	sb.append((char) ch);
      }

      skipToEndOfLine(ch, is);

      return sb;
    }
    else if (ch == '\'') {
      StringValue sb = env.createUnicodeBuilder();
      
      for (ch = is.read(); ch >= 0 && ch != '\''; ch = is.read()) {
	sb.append((char) ch);
      }

      skipToEndOfLine(ch, is);

      return sb;
    }
    else {
      StringBuilder sb = new StringBuilder();

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
	sb.append((char) ch);
      }

      String value = sb.toString().trim();

      if (value.equalsIgnoreCase("null"))
	return env.createEmptyString();
      else if (value.equalsIgnoreCase("true")
	       || value.equalsIgnoreCase("yes"))
	return env.createString("1");
      else if (value.equalsIgnoreCase("false")
	       || value.equalsIgnoreCase("no"))
	return env.createEmptyString();

      if (env.isDefined(value))
	return env.createString(env.getConstant(value).toString());
      else
	return env.createString(value);
    }
  }

  private static boolean isValidIniKeyChar(char ch)
  {
    if (ch <= 0
	|| ch == '='
	|| ch == ';'
	|| ch == '{'
	|| ch == '}'
	|| ch == '|'
	|| ch == '&'
	|| ch == '~'
	|| ch == '!'
	|| ch == '['
	|| ch == '('
	|| ch == ')'
	|| ch == '"')
      return false;
    else
      return true;
  }

  
  private static void skipToEndOfLine(int ch, ReadStream is)
    throws IOException
  {
    for (; ch > 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
    }
  }

  /**
   * Parses the path, splitting it into parts.
   */
  public static Value pathinfo(Env env, String path, @Optional Value optionsV)
  {
    if (optionsV == null)
      return env.createEmptyString();
    
    if (path == null) {
      if (! (optionsV instanceof DefaultValue)) {
        return env.createEmptyString();
      }

      ArrayValueImpl value = new ArrayValueImpl();
      value.put(env.createString("basename"), env.createString(""));
      value.put(env.createString("filename"), env.createString(""));

      return value;
    }

    int p = path.lastIndexOf('/');

    String dirname;
    if (p >= 0) {
      dirname = path.substring(0, p);
      path = path.substring(p + 1);
    }
    else {
      dirname = ".";
    }

    p = path.indexOf('.');
    
    String filename = path;
    String ext = "";
    
    if (p > 0) {
      filename = path.substring(0, p);
      ext = path.substring(p + 1);
    }

    if (! (optionsV instanceof DefaultValue)) {
      int options = optionsV.toInt();

      if ((options & PATHINFO_DIRNAME) == PATHINFO_DIRNAME)
        return env.createString(dirname);
      else if ((options & PATHINFO_BASENAME) == PATHINFO_BASENAME)
        return env.createString(path);
      else if ((options & PATHINFO_EXTENSION) == PATHINFO_EXTENSION)
        return env.createString(ext);
      else if ((options & PATHINFO_FILENAME) == PATHINFO_FILENAME)
        return env.createString(filename);
      else
        return env.createEmptyString();
    }
    else {
      ArrayValueImpl value = new ArrayValueImpl();

      value.put(env.createString("dirname"), env.createString(dirname));
      value.put(env.createString("basename"), env.createString(path));
      value.put(env.createString("extension"), env.createString(ext));
      value.put(env.createString("filename"), env.createString(filename));

      return value;
    }
  }

  public static int pclose(Env env, @NotNull BinaryStream stream)
  {
    if (stream instanceof PopenInput)
      return ((PopenInput) stream).pclose();
    else if (stream instanceof PopenOutput)
      return ((PopenOutput) stream).pclose();
    else {
      env.warning(L.l("{0} was not returned by popen()", stream));

      return -1;
    }
  }

  @ReturnNullAsFalse 
  public static BinaryStream popen(Env env, 
                                   @NotNull String command, 
                                   @NotNull StringValue mode)
  {
    boolean doRead = false;

    if (mode.toString().equalsIgnoreCase("r"))
      doRead = true;
    else if (mode.toString().equalsIgnoreCase("w"))
      doRead = false;
    else
      return null;

    String []args = new String[3];

    try {
      if (Path.isWindows()) {
        args[0] = "cmd";
        args[1] = "/c";
      }
      else {
        args[0] = "sh";
        args[1] = "-c";
      }

      args[2] = command;

      Process process = Runtime.getRuntime().exec(args);

      if (doRead)
        return new PopenInput(env, process);
      else
        return new PopenOutput(env, process);
    } catch (Exception e) {
      env.warning(e.getMessage(), e);

      return null;
    }
  }
  
  /**
   * Reads the next entry
   *
   * @param dirV the directory resource
   */
  public static Value readdir(Env env, @NotNull DirectoryValue dir)
  {
    if (dir == null)
      return BooleanValue.FALSE;
    
    return dir.readdir();
  }

  /**
   * Read the contents of a file and write them out.
   */
  public Value readfile(Env env,
                        StringValue filename,
                        @Optional boolean useIncludePath,
                        @Optional Value context)
  {
    if (filename == null)
      return BooleanValue.FALSE;

    BinaryStream s = fopen(env, filename, "r", useIncludePath, context);

    if (! (s instanceof BinaryInput))
      return BooleanValue.FALSE;

    BinaryInput is = (BinaryInput) s;

    try {
      return fpassthru(env, is);
    } finally {
      is.close();
    }
  }

  /**
   * The readlink always fails.
   */
  public static boolean readlink(Env env, String path)
  {
    env.stub("readlink(" + path + ")");

    return false;
  }

  /**
   * Returns the actual path name.
   */
  public static String realpath(Path path)
  {
    String fullPath = path.getFullPath();

    if (fullPath.endsWith("/") && ! fullPath.equals("/"))
      return fullPath.substring(0, fullPath.length() - 1);
    else
      return fullPath;
  }

  /**
   * Renames a file
   *
   * @param fromPath the path to change to
   * @param toPath the path to change to
   */
  public static boolean rename(Env env, StringValue from, StringValue to)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, from);

    if (wrapper != null)
      return wrapper.rename(env, from, to);

    Path fromPath = env.getPwd().lookup(from.toString());
    Path toPath = env.getPwd().lookup(to.toString());

    if (!fromPath.canRead()) {
      env.warning(L.l("{0} cannot be read", fromPath.getFullPath()));
      return false;
    }

    try {
      return fromPath.renameTo(toPath);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Rewinds the stream.
   *
   * @param is the file resource
   */
  public static Value rewind(Env env,
			     @NotNull BinaryInput is)
  {
    if (is == null)
      return BooleanValue.FALSE;

    fseek(env, is, 0, BinaryInput.SEEK_SET);
    
    return BooleanValue.TRUE;
  }

  /**
   * Rewinds the directory listing
   *
   * @param dirV the directory resource
   */
  public static void rewinddir(Env env, @NotNull DirectoryValue dir)
  {
    if (dir == null)
      return;
    
    dir.rewinddir();
  }

  /**
   * remove a directory
   */
  public static boolean rmdir(Env env,
                              StringValue filename,
                              @Optional Value context)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, filename);

    if (wrapper != null)
      // XXX options?
      return wrapper.rmdir(env, filename, LongValue.ZERO);

    // quercus/160s

    // XXX: safe_mode
    try {
      Path path = env.getPwd().lookup(filename.toString());

      if (!path.isDirectory()) {
        env.warning(L.l("{0} is not a directory", path.getFullPath()));
        return false;
      }

      return path.remove();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Closes the directory
   *
   * @param dirV the directory resource
   */
  public static void closedir(Env env, @NotNull DirectoryValue dirV)
  {
    if (dirV == null)
      return;
    
    dirV.close();
  }

  /**
   * Scan the directory
   *
   * @param fileName the directory
   */
  public static Value scandir(Env env, String fileName,
                              @Optional("1") int order,
                              @Optional Value context)
  {
    if (fileName == null) {
      env.warning(L.l("file name must not be NULL"));
      return BooleanValue.FALSE;
    }

    try {
      Path path = env.getPwd().lookup(fileName);

      if (!path.isDirectory()) {
        env.warning(L.l("{0} is not a directory", path.getFullPath()));
        return BooleanValue.FALSE;
      }

      String []values = path.list();

      Arrays.sort(values);

      ArrayValue result = new ArrayValueImpl();

      if (order == 1) {
        for (int i = 0; i < values.length; i++)
          result.append(new LongValue(i), env.createString(values[i]));
      }
      else {
        for (int i = values.length - 1; i >= 0; i--) {
          result.append(new LongValue(values.length - i - 1),
          env.createString(values[i]));
        }
      }

      return result;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the write buffer.
   */
  public static int set_file_buffer(Env env, BinaryOutput stream,
                                    int bufferSize)
  {
    return StreamModule.stream_set_write_buffer(env, stream, bufferSize);
  }

  /**
   * Returns file statistics
   */
  public static Value stat(Env env, StringValue filename)
  {
    ProtocolWrapper wrapper = getProtocolWrapper(env, filename);

    if (wrapper != null)
      // XXX flags?
      return wrapper.url_stat(env, filename, LongValue.ZERO);

    Path path = env.getPwd().lookup(filename.toString());

    return statImpl(env, path);
  }

  static Value statImpl(Env env, Path path)
  {
    if (! path.exists()) {
      env.warning(L.l("stat failed for {0}", path.getFullPath().toString()));
      return BooleanValue.FALSE;
    }

    ArrayValue result = new ArrayValueImpl();

    result.put(path.getDevice());
    result.put(path.getInode());
    result.put(path.getMode());
    result.put(path.getNumberOfLinks());
    result.put(path.getUser());
    result.put(path.getGroup());
    result.put(path.getDeviceId());
    result.put(path.getLength());

    result.put(path.getLastAccessTime() / 1000L);
    result.put(path.getLastModified() / 1000L);
    result.put(path.getLastStatusChangeTime() / 1000L);
    result.put(path.getBlockSize());
    result.put(path.getBlockCount());

    result.put("dev", path.getDevice());
    result.put("ino", path.getInode());
    
    result.put("mode", path.getMode());
    result.put("nlink", path.getNumberOfLinks());
    result.put("uid", path.getUser());
    result.put("gid", path.getGroup());
    result.put("rdev", path.getDeviceId());
    
    result.put("size", path.getLength());

    result.put("atime", path.getLastAccessTime() / 1000L);
    result.put("mtime", path.getLastModified() / 1000L);
    result.put("ctime", path.getLastStatusChangeTime() / 1000L);
    result.put("blksize", path.getBlockSize());
    result.put("blocks", path.getBlockCount());

    return result;
  }

  /**
   * Creates a symlink
   */
  public boolean symlink(Env env, Path source, Path destination)
  {
    try {
      return destination.createLink(source, false);
    } catch (Exception e) {
      env.warning(e);

      return false;
    }
  }

  /**
   * Creates a temporary file.
   */
  public static Value tempnam(Env env, Path dir, String prefix)
  {
    // quercus/160u

    if (!dir.isDirectory()) {
      env.warning(L.l("{0} is not a directory", dir.getFullPath()));
      return BooleanValue.FALSE;
    }

    try {
      Path path = dir.createTempFile(prefix, ".tmp");
      return env.createString(path.getTail());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Creates a temporary file.
   */
  @ReturnNullAsFalse
  public static FileInputOutput tmpfile(Env env)
  {
    try {
      // XXX: location of tmp files s/b configurable

      Path tmp = env.getPwd().lookup("file:/tmp");

      tmp.mkdirs();

      Path file = tmp.createTempFile("resin", "tmp");

      return new FileInputOutput(env, file, false, false, true);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * sets the time to the current time
   */
  public static boolean touch(Path path,
                              @Optional int time,
                              @Optional int atime)
  {
    // XXX: atime not implemented (it might be > time)

    try {
      if (path.exists()) {
        if (time > 0)
          path.setLastModified(1000L * time);
        else
          path.setLastModified(Alarm.getCurrentTime());
      }
      else {
        WriteStream ws = path.openWrite();
        ws.close();
      }

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * umask call
   */
  public static int umask(Env env, int mask)
  {
    env.stub("umask(" + mask + ")");

    return mask;
  }

  /**
   * remove call
   */
  public static boolean unlink(Env env,
                               StringValue filename,
                               @Optional Value context)
  {
    // quercus/160p

    // XXX: safe_mode
    try {
      ProtocolWrapper wrapper = getProtocolWrapper(env, filename);

      if (wrapper != null)
        return wrapper.unlink(env, filename);

      Path path = env.getPwd().lookup(filename.toString());

      return path.remove();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  static {
    ProtocolWrapper zlibProtocolWrapper = new ZlibProtocolWrapper();
    StreamModule.stream_wrapper_register(new StringBuilderValue("compress.zlib"),
                                         zlibProtocolWrapper);
    StreamModule.stream_wrapper_register(new StringBuilderValue("zlib"),
                                         zlibProtocolWrapper);
    StreamModule.stream_wrapper_register(new StringBuilderValue("php"),
                                         new PhpProtocolWrapper());

    _constMap.put("SEEK_SET", LongValue.create(BinaryInput.SEEK_SET));
    _constMap.put("SEEK_CUR", LongValue.create(BinaryInput.SEEK_CUR));
    _constMap.put("SEEK_END", LongValue.create(BinaryInput.SEEK_END));

    _constMap.put("LOCK_SH", LongValue.create(LOCK_SH));
    _constMap.put("LOCK_EX", LongValue.create(LOCK_EX));
    _constMap.put("LOCK_UN", LongValue.create(LOCK_UN));
    _constMap.put("LOCK_NB", LongValue.create(LOCK_NB));

    _constMap.put("FNM_PATHNAME", LongValue.create(FNM_PATHNAME));
    _constMap.put("FNM_NOESCAPE", LongValue.create(FNM_NOESCAPE));
    _constMap.put("FNM_PERIOD", LongValue.create(FNM_PERIOD));
    _constMap.put("FNM_CASEFOLD", LongValue.create(FNM_CASEFOLD));

    _constMap.put("GLOB_MARK", LongValue.create(GLOB_MARK));
    _constMap.put("GLOB_NOSORT", LongValue.create(GLOB_NOSORT));
    _constMap.put("GLOB_NOCHECK", LongValue.create(GLOB_NOCHECK));
    _constMap.put("GLOB_NOESCAPE", LongValue.create(GLOB_NOESCAPE));
    _constMap.put("GLOB_BRACE", LongValue.create(GLOB_BRACE));
    _constMap.put("GLOB_ONLYDIR", LongValue.create(GLOB_ONLYDIR));
  }

  static final IniDefinition INI_ALLOW_URL_FOPEN
    = _iniDefinitions.add("allow_url_fopen", true, PHP_INI_SYSTEM);

  static final IniDefinition INI_USER_AGENT
    = _iniDefinitions.add("user_agent", null, PHP_INI_ALL);

  static final IniDefinition INI_DEFAULT_SOCKET_TIMEOUT
    = _iniDefinitions.add("default_socket_timeout", 60, PHP_INI_ALL);

  static final IniDefinition INI_FROM
    = _iniDefinitions.add("from", "", PHP_INI_ALL);

  static final IniDefinition INI_AUTO_DETECT_LINE_ENDINGS
    = _iniDefinitions.add("auto_detect_line_endings", false, PHP_INI_ALL);

  // file uploads

  static final IniDefinition INI_FILE_UPLOADS
    = _iniDefinitions.add("file_uploads", true, PHP_INI_SYSTEM);

  static final IniDefinition INI_UPLOAD_TMP_DIR
    = _iniDefinitions.add("upload_tmp_dir", null, PHP_INI_SYSTEM);

  static final IniDefinition INI_UPLOAD_MAX_FILESIZE
    = _iniDefinitions.add("upload_max_filesize", "2M", PHP_INI_SYSTEM);
}

