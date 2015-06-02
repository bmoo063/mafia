+!start_debate
	<- 	.broadcast(tell, debating);
		.print("All townsfolk awaken as dawn breaks...");
		.broadcast(untell, mafiaDiscussing);
		.all_names(Y);
		.delete(gameController, Y, N);
		.findall(X, mafia(X) & not dead(X), M);
		.difference(N, M, V);
		.length(M, LM);
		.length(V, LV);
		.print(LM, " Mafia: ", M);
		.print(LV, " Villagers: ", V).
	
+!start_vote
	<- 	.broadcast(tell, voting);
		.print("The townsfolk put it to a vote...");
		.broadcast(untell, debating).
	
+!start_mafia
	<- 	.broadcast(tell, mafiaDiscussing);
		.print("The Mafia meet during the night...");
		.broadcast(untell, voting).

+dead(A) : not lynched(A)
	<-	.print(A, " has been killed by the mafia").
	
+lynched(A) : true
	<-	+dead(A);
		.print(A, " has been lynched by the village").
	
+deny[source(S)] : true
	<- 	display_deny(S);
		.abolish(deny).
	
+accuse(A)[source(S)] : true
	<- 	display_accuse(S, A);
		.abolish(accuse(A)).
	
+vote(A)[source(S)] : true
	<- 	display_vote(S, A);
		.abolish(vote(A)).		
		
+mvote(A)[source(S)] : true
	<-	mafia_vote(S, A);
		.abolish(mvote(A)[source(S)]).
