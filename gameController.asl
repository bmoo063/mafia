+!start_debate
	<- 	.broadcast(tell, debating);
		.print("All townsfolk awaken as dawn breaks...");
		.broadcast(untell, mafiaDiscussing).
	
+!start_vote
	<- 	.broadcast(tell, voting);
		.print("The townsfolk put it to a vote...");
		.broadcast(untell, debating).
	
+!start_mafia
	<- 	.broadcast(tell, mafiaDiscussing);
		.print("The Mafia meet during the night...");
		.broadcast(untell, voting).
	
+deny[source(S)] : true
	<- 	display_deny(S);
		.abolish(deny).
	
+accuse(A)[source(S)] : true
	<- 	display_accuse(S, A);
		.abolish(accuse(A)).
	
+vote(A)[source(S)] : true
	<- 	display_vote(S, A);
		.abolish(vote(A)).
