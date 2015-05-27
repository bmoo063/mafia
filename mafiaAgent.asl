+debating : true
	<-	.broadcast(tell, accuse(agentx)).
		
+voting : true
	<-	.send(gameController, tell, vote(agentx)).
		
+mafiaDiscussing : ismafia
	<-	.send(gameController, tell, vote(agentx)).
