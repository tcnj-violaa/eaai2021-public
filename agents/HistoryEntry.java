import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
//import java.lang.StringBuilder;

public class HistoryEntry{
	private long hand_bitstring;
	private char last_action_type;
	private String last_action_card;
	private int last_player;
	private String tag; //A string concatenation representation of all of these


	public HistoryEntry(long bits, char act, String card, int player){
		this.hand_bitstring = bits;
		this.last_action_type = act;
		this.last_action_card = card;
		this.last_player = player;

		StringBuilder cat = new StringBuilder();
		cat.append(bits);
		cat.append(act);
		cat.append(card);
		cat.append(player);

		this.tag = cat.toString();
	}

	public long getLastHand(){
		return hand_bitstring;
	}

	public char getLastAction(){
		return last_action_type;
	}

	public String getLastActionCard(){
		return last_action_card;
	}

	public int getLastPlayer(){
		return last_player;
	}

	public String getTag(){
		return tag;
	}
}
