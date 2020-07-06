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

import com.opencsv.CSVWriter;
import ginrummy.GinRummyGame;
import ginrummy.GinRummyPlayer;
import eaai.ginrummy.util.FileMap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 */
public class OneAllTournament extends Tournament {
	/* */
	private static final Logger LOG = LogManager.getLogger(OneAllTournament.class);

	/* */
	private final List<GinRummyPlayerClass> classes;

	/* */
	private final int rounds;

	/* */
	private final boolean verbose;

	/**
	 */
	public OneAllTournament(List<GinRummyPlayerClass> classes, int rounds, boolean verbose, Random random) {
		super(random);
		this.classes = classes;
		this.rounds = rounds;
		this.verbose = verbose;
	}

	/**
	 */
	public void run() {
		try {
			LOG.debug("generating GinRummyPlayer instances");
			List<GinRummyPlayer> players = classes.stream()
				.map(c -> c.newInstance())
				.collect(Collectors.toList());

			Path gamesPath = FileMap.get().getPath("games.csv");
			CSVWriter gamesWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(gamesPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			gamesWriter.writeNext(new String[] { "MATCH", "GAME", "PLAYER", "SCORE", "WIN" });

			Path statsPath = FileMap.get().getPath("stats.csv");
			CSVWriter statsWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(statsPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			statsWriter.writeNext(new String[] { "METHOD", "PLAYER", "TIME" });

			int m = 0;
			for(int p = 1; p < players.size(); p += 1, m += 1) {
				LOG.info("starting match [{}]{} vs [{}]{}", 0, classes.get(0), p, classes.get(p));
				int wins = 0;
				for(int g = 0; g < rounds; g += 1) {
					PrintStream gameStream = System.out;
					if(verbose) {
						Path gamePath = FileMap.get().getPath(String.format("%d-%dv%d-game%d.txt", m, 0, p, g));
						gameStream = new PrintStream(Files.newOutputStream(gamePath, StandardOpenOption.CREATE_NEW));
					}
					PrintStream out = System.out; System.setOut(gameStream);
					PrintStream err = System.err; System.setErr(gameStream);

					GinRummyAgent agent0 = new GinRummyAgent(players.get(0), 0, statsWriter, gameStream, gameStream);
					GinRummyAgent agent1 = new GinRummyAgent(players.get(p), p, statsWriter, gameStream, gameStream);
					GinRummyGame game = new GinRummyGame(agent0, agent1);
					int[] scores = game.play();

					System.setOut(out);
					System.setErr(err);
					gameStream.close();

					gamesWriter.writeNext(new String[] { Integer.toString(m), Integer.toString(g), Integer.toString(0), Integer.toString(scores[0]), Integer.toString(scores[0] > scores[1] ? 1 : 0) });
					gamesWriter.writeNext(new String[] { Integer.toString(m), Integer.toString(g), Integer.toString(p), Integer.toString(scores[1]), Integer.toString(scores[0] < scores[1] ? 1 : 0) });
					wins += scores[0] > scores[1] ? 1 : 0;
					LOG.info("game {} final scores [{}, {}]", g, scores[0], scores[1]);
				}
				LOG.info("match [{}]{} ({} wins) vs [{}]{} ({} wins)", 0, classes.get(0), wins, p, classes.get(p), rounds - wins);
			}

			gamesWriter.close();
			statsWriter.close();
		}
		catch(IOException except) {
			LOG.fatal("could not write game files", except);
			System.exit(1);
		}
	}
}
