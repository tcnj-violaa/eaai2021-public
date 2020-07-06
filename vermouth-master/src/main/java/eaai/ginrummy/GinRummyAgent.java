package eaai.ginrummy;

import com.opencsv.CSVWriter;
import com.sun.management.ThreadMXBean;
import ginrummy.Card;
import ginrummy.GinRummyPlayer;
import java.lang.management.ManagementFactory;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;

/**
 */
public class GinRummyAgent implements GinRummyPlayer {

	/*
	 */
	private final ThreadMXBean monitor = ((ThreadMXBean) (ManagementFactory.getThreadMXBean()));

	/*
	 */
	private final int id;

	/*
	 */
	private final CSVWriter writer;

	/**
	 */
	public final GinRummyPlayer player;

	/**
	 */
	public final PrintStream out;

	/**
	 */
	public final PrintStream err;

	/**
	 */
	public GinRummyAgent(GinRummyPlayer player, int id, CSVWriter writer, PrintStream out, PrintStream err) {
		this.player = player;
		this.id = id;
		this.writer = writer;
		this.out = out;
		this.err = err;
	}

	/*
	 */
	private static PrintStream swapOut(PrintStream out) {
		PrintStream old = System.out;
		System.setOut(out);
		return old;
	}

	/*
	 */
	private static PrintStream swapErr(PrintStream err) {
		PrintStream old = System.err;
		System.setErr(err);
		return old;
	}

	/**
	 * Inform player of 0-based player number (0/1), starting player number (0/1), and dealt cards
	 * @param playerNum player's 0-based player number (0/1)
	 * @param startingPlayerNum starting player number (0/1)
	 * @param cards dealt cards
	 */
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.startGame(playerNum, startingPlayerNum, cards);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "startGame", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * Return whether or not player will draw the given face-up card on the draw pile.
	 * @param card face-up card on the draw pile
	 * @return whether or not player will draw the given face-up card on the draw pile
	 */
	public boolean willDrawFaceUpCard(Card card) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		boolean ret = player.willDrawFaceUpCard(card);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "willDrawFaceUpCard", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
		return ret;
	}

	/**
	 * Report that the given player has drawn a given card and, if known, what the card is.
	 * If the card is unknown because it is drawn from the face-down draw pile, the drawnCard is null.
	 * Note that a player that returns false for willDrawFaceUpCard will learn of their face-down draw from this method.
	 * @param playerNum - player drawing a card
	 * @param drawnCard - the card drawn or null, depending on whether the card is known to the player or not, respectively.
	 */
	public void reportDraw(int playerNum, Card drawnCard) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportDraw(playerNum, drawnCard);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportDraw", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * Get the player's discarded card.  If you took the top card from the discard pile,
	 * you must discard a different card.
	 * If this is not a card in the player's possession, the player forfeits the game.
	 * @return the player's chosen card for discarding
	 */
	public Card getDiscard() {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		Card ret = player.getDiscard();
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "getDiscard", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
		return ret;
	}


	/**
	 * Report that the given player has discarded a given card.
	 * @param playerNum the discarding player
	 * @param discardedCard the card that was discarded
	 */
	public void reportDiscard(int playerNum, Card discardedCard) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportDiscard(playerNum, discardedCard);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportDiscard", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * At the end of each turn, this method is called and the player that cannot (or will not) end the round will return a null value.
	 * However, the first player to "knock" (that is, end the round), and then their opponent, will return an ArrayList of ArrayLists of melded cards.
	 * All other cards are counted as "deadwood", unless they can be laid off (added to) the knocking player's melds.
	 * When final melds have been reported for the other player, a player should return their final melds for the round.
	 * @return null if continuing play and opponent hasn't melded, or an ArrayList of ArrayLists of melded cards.
	 */
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		ArrayList<ArrayList<Card>> ret = player.getFinalMelds();
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "getFinalMelds", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
		return ret;
	}

	/**
	 * When an player has ended play and formed melds, the melds (and deadwood) are reported to both players.
	 * @param playerNum player that has revealed melds
	 * @param melds an ArrayList of ArrayLists of melded cards with the last ArrayList (possibly empty) being deadwood.
	 */
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportFinalMelds(playerNum, melds);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportFinalMelds", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * Report current player scores, indexed by 0-based player number.
	 * @param scores current player scores, indexed by 0-based player number
	 */
	public void reportScores(int[] scores) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportScores(scores);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportScores", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * Report layoff actions.
	 * @param playerNum player laying off cards
	 * @param layoffCard card being laid off
	 * @param opponentMeld the opponent meld that card is being added to
	 */
	public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportLayoff(playerNum, layoffCard, opponentMeld);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportLayoff", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

	/**
	 * Report the final hands of players.
	 * @param playerNum player of hand reported
	 * @param hand complete hand of given player
	 */
	public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
		PrintStream out = swapOut(this.out), err = swapErr(this.err);
		long time = monitor.getThreadCpuTime(Thread.currentThread().getId());
		player.reportFinalHand(playerNum, hand);
		time = monitor.getThreadCpuTime(Thread.currentThread().getId()) - time;
		if(writer != null) {
			writer.writeNext(new String[] { "reportFinalHand", Integer.toString(id), Long.toString(time) });
		}
		swapOut(out); swapErr(err);
	}

}
