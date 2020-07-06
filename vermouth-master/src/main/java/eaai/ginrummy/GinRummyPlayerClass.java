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
package eaai.ginrummy;

import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URI;
import java.net.MalformedURLException;
import ginrummy.GinRummyPlayer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * A container class and validation logic for loading GinRummyPlayer derived
 * classes at runtime through reflection.
 *
 * @author Jason Hiebel
 * @version 1.0
 */
public class GinRummyPlayerClass {

	/* terminal logging */
	private static final Logger LOG = LogManager.getLogger(GinRummyPlayerClass.class);

	/* base class file path */
	private final String path;

	/* loaded GinRummyPlayer class */
	private final Class<? extends GinRummyPlayer> player;

	/**
	 * Initialize the GinRummyPlayerClass with the loaded GinRummyPlayer class
	 * file and the associated path to the class file.
	 *
	 * @param path base class file path
	 * @param player loaded GinRummyPlayer class
	 */
	public GinRummyPlayerClass(String path, Class<? extends GinRummyPlayer> player) {
		this.path = path;
		this.player = player;
	}

	/**
	 * Returns the base class file path.
	 *
	 * @return the base class file path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Returns a new instance of the loaded GinRummyPlayer class.
	 *
	 * @return a new instance of the loaded GinRummyPlayer class
	 */
	public GinRummyPlayer newInstance() {
		try {
			return player.newInstance();
		}
		catch(InstantiationException|IllegalAccessException except) {
			LOG.fatal("could not instantiate player", except);
			System.exit(1);
		}
		return null;
	}

	/*
	 */
	@Override
	public String toString() {
		return String.format("%s/(%s)", path, player);
	}

	/**
	 * Verifies the loaded class as a GinRummyPlayer class instance. Checks
	 * that the loaded class is derived from GinRummyPlayer and has a default
	 * (empty) constructor.
	 *
	 * @return a verified instance of the loaded class, cast as a GinRummyPlayer
	 */
	public static Class<? extends GinRummyPlayer> verify(Class<?> player) {
		LOG.debug("{} ({} / {}) available.",
			player,
			player.getCanonicalName(),
			player.getSimpleName()
		);

		try {
			// verify no argument constructor exists
			player.getConstructor();
			LOG.debug("default (no argument) constructor found.");

			// verify class cast and return cast
			return player.asSubclass(GinRummyPlayer.class);
		}
		catch(NoSuchMethodException except) {
			LOG.error("could not find (no argument) constructor!", except);
			System.exit(1);
		}
		catch(ClassCastException except) {
			LOG.error("could not cast class as GinRummyPlayer!", except);
			System.exit(1);
		}
		return null;
	}

	/**
	 * Load and verify a GinRummyPlayer class file from the specified JAR URL.
	 * The url is expected to have a valid protocol and file description; for
	 * a JAR file my.jar containing GinRummyPlayer class file MyPlayer.class,
	 * the corresponding URL would read jar:file:./my.jar!/MyPlayer, noting that
	 * the JAR path can be any valid relative or absolute path and the class
	 * file is specified without the .class suffix.
	 *
	 * @param url the specified JAR URL
	 * @return an instance of this class containing a loaded and validated
	 * GinRummyPlayer class.
	 */
	public static GinRummyPlayerClass fromJar(URL url) {
		LOG.trace("using 'jar:file:' protocol.");

		try {
			JarURLConnection connection = (JarURLConnection)(url.openConnection());
			LOG.trace("jar connection established");

			URLClassLoader loader = new URLClassLoader(new URL[] { connection.getJarFileURL() });
			LOG.trace("class loader established");

			String name = Paths.get(connection.getJarFileURL().getPath()).toAbsolutePath().normalize().toString();
			Class<? extends GinRummyPlayer> player = verify(loader.loadClass(connection.getEntryName()));
			return new GinRummyPlayerClass(name, player);
		}
		catch(IOException except) {
			LOG.fatal("could not open the specified jar!", except);
			System.exit(1);
		}
		catch(ClassNotFoundException except) {
			LOG.fatal("could not find class in the specified jar!", except);
			System.exit(1);
		}
		return null;
	}

	/**
	 * Load and verify a GinRummyPlayer class file from the specified class
	 * file URL. The url is expected to have a valid protocol and file
	 * description; for a GinRummyPlayer class file MyPlayer.class, the
	 * corresponding URL would read file:./MyPlayer, noting that the class file
	 * path can be any valid relative or absolute path and the class file is
	 * specified withou the .class suffix.
	 *
	 * @param url the specified JAR URL
	 * @return an instance of this class containing a loaded and validated
	 * GinRummyPlayer class.
	 */
	public static GinRummyPlayerClass fromFile(URL url) {
		LOG.trace("using 'file:' protocol.");

		try {
			Path path = Paths.get("").resolve(url.getPath()).toAbsolutePath().normalize();
			LOG.trace("using path {}", path);

			URLClassLoader loader = new URLClassLoader(new URL[] { path.getParent().toFile().toURI().toURL() });
			LOG.trace("class loader established");

			String name = String.format("%s", path.getParent().toString());
			Class<? extends GinRummyPlayer> player = verify(loader.loadClass(path.getFileName().toString()));
			return new GinRummyPlayerClass(name, player);
		}
		catch(IOException except) {
			LOG.fatal("could not resolve the specified file!", except);
			System.exit(1);
		}
		catch(ClassNotFoundException except) {
			LOG.fatal("could not find class at the specified directory!", except);
			System.exit(1);
		}
		return null;
	}

	/**
	 * Load a Gin Rummy player class from the specified file, either as a
	 * direct reference to a class file in the file system or as a direct
	 * reference to a class file within a jar.
	 *
	 * @param file the specified class file
	 * @return an instance of this class containing a loaded and validated
	 * GinRummyPlayer class.
	 */
	public static GinRummyPlayerClass from(String file) {
		try {
			LOG.trace("loading {} player.", file);
			URL url = new URL(file);

			GinRummyPlayerClass player;
			switch(url.getProtocol()) {
				case "jar"  : player = fromJar  (url); break;
				case "file" : player = fromFile (url); break;
				default:
					throw new MalformedURLException(String.format("unknown protocol %s", url.getProtocol()));
			}

			LOG.info("player {} loaded.", player);
			return player;
		}
		catch(MalformedURLException except) {
			LOG.fatal("invalid filename!", except);
			System.exit(1);
		}
		return null;
	}
}
