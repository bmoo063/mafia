import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedList;

import jason.architecture.*;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Message;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class GameGUI extends AgArch {
	
	enum GameState {START, DEBATE, VOTE, MAFIA, END}
	
	GameState state = GameState.START;
	int 		numAgents;
	int 		numMafia;
	Set<String> agents;
	Set<String> mafia;
	Set<String> villagers;
	HashMap<String, Integer>	mvotes;
	HashMap<String, Integer>	vvotes;
	int			mvoteCount;
	int			vvoteCount;
	String		lastDead;
	String		lastLynch;
	boolean		humanMafia;
	boolean		humanAlive;

	JTextArea	guiText;
	JFrame		guiFrame;
	
	JPanel		guiPanelButtons;
	
	JButton		guiBtnNextstate;
	JButton		guiBtnAccuse;
	JButton		guiBtnDefend;
	JButton		guiBtnVote;
	JButton		guiBtnMVote;
	
	int day = 0;
	
	public GameGUI() {
		guiText = new JTextArea(20, 40);
		DefaultCaret c = (DefaultCaret)guiText.getCaret();
		c.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		guiText.setLineWrap(true);
		
		Font f = new Font("Arial", Font.PLAIN, 20);
		guiText.setFont(f);
		
		guiBtnNextstate = new JButton("Next State");
		guiBtnNextstate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manageState();
			}
		});
		guiBtnAccuse	= new JButton("Accuse");
		guiBtnAccuse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				humanAccuse();
			}
		});
		
		guiBtnDefend	= new JButton("Defend");
		guiBtnDefend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				humanDefend();
			}
		});
		
		guiBtnVote		= new JButton("Vote");
		guiBtnVote.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				humanVote();
			}
		});
		
		guiBtnMVote		= new JButton("Mafia Vote");
		guiBtnMVote.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mafiaVote();
			}
		});
		
		guiBtnAccuse.setEnabled(false);
		guiBtnDefend.setEnabled(false);
		guiBtnVote.setEnabled(false);
		guiBtnMVote.setEnabled(false);
		
		guiPanelButtons = new JPanel();
		guiPanelButtons.setLayout(new BoxLayout(guiPanelButtons, BoxLayout.X_AXIS));
		guiPanelButtons.add(guiBtnNextstate);
		guiPanelButtons.add(guiBtnAccuse);
		guiPanelButtons.add(guiBtnDefend);
		guiPanelButtons.add(guiBtnVote);
		guiPanelButtons.add(guiBtnMVote);
		
		guiFrame = new JFrame("Multi-Agent Mafia");
		guiFrame.getContentPane().setLayout(new BorderLayout());
		guiFrame.getContentPane().add(BorderLayout.CENTER, new JScrollPane(guiText));
		guiFrame.getContentPane().add(BorderLayout.SOUTH, guiPanelButtons);
		guiFrame.pack();
		guiFrame.setBounds(100, 50, 1000, 800);
		guiFrame.setVisible(true);
		
		mvotes = new HashMap<String, Integer>();
		vvotes = new HashMap<String, Integer>();
	}
	
	@Override
	public void act(ActionExec action, List<ActionExec> feedback) {
		if (action.getActionTerm().getFunctor().startsWith("display_deny")) {
			guiText.append("[" + action.getActionTerm().getTerm(0) + "] Denies accusation\n");
			action.setResult(true);
			feedback.add(action);
			
		} else if (action.getActionTerm().getFunctor().startsWith("display_accuse")) {
			guiText.append("[" + action.getActionTerm().getTerm(0) + "] I think " + action.getActionTerm().getTerm(1) + " is Mafia!\n");
			action.setResult(true);
			feedback.add(action);
			
		} else if (action.getActionTerm().getFunctor().startsWith("display_vote")) {
			String source = action.getActionTerm().getTerm(0).toString();
			String votee = action.getActionTerm().getTerm(1).toString();
			
			guiText.append("[" + source + "] Votes for " + votee + "\n");
			
			if (vvotes.containsKey(votee)){
				int n = vvotes.get(votee);
				vvotes.put(votee, n+1);
			} else {
				vvotes.put(votee, 1);
			}
			
			vvoteCount++;
			
			if (vvoteCount == numAgents){
				Map.Entry<String, Integer> maxEntry = null;
				for (Map.Entry<String, Integer> entry : vvotes.entrySet()) {
					if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
						maxEntry = entry;
					}
				}
				
				lastLynch = maxEntry.getKey();
				if (lastLynch.equals("human")) humanAlive = false;
				
				guiText.append(lastLynch + " has been chosed by the majority to be lynched...\n");
				
				getRuntimeServices().killAgent(lastLynch, getAgName());
				numAgents--;
				if (mafia.contains(lastLynch)) numMafia--;
				
				Message msg;
				
				try{
					msg = new Message("tell", getAgName(), null,
								ASSyntax.parseLiteral("lynched(" + lastLynch + ")"));
					broadcast(msg);
					sendMsg(new Message("tell", getAgName(), getAgName(), ASSyntax.parseLiteral("lynched(" + lastLynch + ")")));
				} catch (Exception e){
					System.out.println("Failed to broadcast lynch");
				}
			}
			
			action.setResult(true);
			feedback.add(action);
		
		} else if (action.getActionTerm().getFunctor().startsWith("mafia_vote")) {
			String m = action.getActionTerm().getTerm(0).toString();
			String v = action.getActionTerm().getTerm(1).toString();
			
			if (mvotes.containsKey(v)){
				int n = mvotes.get(v);
				mvotes.put(v, n+1);
			} else {
				mvotes.put(v, 1);
			}
			
			mvoteCount++;
			
			if (humanMafia && humanAlive){
				guiText.append("[" + m + "] Votes to kill " + v + "\n");
			}
			
			if (mvoteCount == numMafia){
				Map.Entry<String, Integer> maxEntry = null;
				for (Map.Entry<String, Integer> entry : mvotes.entrySet()) {
					if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
						maxEntry = entry;
					}
				}
				lastDead = maxEntry.getKey();
				if (lastDead.equals("human")) humanAlive = false;
				
				guiText.append(lastDead + " appears to have met his demise, a knife protrudes from his left ear ...\n");

				
				getTS().getUserAgArch().getRuntimeServices().killAgent(lastDead, getTS().getUserAgArch().getAgName());
				numAgents--;
				
				Message msg;
				
				try {
					msg = new Message("tell", getAgName(), null, 
								ASSyntax.parseLiteral("dead(" + lastDead + ")"));
					broadcast(msg);
					sendMsg(new Message("tell", getAgName(), getAgName(), ASSyntax.parseLiteral("dead(" + lastDead + ")")));
				} catch (Exception e) {
					System.out.println("Couldn't parse literal");
				}
			}
			
			action.setResult(true);
			feedback.add(action);
		
		} else {
			super.act(action, feedback);
		}
	}
	
	public void prepareDebate() {
		if (!testWinConditions()){
			agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
			agents.remove("gameController");
			
			guiText.append("\n" + agents.size() + " agents remain :\n[ ");
			
			for (String a : agents){
				guiText.append(a + ", ");
			}
		
			guiText.append("]\n\nAll Townsfolk awaken as dawn breaks...\n");
			Literal goal = ASSyntax.createLiteral("start_debate");
			getTS().getC().addAchvGoal(goal, null);
		}
		
		
		if (humanMafia) guiBtnMVote.setEnabled(false);
		if (humanAlive){
			guiBtnAccuse.setEnabled(true);
			guiBtnDefend.setEnabled(true);
		}
	}

	public void prepareVote() {
		guiText.append("\nThe Townsfolk put it to a vote...\n");
		Literal goal = ASSyntax.createLiteral("start_vote");
		getTS().getC().addAchvGoal(goal, null);
		vvotes.clear();
		vvoteCount = 0;
		
		guiBtnAccuse.setEnabled(false);
		guiBtnDefend.setEnabled(false);
		
		if (humanAlive) guiBtnVote.setEnabled(true);
	}
	
	public void prepareMafia(){
		if (!testWinConditions()){
			guiText.append("\nThe Mafia meet during the night...\n");
			Literal goal = ASSyntax.createLiteral("start_mafia");
			getTS().getC().addAchvGoal(goal, null);
			mvotes.clear();
			mvoteCount = 0;
		}
		
		guiBtnVote.setEnabled(false);
		
		if (humanMafia && humanAlive) guiBtnMVote.setEnabled(true);
	}
	
	public void manageState() {
		GameState cur = state;
		
		switch(cur){
			case START:
				for (String m : mafia){
					try{
						sendMsg(new Message("tell", getAgName(), "gameController", ASSyntax.parseLiteral("mafia(" + m + ")")));
					} catch (Exception e) {
						System.out.println("Couldn't send '" + m + "' to game controller");
					}
				}
			case MAFIA:
				state = GameState.DEBATE;
				//System.out.println("Starting debate ...");
				prepareDebate();
				break;
			
			case DEBATE:
				state = GameState.VOTE;
				//System.out.println("Starting vote ...");
				prepareVote();
				break;
				
			case VOTE:
				state = GameState.MAFIA;
				//System.out.println("Starting Mafia discussion ...");
				prepareMafia();
				break;
				
			case END:
				System.out.println("The game is over now, you can go home");
				guiText.append("\nThe game is over now, you can go home");
				
			default:
				break;
		}
	}
	
	public void humanAccuse() {
		agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
		agents.remove("gameController");
		
		String[] possiblities = agents.toArray(new String[agents.size()]);
		Arrays.sort(possiblities);
		
		String a = (String)JOptionPane.showInputDialog(
						guiFrame,
						"Choose an agent to accuse",
						"Accusing",
						JOptionPane.PLAIN_MESSAGE,
						null,
						possiblities,
						possiblities[0]);
						
		if (a != null ) {
			Message msgs;
			Message msgb;
			try{
				msgb = new Message("tell", "human", null, ASSyntax.parseLiteral("accuse(" + a + ")"));
				broadcast(msgb);
				
				msgs = new Message("tell", "human", "gameController", ASSyntax.parseLiteral("accuse(" + a + ")"));
				sendMsg(msgs);
			} catch (Exception e){
				System.out.println("Failed to broadcast accusation");
			}
		}
	}
	
	public void humanDefend() {
		Message msgb;
		Message msgs;
		try{
			msgb = new Message("tell", "human", null, ASSyntax.parseLiteral("deny"));
			broadcast(msgb);
			
			msgs = new Message("tell", "human", "gameController", ASSyntax.parseLiteral("deny"));
			sendMsg(msgs);
		} catch (Exception e){
			System.out.println("Failed to send deny");
		}
	}
	
	public void humanVote() {
		agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
		agents.remove("gameController");
		
		String[] possiblities = agents.toArray(new String[agents.size()]);
		Arrays.sort(possiblities);
		
		String a = (String)JOptionPane.showInputDialog(
						guiFrame,
						"Choose an agent to vote to be lynched",
						"Voting",
						JOptionPane.PLAIN_MESSAGE,
						null,
						possiblities,
						possiblities[0]);
						
		if (a != null ) {
			Message msg;
			try{
				msg = new Message("tell", "human", "gameController", ASSyntax.parseLiteral("vote(" + a + ")"));
				sendMsg(msg);
			} catch (Exception e){
				System.out.println("Failed to send human vote");
			}
		}
	}
	
	public void mafiaVote() {
		agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
		agents.remove("gameController");
		
		String[] possiblities = agents.toArray(new String[agents.size()]);
		Arrays.sort(possiblities);
		
		String a = (String)JOptionPane.showInputDialog(
						guiFrame,
						"Choose an agent for mafia to kill",
						"Voting",
						JOptionPane.PLAIN_MESSAGE,
						null,
						possiblities,
						possiblities[0]);
						
		if (a != null ) {
			Message msg;
			try{
				msg = new Message("tell", "human", "gameController", ASSyntax.parseLiteral("mvote(" + a + ")"));
				sendMsg(msg);
			} catch (Exception e){
				System.out.println("Failed to send mafia vote");
			}
		}
	}
	
	@Override
	public void init() throws Exception {
		agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
		
		numAgents = agents.size();
		numMafia = (int)Math.floor(Math.sqrt(numAgents));
		
		System.out.println("Loaded " + numAgents + " agents:");
		for (String a : agents){
			System.out.println("\t" + a);
		}
		
		List<String> l = new LinkedList<String>(agents);
		Collections.shuffle(l);
		mafia = new HashSet<String>(l.subList(0, numMafia));
		
		System.out.println(numMafia + " chosen as mafia:");
		for (String m : mafia){
			System.out.println("\t" + m);
		}
		
		for (String a : agents){
			if (mafia.contains(a)){
				for (String m : mafia){
					sendMsg(new Message("tell", getAgName(), a, ASSyntax.parseLiteral("mafia(" + m + ")")));
				}
			} else sendMsg(new Message("tell", getAgName(), a, ASSyntax.parseLiteral("villager(" + a + ")")));
		}
		
		if (mafia.contains("human")){
			humanMafia = true;
			guiText.append("You have been assigned the role of a Mafia\n\n");
			guiText.append("The mafia are:\n");
			for (String m : mafia){
				guiText.append("\t" + m + "\n");
			}
		} else {
			humanMafia = false;
			guiText.append("You have been assigned the role of a Villager\n");
		}
		humanAlive = true;
	}
	
	public boolean testWinConditions(){
		agents = new HashSet<String>(getRuntimeServices().getAgentsNames());
		agents.remove("gameController");
		
		Set<String> villagers = new HashSet<String>(agents);
		villagers.removeAll(mafia);
		
		Set<String> remainingMafia = new HashSet<String>(agents);
		remainingMafia.removeAll(villagers);
		
		int numV = villagers.size();
		int numM = remainingMafia.size();
		
		System.out.printf("%d villagers and %d mafia remain\n", numV, numM);
		
		if (numM == 0) {
			System.out.println("All Mafia are dead, Villagers win!");
			guiText.append("\nAll Mafia are dead, Villagers win!");
			state = GameState.END;
			return true;
		} else if (numM > numV && state == GameState.DEBATE) {
			System.out.println("Mafia outnumber the Villagers, Mafia win!");
			guiText.append("\nMafia outnumber the Villagers, Mafia win!");
			state = GameState.END;
			return true;
		} else if (numM >= numV && state == GameState.MAFIA) {
			System.out.println("Mafia outnumber the Villagers, Mafia win!");
			guiText.append("\nMafia outnumber the Villagers, Mafia win!");
			state = GameState.END;
			return true;
		} else return false;
	}
	
	@Override
	public void stop() {
		guiFrame.dispose();
		super.stop();
	}
}
