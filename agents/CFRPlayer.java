import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
import StateNode;
import HistoryEntry;


/**
 * Implements a random dummy Gin Rummy player that has the following trivial, poor play policy: 
 * Ignore opponent actions and cards no longer in play.
 * Draw face up card only if it becomes part of a meld.  Draw face down card otherwise.
 * Discard a highest ranking unmelded card without regard to breaking up pairs, etc.
 * Knock as early as possible.
 * 
 * @author Todd W. Neller
 * @version 1.0

Copyright (C) 2020 Todd Neller

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Information about the GNU General Public License is available online at:
  http://www.gnu.org/licenses/
To receive a copy of the GNU General Public License, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
02111-1307, USA.

 */

//Many comments are notes/thoughts, not definite nor guaranteed correct
public class CFRPlayer implements GinRummyPlayer {
	private int playerNum;
	@SuppressWarnings("unused")
	private int startingPlayerNum;
	private ArrayList<Card> cards = new ArrayList<Card>();
	private Random random = new Random();
	private boolean opponentKnocked = false;
	private boolean train = false;
	private TreeMap<String,StateNode> stateNodeMap = new TreeMap<String, StateNode>; //change from string probably. We're gonn have more complicated history
	private ArrayList<HistoryEntry> history = new ArrayList<HistoryEntry>;
	private final num_actions = 55;
	private int[] actions = new int[num_actions]
	//actions: 0 = draw face-up; 1 = draw from discard; 2 = knock ; 3 = don't knock ; 4-55 discard cards based on int index
	//definitely need some sort of trimming logic for this, so cfr only explores tree as necessary--could cut down size significantly
	//on draw, traverse only actions 0 and 1; when we can knock, traverse 2 and 3
	//on discard, traverse only cards in hand--"try" discarding them...

	//We probably want this player to be serializable so we can save the class and don't have to 
	//retrain it every single time
	
	Card faceUpCard, drawnCard; 
	ArrayList<Long> drawDiscardBitstrings = new ArrayList<Long>();
	
	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		this.playerNum = playerNum;
		this.startingPlayerNum = startingPlayerNum;
		this.cards.clear();
		for (Card card : cards)
			this.cards.add(card);
		
		history.removeAll(); //Clear history
		long card_bits = GinRummyUtil.cardsToBitstring(this.cards);

