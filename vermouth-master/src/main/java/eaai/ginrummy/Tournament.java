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

import ginrummy.GinRummyPlayer;
import ginrummy.GinRummyGame;
import java.util.Random;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 */
public abstract class Tournament {
	/* */
	protected static final Logger LOG = LogManager.getLogger(Tournament.class);

	/**
	 */
	protected final Random random;

	/**
	 */
	protected Tournament(Random random) {
		this.random = random;
	}

	/**
	 */
	public abstract void run();

	/**
	 */
	protected int[] play(GinRummyPlayer player0, GinRummyPlayer player1) {
		long seed = random.nextLong();

		GinRummyGame game = new GinRummyGame(player0, player1);
		return game.play();
	}
}
