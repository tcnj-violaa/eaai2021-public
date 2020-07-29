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
import eaai.ginrummy.util.RunningStatistic;
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
public class EloTournament extends Tournament {
	/* */
	private static final Logger LOG = LogManager.getLogger(EloTournament.class);

	/* */
	private final List<GinRummyPlayerClass> classes;

	/* */
	private final int rounds;

	/* */
	private final boolean verbose;

	/**
	 */
	public EloTournament(List<GinRummyPlayerClass> classes, int rounds, boolean verbose, Random random) {
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

			List<Integer> rankings = players.stream()
				.map(p -> 1000)
				.collect(Collectors.toList());

			List<RunningStatistic> averages = players.stream()
				.map(p -> new RunningStatistic(1000.))
				.collect(Collectors.toList());

			Path playerPath = FileMap.get().getPath("players.csv");
			CSVWriter playerWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(playerPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			playerWriter.writeNext(new String[] { "ID", "NAME" });
			for(int p = 0; p < players.size(); p += 1) { playerWriter.writeNext(new String[] { Integer.toString(p), classes.get(p).name() }); }

			Path roundPath = FileMap.get().getPath("rounds.csv");
			CSVWriter roundWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(roundPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			roundWriter.writeNext(new String[] { "ROUND", "PLAYER", "RANKING" });
			for (int p = 0; p < players.size(); p += 1) { roundWriter.writeNext(new String[] { Integer.toString(0), Integer.toString(p), Integer.toString(rankings.get(p)) }); }

			Path gamesPath = FileMap.get().getPath("games.csv");
			CSVWriter gamesWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(gamesPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			gamesWriter.writeNext(new String[] { "MATCH", "ROUND", "PLAYER", "SCORE", "WIN" });

			Path statsPath = FileMap.get().getPath("stats.csv");
			CSVWriter statsWriter = new CSVWriter(new PrintWriter(Files.newOutputStream(statsPath, StandardOpenOption.CREATE_NEW)), ',', '\0', '\\', "\n");
			statsWriter.writeNext(new String[] { "METHOD", "PLAYER", "TIME" });

			int m = 0;
			for(int r = 1; r <= rounds; r += 1) {
				LOG.info("starting round {} with average rankings {}", Integer.toString(r), averages.stream().map(p -> String.format("%06.4f", p.mean())).collect(Collectors.toList()));

				List<Integer> updates = players.stream()
					.map(p -> 0)
					.collect(Collectors.toList());
				for(int p0 = 0; p0 < players.size(); p0 += 1, m += 1) {
					for(int p1 = p0 + 1; p1 < players.size(); p1 += 1, m += 1) {
						LOG.info("starting match [{}] {} (rank {}) vs [{}] {} (rank {})", p0, classes.get(p0).name(), rankings.get(p0), p1, classes.get(p1).name(), rankings.get(p1));

						PrintStream gameStream = System.out;
						if(verbose) {
							Path gamePath = FileMap.get().getPath(String.format("%d-%dv%d-round%d.txt", m, p0, p1, r));
							gameStream = new PrintStream(Files.newOutputStream(gamePath, StandardOpenOption.CREATE_NEW));
						}
						PrintStream out = System.out; System.setOut(gameStream);
						PrintStream err = System.err; System.setErr(gameStream);

						GinRummyAgent agent0 = new GinRummyAgent(players.get(p0), p0, statsWriter, gameStream, gameStream);
						GinRummyAgent agent1 = new GinRummyAgent(players.get(p1), p1, statsWriter, gameStream, gameStream);
						GinRummyGame game = new GinRummyGame(agent0, agent1);
						int[] scores = game.play();

						System.setOut(out);
						System.setErr(err);
						if(gameStream != System.out) { gameStream.close(); }

						// earned score (is a draw possible?)
						double s0 = scores[0] > scores[1] ? 1 : 0;
						double s1 = scores[1] > scores[0] ? 1 : 0;
						// quality ranking score
						double q0 = Math.pow(10, rankings.get(p0) / 400.);
						double q1 = Math.pow(10, rankings.get(p1) / 400.);
						// expected ranking socre
						double e0 = q0 / (q0 + q1);
						double e1 = q1 / (q0 + q1);
						// calculate and accumulate score adjustments
						final int K = 8;
						double d0 = K * (s0 - e0);
						double d1 = K * (s1 - e1);
						updates.set(p0, updates.get(p0) + (int)Math.round(d0));
						updates.set(p1, updates.get(p1) + (int)Math.round(d1));

						gamesWriter.writeNext(new String[] { Integer.toString(m), Integer.toString(r), Integer.toString(p0), Integer.toString(scores[0]), Integer.toString(scores[0] > scores[1] ? 1 : 0) });
						gamesWriter.writeNext(new String[] { Integer.toString(m), Integer.toString(r), Integer.toString(p1), Integer.toString(scores[1]), Integer.toString(scores[0] < scores[1] ? 1 : 0) });
					}
				}

				for (int p = 0; p < players.size(); p += 1) {
					rankings.set(p, rankings.get(p) + (int)Math.round(updates.get(p)));
					averages.get(p).add(rankings.get(p));
					roundWriter.writeNext(new String[] { Integer.toString(r), Integer.toString(p), Integer.toString(rankings.get(p)) });
				}
			}

			LOG.info("final average rankings {}", averages.stream().map(p -> String.format("%06.2f", p.mean())).collect(Collectors.toList()));

			playerWriter.close();
			 roundWriter.close();
			 gamesWriter.close();
			 statsWriter.close();
		}
		catch(IOException except) {
			LOG.fatal("could not write game files", except);
			System.exit(1);
		}
	}
}
