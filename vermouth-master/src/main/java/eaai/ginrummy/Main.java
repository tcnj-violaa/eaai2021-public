/*
 * Copyright (C) 2020 Todd Neller
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

import ginrummy.GinRummyGame;
import ginrummy.GinRummyPlayer;
import eaai.ginrummy.util.StubPrintStream;
import eaai.ginrummy.util.FileMap;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;
import java.nio.file.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Main entry point and command line interface for the EAAI Gin Rummy
 * tournament and agent testing software.
 *
 * @author Jason Hiebel
 * @version 1.0
 */
public class Main {
	/* */
	private static final Logger LOG = LogManager.getLogger(Main.class);

	/* */
	private static Options generateOptions() {
		/* create command line argument options */
		Options opt = new Options();

		/* misc. options */
		opt.addOption(Option.builder("v")
			.longOpt("verbose")
			.desc("output game details")
			.build());

		opt.addOption(Option.builder("l")
			.longOpt("log")
			.hasArg()
			.argName("level")
			.desc("the logging level [OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL]")
			.build());

		opt.addOption(Option.builder("h")
			.longOpt("help")
			.desc("print this description")
			.build());

		/* specify competition type */
		opt.addOptionGroup(new OptionGroup()
			.addOption(Option.builder("1v")
				.longOpt("oneall")
				.argName("oneall")
				.desc("run a series of 1v1 competitions between the first competitor and subsequent competitors")
				.build())
			.addOption(Option.builder("rr")
				.longOpt("roundrobin")
				.argName("roundrobin")
				.desc("run a round robin competition between the competitors")
				.build())
			.addOption(Option.builder("el")
				.longOpt("elo")
				.argName("elo")
				.desc("run a Elo-scored competition between the competitors")
				.build()));

		/* specify competition parameters */
		opt.addOption(Option.builder("id")
			.longOpt("identifier")
			.argName("id")
			.desc("run identifier, used as filename prefix, with timestamp default")
			.hasArg()
			.build());

		/* specify competition parameters */
		opt.addOption(Option.builder("s")
			.longOpt("seed")
			.argName("s")
			.desc("random seed (hexidecimal) for generating game seeds")
			.hasArg()
			.build());

		opt.addOption(Option.builder("g")
			.longOpt("games")
			.hasArg()
			.argName("games")
			.desc("number of games to evaluate per match")
			.build());

		/* specify competitor agents */
		opt.addOption(Option.builder("a")
			.longOpt("agents")
			.argName("agents")
			.desc("agent classfile specifications")
			.hasArgs()
			//.required()
			.build());

		return opt;
	}

	/**
	 */
	public static void main(String[] args) throws ParseException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		try {
			/* parse command line arguments */
			Options opt = generateOptions();
			CommandLine cmd = new DefaultParser().parse(opt, args);

			/* configure all loggers */
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.valueOf(cmd.getOptionValue("l", "INFO")));

			/* print usage information if requested */
			if(cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar vermouth.jar", opt);
			}

			/* competition type */
			String type = null;
			if(cmd.hasOption("1v")) { type = "1v"; }
			if(cmd.hasOption("rr")) { type = "rr"; }
			if(cmd.hasOption("el")) { type = "el"; }
			switch(type) {
				default:   LOG.warn("no competition type specified, setting one-vs-all as default");
				case "1v": LOG.info("competition type: one-vs-all");
					break;
				case "rr": LOG.info("competition type: round-robin");
					break;
				case "el": LOG.info("competition type: elo");
					break;
			}

			/* competition parameters */
			String identifier = cmd.getOptionValue("id", (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")).format(new Date()));
			int games = Integer.parseInt(cmd.getOptionValue("g", "1"));
			boolean verbose = cmd.hasOption("v");
			LOG.info("competition parameters: id ({}), rounds ({}), verbose ({})", identifier, games, verbose);

			/* */
			try {
				FileMap.create(identifier);
			}
			catch(IOException except) {
				LOG.fatal("failed to create experiment data file", except);
				System.exit(1);
			}

			/* random number seed */
			Random random = new Random();
			if(cmd.hasOption("s")) {
				long seed = Long.parseLong(cmd.getOptionValue("s"), 16);
				LOG.info("initializing competition seed to 0x{0:X12}", seed);
				random = new Random(seed);
			}

			GinRummyGame.setPlayVerbose(cmd.hasOption("v"));

			/* competitors */
			List<GinRummyPlayerClass> agents = Arrays.stream(cmd.getOptionValues("a"))
				.map(path -> GinRummyPlayerClass.from(path))
				.collect(Collectors.toList());

			/* tournament */
			switch(type) {
				case "1v" : new OneAllTournament     (agents, games, cmd.hasOption("v"), random).run(); break;
			 	case "rr" : new RoundRobinTournament (agents, games, cmd.hasOption("v"), random).run(); break;
				case "el" : new EloTournament        (agents, games, cmd.hasOption("v"), random).run(); break;
			}

			try {
				FileMap.get().close();
			}
			catch(IOException except) {
				LOG.fatal("failed to close experiment data file", except);
			}
		}
		catch(AlreadySelectedException except) {
			LOG.error("conflict in option specification!", except);
			System.exit(1);
		}
		catch(UnrecognizedOptionException except) {
			LOG.error("unrecognized option!", except);
			System.exit(1);
		}
	}
}
