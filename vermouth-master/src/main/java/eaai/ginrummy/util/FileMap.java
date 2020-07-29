/*
 * Copyright (C) 2020 Jason Hiebel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Information about the GNU General Public License is available online at:
 *   http://www.gnu.org/licenses/
 * To receive a copy of the GNU General Public License, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package eaai.ginrummy.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class FileMap {

	/* */
	private static FileSystem base = null;

	/* */
	private static Map<String, FileSystem> filesystems = new HashMap<>();

	/**
	 */
	public static FileSystem create(String identifier) throws IOException {
		/* */
		if(filesystems.containsKey(identifier)) { }

		/* */
		//Path path = Paths.get(String.format("./%s.zip", identifier)).toAbsolutePath().normalize();
		//URI uri = URI.create("jar:file:" + path);
		URI uri = URI.create("jar:" + Paths.get(String.format("./%s.zip", identifier)).toUri());

		/* */
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");
		FileSystem fs = FileSystems.newFileSystem(uri, env);

		/* */
		if(base == null) { base = fs; }
		filesystems.put(identifier, fs);

		return fs;
	}

	/**
	 */
	public static FileSystem get() {
		return base;
	}

	/**
	 */
	public static FileSystem get(String identifier) {
		return filesystems.get(identifier);
	}
}
