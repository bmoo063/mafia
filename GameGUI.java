import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
	
	enum GameState {START, DEBATE, VOTE, MAFIA}
	
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

	JTextArea	guiText;
	JFrame		guiFrame;
	
	JPanel		guiPanelButtons;
	
	JButton		guiBtnNextstate;
	JButton		guiBtnAccuse;
	JButton		guiBtnDefend;
	
	int day = 0;
	
	public GameGUI() {
		guiText = new JTextArea(20, 40);
		DefaultCaret c = (DefaultCaret)guiText.getCaret();
		c.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		guiText.setLineWrap(true);
		
		guiBtnNextstate = new JButton("Next State");
		guiBtnNextstate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manageState();
			}
		});
		guiBtnAccuse	= new JButton("Accuse");
		guiBtnDefend	= new JButton("Defend");
		
		guiPanelButtons = new JPanel();
		guiPanelButtons.setLayout(new BoxLayout(guiPanelButtons, BoxLayout.X_AXIS));
		guiPanelButtons.add(guiBtnNextstate);
		guiPanelButtons.add(guiBtnAccuse);
		guiPanelButtons.add(guiBtnDefend);
		
		guiFrame = new JFrame("Multi-Agent Mafia");
		guiFrame.getContentPane().setLayout(new BorderLayout());
		guiFrame.getContentPane().add(BorderLayout.CENTER, new JScrollPane(guiText));
		guiFrame.getContentPane().add(BorderLayout.SOUTH, guiPanelButtons);
		guiFrame.pack();
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
			
			if (mvoteCount == numMafia){
				Map.Entry<String, Integer> maxEntry = null;
				for (Map.Entry<String, Integer> entry : mvotes.entrySet()) {
					if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
						maxEntry = entry;
					}
				}
				lastDead = maxEntry.getKey();

				
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
		if (lastDead != null && !lastDead.isEmpty()){
			guiText.append(lastDead + " appears to have met his demise, a knife protrudes from his left ear ...\n");
		}
		
		guiText.append("All Townsfolk awaken as dawn breaks...\n");
		
		Literal goal = ASSyntax.createLiteral("start_debate");
		getTS().getC().addAchvGoal(goal, null);
	}

	public void prepareVote() {
		guiText.append("The Townsfolk put it to a vote...\n");
		Literal goal = ASSyntax.createLiteral("start_vote");
		getTS().getC().addAchvGoal(goal, null);
		vvotes.clear();
		vvoteCount = 0;
	}
	
	public void prepareMafia(){
		if (lastLynch != null && !lastLynch.isEmpty()){
			guiText.append(lastLynch + " has been chosed by the majority to be lynched...\n");
		}
		
		guiText.append("The Mafia meet during the night...\n");
		Literal goal = ASSyntax.createLiteral("start_mafia");
		getTS().getC().addAchvGoal(goal, null);
		mvotes.clear();
		mvoteCount = 0;
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
				
			default:
				break;
		}
	}
	
	
	@Override
	public void init() throws Exception {
		agents = getRuntimeServices().getAgentsNames();
		
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
	}
	
	@Override
	public void stop() {
		guiFrame.dispose();
		super.stop();
	}
}
