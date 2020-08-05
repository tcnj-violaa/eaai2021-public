import ginrummy.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
//import java.


/**
 * Modified from SimplePlayer--tracks probable locations of cards throughout play. 
 * Currently, no special decisions are made and act exactly according to the below description.
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
public class CardProbHeuristicPlayer implements GinRummyPlayer {
	protected int playerNum;
	@SuppressWarnings("unused")
	protected int startingPlayerNum;
	protected ArrayList<Card> cards = new ArrayList<Card>();
	protected Random random = new Random();
	protected boolean opponentKnocked = false;
	protected final int NUM_LOCS = 4;
	protected final int MAX_HAND = 10;
	Card dummyCard;
	Card faceUpCard, drawnCard;
	ArrayList<Long> drawDiscardBitstrings = new ArrayList<Long>();

	//An array of probabilities of cards being in one of the four possible locations
	//card_probs[0] = our hand; card_probs[1] = opponent's hand; card_probs[2] = deck; card_probs[3] = discard
	protected double[][] card_probs = new double[4][52];
	protected static final int OUR_HAND = 0, THEIR_HAND = 1, DECK = 2, DISCARD_PILE = 3;

	@Override
	public void startGame(int playerNum, int startingPlayerNum, Card[] cards) {
		this.playerNum = playerNum;
		this.startingPlayerNum = startingPlayerNum;
		this.cards.clear();
		probsInit();
		ArrayList<Integer> tempHand = new ArrayList<Integer>();


		for (Card card : cards){
			this.cards.add(card);
			tempHand.add((Integer)card.getId());
			findCard(card, OUR_HAND); //These cards are in our hand...
		}

		for (int i = 0; i < 52; i++){
			if(tempHand.contains((Integer)i)){
				continue;
			}

			else{
				cardNotIn(i, OUR_HAND);
			}
		}

		//printProbs();
		opponentKnocked = false;
		drawDiscardBitstrings.clear();

	}

	@Override
	public boolean willDrawFaceUpCard(Card card) {
		// Return true if the card is a meld -and- will reduce the amount of deadwood
		// Should keep good melds from being destroyed, I think
		this.faceUpCard = card;
		findCard(card, DISCARD_PILE);
		@SuppressWarnings("unchecked")
		ArrayList<Card> newCards = (ArrayList<Card>) cards.clone();
		newCards.add(card);
		ArrayList<ArrayList<ArrayList<Card>>> orig_best_melds = GinRummyUtil.cardsToBestMeldSets(cards);
		ArrayList<ArrayList<ArrayList<Card>>> new_best_melds = GinRummyUtil.cardsToBestMeldSets(newCards);

		int orig_deadwood = GinRummyUtil.getDeadwoodPoints(cards); //In case there are no melds
		for (ArrayList<ArrayList<Card>> meld_set : orig_best_melds){
			orig_deadwood = GinRummyUtil.getDeadwoodPoints(meld_set, cards);
		}

		int potent_deadwood = GinRummyUtil.getDeadwoodPoints(newCards); //In case there are no melds...
		for (ArrayList<ArrayList<Card>> meld_set : new_best_melds){
			potent_deadwood = GinRummyUtil.getDeadwoodPoints(meld_set, newCards);
		}

		//System.out.println("orig_deadwood: " + orig_deadwood + " | potent_deadwood: " + potent_deadwood);
		for (ArrayList<Card> meld : GinRummyUtil.cardsToAllMelds(newCards)){
			//System.out.println("contains : " + meld.contains(card));
			if (meld.contains(card) && potent_deadwood <= orig_deadwood){
				//System.out.println("Drawing face-up...");
				return true;
			}
		}
		return false;
	}

	@Override
	public void reportDraw(int playerNum, Card drawnCard) {
		// Ignore other player draws.  Add to cards if playerNum is this player.
		if (playerNum == this.playerNum) {
			cards.add(drawnCard);
			this.drawnCard = drawnCard;
			findCard(drawnCard, OUR_HAND);
		}

		else{
			findCard(drawnCard, THEIR_HAND);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Card getDiscard() {
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
	}

	@Override
	public void reportDiscard(int playerNum, Card discardedCard) {
		// Ignore other player discards.  Remove from cards if playerNum is this player.
		if (playerNum == this.playerNum)
			cards.remove(discardedCard);

		findCard(discardedCard, DISCARD_PILE);
	}

	@Override
	public ArrayList<ArrayList<Card>> getFinalMelds() {
		// Check if deadwood of maximal meld is low enough to go out.
		ArrayList<ArrayList<ArrayList<Card>>> bestMeldSets = GinRummyUtil.cardsToBestMeldSets(cards);

		int ourDeadwood = bestMeldSets.isEmpty() ? GinRummyUtil.getDeadwoodPoints(cards) : GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards);

		//Guess what the opponent's deadwood might be
		ArrayList<Card> oppHand = inferOppHand();
		ArrayList<ArrayList<ArrayList<Card>>> oppMelds = GinRummyUtil.cardsToBestMeldSets(oppHand);
		int potentialOppDeadwood = oppMelds.isEmpty() ? GinRummyUtil.getDeadwoodPoints(oppHand) : 
			GinRummyUtil.getDeadwoodPoints(oppMelds.get(0), oppHand);
		
		//If our deadwood is probably less than the opponent's, then knock.
		//If it's unlikely, then don't to avoid being undercut and losing points.
		//If we have gin, then definitely knock.
	
		if (!opponentKnocked && (bestMeldSets.isEmpty() || GinRummyUtil.getDeadwoodPoints(bestMeldSets.get(0), cards) > GinRummyUtil.MAX_DEADWOOD || ourDeadwood > potentialOppDeadwood))
			return null;

				

		//System.out.println("Knocking!");
		return bestMeldSets.isEmpty() ? new ArrayList<ArrayList<Card>>() : bestMeldSets.get(random.nextInt(bestMeldSets.size()));

	}

	@Override
	public void reportFinalMelds(int playerNum, ArrayList<ArrayList<Card>> melds) {
		// Melds ignored by simple player, but could affect which melds to make for complex player.
		int hand = OUR_HAND;
		if (playerNum != this.playerNum){
			opponentKnocked = true;
			hand = THEIR_HAND;
		}

		for (ArrayList<Card> meld : melds) {
			for (Card card : meld) {
				findCard(card, hand);
			}
		}
	}

	@Override
	public void reportScores(int[] scores) {
		// Ignored by simple player, but could affect strategy of more complex player.
	}

	@Override
	public void reportLayoff(int playerNum, Card layoffCard, ArrayList<Card> opponentMeld) {
		// Ignored by simple player, but could affect strategy of more complex player.

	}

	@Override
	public void reportFinalHand(int playerNum, ArrayList<Card> hand) {
		// Ignored by simple player, but could affect strategy of more complex player.
	}

	private void printProbs(){
		System.out.println("Calling printprobs");
		for (int loc = 0 ; loc < NUM_LOCS; loc++){
			System.out.println(loc);
			//System.out.println(card_probs[loc].toString());
			for (int card = 0; card < 52; card++){
				System.out.print(card_probs[loc][card] + " ");
			}
			System.out.println(" ");
		}
	}

	//If we see a card definitely, set probabilities accordingly
	private void findCard(int cardIdx, int foundLocation){
		for (int loc = 0; loc < NUM_LOCS; loc++){
			if(loc == foundLocation){
				card_probs[loc][cardIdx] = 1.0;
			}
			else{
				card_probs[loc][cardIdx] = 0.0;
			}
		}
	}

	//Overload of above
	private void findCard(Card card, int foundLocation){
		if (card == null) { return; }
		int cardIdx = card.getId();
		//System.out.println("Got cardIdx");
		for (int loc = 0; loc < NUM_LOCS; loc++){
			if(loc == foundLocation){
				card_probs[loc][cardIdx] = 1.0;
			}
			else{
				card_probs[loc][cardIdx] = 0.0;
			}
		}

		//printProbs();
	}

	//If we can determine a card is not in a particular location, but not necessarily where
	//a card definitely is, use this...
	//Mostly useful when we're dealt our cards and know that the other 42 aren't in our hand
	private void cardNotIn(int cardIdx, int notLocation){
		card_probs[notLocation][cardIdx] = 0.0;

		//Find how many other locations a card isn't--useful for adjusting
		//our probabilities.
		int num_not_in = 0;
		for (int loc = 0; loc < NUM_LOCS; loc++){
			if(card_probs[loc][cardIdx] == 0.0)
				num_not_in++;
		}

		double denominator = NUM_LOCS-num_not_in;

		//For the places it isn't, adjust probabilities
		for (int loc = 0; loc < NUM_LOCS; loc++){
			if(card_probs[loc][cardIdx] < 1.0 && card_probs[loc][cardIdx] > 0.0)
				card_probs[loc][cardIdx] = 1.0/denominator;
		}

		//printProbs();
	}

	//Over the course of face-up draws and discards we can infer what the opponent's hand is.
	//The more face-up draws they do, the more we know exactly what their hand is. 
	//Returns a list of known cards -- those with "1.0" probability in the probs map
	private ArrayList<Card> getKnownOpp(){
		ArrayList<Card> tempOpp = new ArrayList<Card>();
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for (int card = 0; card < 52; card++){
			if (card_probs[THEIR_HAND][card] == 1.0){
				temp.add(card);
			}
		}

		for (Integer card : temp) {
			tempOpp.add(Card.getCard((int)card));
		}

		return tempOpp;
	}

	private ArrayList<Card> inferOppHand(){
		ArrayList<Card> tempOpp = getKnownOpp();
		ArrayList<Integer> guessTemp = new ArrayList<Integer>();
		int handLength = tempOpp.size();

		/* WIP code for guessing what cards they may have in melds based on 
		 * what isn't in a meld of the cards of theirs we know--assuming that 
		 * a player would avoid having high rank cards (like qkj) if they don't have
		 * a meld containing them...
		ArrayList<ArrayList<ArrayList<Card>>> probMeldSets = cardsToBestMeldSets(tempOpp);
		int pick_rand = random.NextInt() % probMeldSets.size();

		ArrayList<ArrayList<Card>> probMelds = probMeldSets.get(pick_rand);
		ArrayList<ArrayList<Card>> probMelds = probMeldSets.size() > 0 ? probMeldSets.get(pick_rand) : null;

		int knownDeadwood;

		if(probMelds != null){
			knownDeadwood = GinRummyUtil.getDeadwoodPoints(probMelds, tempOpp);
		}
		else{
			knownDeadwood = GinRummyUtil.getDeadwoodPoints(tempOpp);
		}
		*/

		
		

		//Finally, guess what the remaining cards might be otherwise...
		//System.out.println("Guessing " + (10-handLength) + " remaining cards in the opponent's hand...");
		
		//Get a list of likely cards: find the highest probability
		double maxProb = 0.0;
		for(int card = 0; card < 52; card++){
			if(card_probs[THEIR_HAND][card] > maxProb)
				maxProb = card_probs[THEIR_HAND][card];
		}

		//
		//Add these likely cards to a list...
		for(int card = 0; card < 52; card++){
			if(card_probs[THEIR_HAND][card] >= maxProb)
				guessTemp.add(card);
		}

		//Fill in the remainder of the hand -- "guess" what the cards might be
		//by picking random cards from the above list until we reach a full hand.
		while(tempOpp.size() <10 && guessTemp.size() > 0){
			int pick_rand = random.nextInt(guessTemp.size());
			int cardId = guessTemp.get(pick_rand);
			tempOpp.add(Card.getCard(cardId));
			guessTemp.remove(pick_rand);
		}

		//Potential errors: if we have only a couple high probability options and run out,
		//then our max probability might prevent us from filling the remaining cards with
		//the next highest probability
		//System.out.println("guessed hand size: " + tempOpp.size());
		return tempOpp;
	}

	private void probsInit(){
		for (int loc = 0; loc < NUM_LOCS; loc++){
			Arrays.fill(card_probs[loc], 1.0/NUM_LOCS);
		}
	}


}