		history.add(new HistoryEntry(card_bits, 'i', "", this.playerNum))
		opponentKnocked = false;
		drawDiscardBitstrings.clear();
	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		//poll from strategy to decide if we'll draw...	
		// Return true if card would be a part of a meld, false otherwise.
		this.faceUpCard = card;
		@SuppressWarnings("unchecked")
		ArrayList<Card> newCards = (ArrayList<Card>) cards.clone();
		//If we draw, add that action to history
		//If we draw, add the new card to our hand/info set
		newCards.add(card);
		for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(newCards))
			if (meld.contains(card))
				return true;
		return false;

		//work-in-progress cfr-based logic...
		//cards_string and turn_history currently dummy variables, won't compile

		String info = getFullHistory();
		int draw_action = getAction(info);
		boolean face_up = draw_action == 0 ? true : false;

		return face_up;
		
	}

	@Override
	public void reportDraw(int playerNum, Card drawnCard) {
		// Add to history if other player draws
		// Ignore other player draws.  Add to cards if playerNum is this player.
		long card_bits = 0;
		if (playerNum == this.playerNum) {
			cards.add(drawnCard);
			this.drawnCard = drawnCard;

			card_bits = GinRummyUtil.cardsToBitstring(cards);
		}

		//Add event to history... if card_bits is 0, then it's the opponent's play
		String drawn_card = drawnCard.toString();
		history.add(new HistoryEntry(card_bits,'d',drawn_card,playernum));

	}

	@SuppressWarnings("unchecked")
	@Override
	public Card getDiscard() {
		// Add discard to history
		// Discard a random card (not just drawn face up) leaving minimal deadwood points.
		int minDeadwood = Integer.MAX_VALUE;
		ArrayList<Card> candidateCards = new ArrayList<Card>();
		for (Card card : cards) {
			// Cannot draw and discard face up card.
			if (card == drawnCard && drawnCard == faceUpCard)
				continue;
			// Disallow repeat of draw and discard.
			ArrayList<Card> drawDiscard = new ArrayList<Card>();
			drawDiscard.add(drawnCard);
			drawDiscard.add(card);
			if (drawDiscardBitstrings.contains(GinRummyUtil.cardsToBitstring(drawDiscard)))
				continue;
			
			ArrayList<Card> remainingCards = (ArrayList<Card>) cards.clone();
			remainingCards.remove(card);
			ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(remainingCards);
			int deadwood = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(remainingCards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), remainingCards);
			if (deadwood <= minDeadwood) {
				if (deadwood < minDeadwood) {
					minDeadwood = deadwood;
					candidateCards.clear();
				}
				candidateCards.add(card);
			}
		}
		Card discard = candidateCards.get(random.nextInt(candidateCards.size()));
		// Prevent future repeat of draw, discard pair.
		ArrayList<Card> drawDiscard = new ArrayList<Card>();
		drawDiscard.add(drawnCard);
		drawDiscard.add(discard);
		drawDiscardBitstrings.add(GinRummyUtil.cardsToBitstring(drawDiscard));
		return discard;

		//Draft code, won't compile; conflicts with above code
		String info = getFullHistory();
		int discard_action = getAction(info);
		Card discard = getCard(discard_action-3)
		//boolean face_up = draw_action == 0 ? true : false;

		return discard;

	}

	@Override
	public void reportDiscard(int playerNum, Card discardedCard) {
		// Add other player discard to history...
		// Ignore other player discards.  Remove from cards if playerNum is this player.
		long card_bits = 0;
		if (playerNum == this.playerNum){
			cards.remove(discardedCard);
			card_bits = GinRummyUtil.cardsToBitstring(cards);
		}

		String discard = discardedCard.toString();
		history.add(new HistoryEntry(card_bits, 'r', discard, playernum));
	}

	@Override
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		// Poll strategy to see if we might knock, given valid cards/deadwood
		// Check if deadwood of maximal meld is low enough to go out. 
		ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(cards);
		if (!opponentKnocked && (bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD))
			return null;
		return bestMeldSets.isEmpty() ? new ArrayList<ArrayList<Card>>() : bestMeldSets.get(random.nextInt(bestMeldSets.size()));

		//WIP code - depending on implementation may need checking for validity of knocking...
		String info = getFullHistory();
		int knock_action = getAction(info);
		boolean knock = knock_action == 2 ? true : false;
		return knock;
	
	}

	@Override
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		// Add to history?
		// Melds ignored by simple player, but could affect which melds to make for complex player.
		long card_bits = 0;
		if (playerNum != this.playerNum){
			opponentKnocked = true;
			card_bits = GinRummyUtil.cardsToBitstring(cards);
		}

		//Is this logic sufficient? Will store two 'knock' actions
		history.add(new HistoryEntry(card_bits, 'k', "", playerNum);
	}

	@Override
	public void reportScores(int[] scores) {
		// Ignored by simple player, but could affect strategy of more complex player.
		// use this for regret, maybe?
	}

	@Override
	public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
		// Ignored by simple player, but could affect strategy of more complex player.
		
	}

	@Override
	public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
		// Ignored by simple player, but could affect strategy of more complex player.		
	}

	private int getAction(String state_node){
		Node node = nodeMap.get(state_node);

		//Currently just get player 1's strategy--we're only maintaining a 
		//tree for this player anyway? Maybe change if there are bugs
		double[] cur_strat = node.getStrategy(0);

		double r = random.nextDouble();
		int action=0;
		double cumulativeProbability=0;

		while (action < NUM_ACTIONS - 1){
			cumulativeProbability += cur_strat[action];
			if (r < cumulativeProbability)
				break;
			action++;
		}

		return action;
	}

	private String getFullHistory(){
		StringBuilder h = new StringBuilder();
		for (int i = 0; i < history.size()-1; i++){
			h.append(history[i].getTag())
		}
		
		String strhist = h.toString();
		return strhist;
	}
		
	
}
