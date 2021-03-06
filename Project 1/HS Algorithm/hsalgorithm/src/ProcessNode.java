/**
* @author Subhasis Dutta, Ram Hariesh,Vibin Daniel
* @email-id sxd150830@utdallas.edu,rxc142330@utdallas.edu,vxd141730@utdallas.edu,
* @version 1.0
* 
* Program to simulate SynchGHS algorithm for leader election in Synchronous general networks
*/

public class ProcessNode extends Thread {
	
	private NodeData nodeData;
	private int phaseNo;
	private boolean leaderPrinted=false;
	
	public ProcessNode() {
		//Nothing
	}
	
	ProcessNode(NodeData nodeData){		
		this.phaseNo=0;
		this.nodeData=nodeData;
	}
	
	
	/**
	 * Need to implement the HS Algorithm in this.
	 */
	@Override
	public void run(){
		
		//Loop until the leader is elected and all are aware of the leader
		while(true){
			//Message Generation function applied to current state or NodeData
			//send the current value of send+ to process i + 1
			SharedData.sendMessage(SharedData.roundNo+1, this.nodeData.getUID(), this.nodeData.getPlus_UID(), this.nodeData.getSend_plus_message());
			
			//send the current value of send- to process i - 1
			SharedData.sendMessage(SharedData.roundNo+1, this.nodeData.getUID(), this.nodeData.getMinus_UID(), this.nodeData.getSend_minus_message());
			
			
			//Generate message to send to neighbors
			//send+ := null
			this.nodeData.setSend_plus_message(null);
			//send- := null
			this.nodeData.setSend_minus_message(null);
							
			//receive message and apply to current data to create new data
			Message receivedMessage_minus=SharedData.receiveMessage(SharedData.roundNo, this.nodeData.getMinus_UID(), this.nodeData.getUID());			
			Message receivedMessage_plus=SharedData.receiveMessage(SharedData.roundNo, this.nodeData.getPlus_UID(), this.nodeData.getUID());
						
			if(receivedMessage_minus!=null && receivedMessage_minus.getHops() == 0){				
				if(receivedMessage_minus.getUID() == this.nodeData.getUID()){					
					SharedData.runPocess=false;										
				}else{
					this.nodeData.setIsLeader("NOT_LEADER");
					this.nodeData.setLeaderUID(receivedMessage_minus.getUID());
					this.nodeData.setSend_plus_message(receivedMessage_minus);
					SharedData.writeLine(this.nodeData.getUID()+" knows Leader is "+this.nodeData.getLeaderUID());
				}
			}
			
			/*
			 * if the message from i - 1 is (v, out, h) then
				case
					v > u and h > 1: send+ := (v, out, h - 1)
					v > u and h = 1: send- : - (v, in, 1)
					v = u: status := leader
				endcase*/
			
			if(receivedMessage_minus!=null && receivedMessage_minus.isForwardMessage()){
				int v=receivedMessage_minus.getUID();
				int u=this.nodeData.getUID();
				int h=receivedMessage_minus.getHops();
				
				if(v>u && h>1){
					this.nodeData.setSend_plus_message(new Message(v,true,h-1));						
				}else if(v>u && h==1){
					this.nodeData.setSend_minus_message(new Message(v,false,1));
				}else if(v==u){
					this.nodeData.setIsLeader("LEADER");
					this.nodeData.setLeaderUID(u);
					this.nodeData.setSend_plus_message(new Message(u,true,0));
					if(!leaderPrinted){
						SharedData.writeLine("Leader is :"+this.nodeData.getUID());	
						leaderPrinted=true;
					}
									
				}
			}
			/*
				if the message from i + 1 is (v, out, h) then
				case
					v > u and h > I: send- : - (v, out, h - I)
					v > u and h -- 1: send+ := (v, in, 1)
					v = u: status :-- leader
				endcase*/
			if(receivedMessage_plus!=null && receivedMessage_plus.isForwardMessage()){
				int v = receivedMessage_plus.getUID();
				int u= this.nodeData.getUID();
				int h = receivedMessage_plus.getHops();
				
				if(v>u && h>1){
					this.nodeData.setSend_minus_message(new Message(v,true,h-1));
				}else if(v>u && h==1){
					this.nodeData.setSend_plus_message(new Message(v,false,1));
				}else if(v==u){
					this.nodeData.setIsLeader("LEADER");					
					this.nodeData.setLeaderUID(u);
					this.nodeData.setSend_plus_message(new Message(u,true,0));					
					if(!leaderPrinted){
						SharedData.writeLine("Leader is :"+this.nodeData.getUID());	
						leaderPrinted=true;
					}				
				}
			}
			/*
				if the message from i - 1 is (v, in, 1) and v # u then
					send+ := (v, in, 1)
			*/
			if(receivedMessage_minus!=null && !receivedMessage_minus.isForwardMessage()){			
				int v = receivedMessage_minus.getUID();
				int u= this.nodeData.getUID();				
				if(v!=u){
					this.nodeData.setSend_plus_message(new Message(v,false,1));
				}					
			}
			
			/*
				if the message from i + 1 is (v, in, 1) and v # u then
					send- := (v, in, 1)
			*/
			if(receivedMessage_plus!=null && !receivedMessage_plus.isForwardMessage()){
				int v = receivedMessage_plus.getUID();
				int u= this.nodeData.getUID();
				
				if(v!=u){
					this.nodeData.setSend_minus_message(new Message(v,false,1));
				}
			}
			/*
				if the messages from i - 1 and i + 1 are both (u, in, 1) then
					phase := phase + 1
					send+ := (u, out, 2 phase)
					send- := (u, out, 2 phase)
			 * 
			 */
			if(receivedMessage_minus!=null && receivedMessage_plus!=null){
				if(!receivedMessage_minus.isForwardMessage() && !receivedMessage_plus.isForwardMessage()){
					int v = receivedMessage_plus.getUID();
					int u= this.nodeData.getUID();
					
					if (v==u){
						this.phaseNo++;
						this.nodeData.setSend_minus_message(new Message(u, true, (int)Math.pow(2, this.phaseNo)));
						this.nodeData.setSend_plus_message(new Message(u, true,(int)Math.pow(2, this.phaseNo)));
					}
				}
			}				
						
			synchronized (this){
				try{
					SharedData.status[this.nodeData.getIndex()]=true;
					this.wait();			
				} 
				catch(Exception e){
					e.printStackTrace();
				}
			}				
		}
	}
}
