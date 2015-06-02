// Placeholder reactions for non implemented logic
	
+deny[source(S)] : true
	<-	-deny[source(S)].
	

////////////////////////////////////////////////////////////////////////////////
// Rules for reacting to new accusations

+accuse(M)[source(S)] : .my_name(M) & mafia(M)
	<-	-accuse(M)[source(S)];
		+accused(S, M);
		+is_target(S).
	
+accuse(M)[source(S)] : .my_name(M) & villager(M)
	<-	-accuse(M)[source(S)];
		+accused(S, M);
		.broadcast(tell, deny).
	
+accuse(A)[source(S)] : true
	<-	-accuse(A)[source(S)];
		+accused(S, A).
	
	
////////////////////////////////////////////////////////////////////////////////
// Rules for debating
	
// Villager
+debating : .my_name(M) & villager(M) & is_mafia(A)
	<-	.broadcast(tell, accuse(A)).
	
+debating : .my_name(M) & villager(M) & accused(S, A) & dead(S)
	<-	+is_mafia(A);
		.broadcast(tell, accuse(A)).
		

+debating : .my_name(M) & villager(M) & not is_mafia(_)
	<-	.all_names(N);
		.delete(M, N, X);
		.delete(gameController, X, Y);
		.shuffle(Y, S);
		.nth(0, S, A);
		+is_mafia(A).
		
+is_mafia(A) : debating & .my_name(M) & villager(M)
	<-	.broadcast(tell, accuse(A)).
	

// Mafia
	
	
////////////////////////////////////////////////////////////////////////////////
// Rules for voting

// Villager

+voting : .my_name(M) & villager(M) & is_mafia(A) & not dead(A)
	<-	.send(gameController, tell, vote(A)).
	
+voting : .my_name(M) & villager(M)
	<-	.all_names(N);
		.delete(gameController, N, X);
		.delete(M, X, Y);
		.shuffle(Y, S);
		.nth(0, S, A);
		.send(gameController, tell, vote(A)).
	
		
// Mafia
		
+voting : .my_name(M) & mafia(M)
	<-	.findall(X, mafia(X), L);
		.all_names(N);
		.difference(N, L, Y);
		.delete(gameController, Y, V);
		.shuffle(V, S);
		.nth(0, S, A);
		.send(gameController, tell, vote(A)).
	
////////////////////////////////////////////////////////////////////////////////
// Rules for mafia deciding

+mafiaDiscussing : .my_name(M) & mafia(M) & is_target(A) & not dead(A)
	<-	.print(A, " is a target");
		.send(gameController, tell, mvote(A)).
	
+mafiaDiscussing : .my_name(M) & mafia(M) & .findall(T, is_target(T), G)
					& .length(G, 0)
	<-	.findall(X, mafia(X), L);
		.all_names(N);
		.difference(N, L, Y);
		.delete(gameController, Y, V);
		.shuffle(V, S);
		.nth(0, S, A);
		.print("Chooses ", A, " randomly");
		.send(gameController, tell, mvote(A)).
		

////////////////////////////////////////////////////////////////////////////////
// Helper rules

+dead(A) : true
	<-	-is_target(A).
	
+lynched(A) : true
	<-	+dead(A).
