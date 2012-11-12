package memorysystem;

import generic.Event;
import generic.GenericPooledLinkedList;
import generic.GlobalClock;
import generic.RequestType;

import java.util.ArrayList;

import sun.misc.Cleaner;

import main.ArchitecturalComponent;

public class Mode1MSHR implements MissStatusHoldingRegister {
	
	PooledLinkedList mshr;
	int curLength;
	public int maxLengthReached;
	int mshrSize;
	//int tollerablesize = 100000;
	
	public Mode1MSHR(int mshrSize)
	{
		mshr = new PooledLinkedList(mshrSize+1);
		curLength = 0;
		maxLengthReached = 0;
		this.mshrSize = mshrSize;
	}

	@Override
	public boolean isFull() {
		/*if(curLength >= mshrSize)
		{
			return true;
		}*/
		return false;
	}
	
	@Override
	public int getCurrentSize()
	{
		return curLength;
	}

	@Override
	public boolean addOutstandingRequest(AddressCarryingEvent event) {
		//check_exit(tollerablesize);
		mshr.add(event);
		curLength++;
		//System.out.println("current line of  ="  + curLength + " of "+ this);
		if(curLength > maxLengthReached)
		{
			maxLengthReached = curLength;
		}
		return true;
	}

	@Override
	public ArrayList<Event> removeRequests(AddressCarryingEvent event) {
		//check_exit(tollerablesize);		
		AddressCarryingEvent removedEvent = mshr.removeByAddress(event);
		if(removedEvent == null )
		{
			System.out.println(" : " + event.getAddress() + " : " + event.getRequestType() + " : "+ event.getRequestingElement() + " : " + event.getProcessingElement());
			misc.Error.showErrorAndExit(" null returned from removed event ");
			//ArrayList<Event> toBeReturned = new ArrayList<Event>();
			return null;
		}
		curLength--;
		//System.out.println("current line of  ="  + curLength + " of "+ this);
		event.update(removedEvent.getEventQ(),
						0,
						removedEvent.getRequestingElement(),
						removedEvent.getProcessingElement(),
						removedEvent.getRequestType()
						);
		mshr.clearNode(removedEvent);
		ArrayList<Event> toBeReturned =  new ArrayList<Event>();
		toBeReturned.add(event);
		return toBeReturned;
	}

	public ArrayList<Event> removeRequestsIfAvailable(AddressCarryingEvent event) {
		//check_exit(tollerablesize);		
		AddressCarryingEvent removedEvent = mshr.removeByAddress(event);
		if(removedEvent == null )
		{
			System.out.println(" : " + event.getAddress() + " : " + event.getRequestType() + " : "+ event.getRequestingElement() + " : " + event.getProcessingElement());
			//misc.Error.showErrorAndExit(" null returned from removed event ");
			ArrayList<Event> toBeReturned = new ArrayList<Event>();
			return toBeReturned;
		}
		curLength--;
		//System.out.println("current line of  ="  + curLength + " of "+ this);
		event.update(removedEvent.getEventQ(),
						0,
						removedEvent.getRequestingElement(),
						removedEvent.getProcessingElement(),
						removedEvent.getRequestType()
						);
		mshr.clearNode(removedEvent);
		ArrayList<Event> toBeReturned =  new ArrayList<Event>();
		toBeReturned.add(event);
		return toBeReturned;
	}

	
	@Override
	public boolean removeEvent(AddressCarryingEvent addrevent) {
		mshr.removeByRequestType(addrevent);
		curLength--;
		//System.out.println("current line of  ="  + curLength + " of "+ this);
		return true;
	}
	
	@Override
	public boolean removeEventIfAvailable(AddressCarryingEvent addrevent) {
		if(mshr.removeByRequestType(addrevent) != null)
		{
			curLength--;
		}
		//System.out.println("current line of  ="  + curLength + " of "+ this);
		return true;
	}

	@Override
	public void handleLowerMshrFull(AddressCarryingEvent eventToBeSent) {
		
	}

	@Override
	public boolean containsWriteOfEvictedLine(long address) {
		return false;
	}

	@Override
	public void dump() {
		
		System.out.println("current length = " + curLength + "\t; max length = " + maxLengthReached);
		mshr.dump();
	}

	@Override
	public int getMaxSizeReached() {
		return maxLengthReached;
	}

	@Override
	public int getMSHRStructSize() {
		return mshrSize;
	}

	@Override
	public int numOutStandingRequests(Event event) {
		if (mshr.searchByAddress((AddressCarryingEvent)event) != null) {
			return 1;
		} else {
			return 0;
		}
	}
	
	/*private void check_exit(int size)
	{
		if( curLength >= size ) {
			dump();
			ArchitecturalComponent.dumpOutStandingLoads();
			ArchitecturalComponent.exitOnAssertionFail("mshr size exceeded that "+ size);
		}
	}*/
}



class PooledLinkedList {
	
	LinkedListNode head;
	LinkedListNode tail;
	LinkedListNode addPoint;	//points to first invalid node
	int bufferSize;
	
	public PooledLinkedList(int bufferSize)
	{
		head = tail = null;
		this.bufferSize = bufferSize;
		
		for(int i = 0; i < bufferSize; i++)
		{
			LinkedListNode temp = new LinkedListNode();
			
			if(head == null)
			{
				tail = head = addPoint = temp;
			}
			else
			{
				tail.next = temp;
				tail = temp;
			}
		}
	}
	
	public AddressCarryingEvent searchByAddress(AddressCarryingEvent searchNode)
	{
		LinkedListNode temp;
		temp = head;
		while(temp != null)
		{
			if(temp.valid == false)
			{
				break;
			}
			if(searchNode.getAddress() == temp.element.getAddress() &&
					searchNode.coreId == temp.element.coreId)
			{
				return temp.element;
			}
			temp = temp.next;
		}
		return null;
	}
	
	public AddressCarryingEvent searchByRequestType(AddressCarryingEvent searchNode)
	{
		LinkedListNode temp;
		temp = head;
		while(temp != null)
		{
			if(temp.valid == false)
			{
				break;
			}
			if(searchNode.getAddress() == temp.element.getAddress() &&
				searchNode.getRequestType() == temp.element.getRequestType() &&
						searchNode.coreId == temp.element.coreId)
			{
				return temp.element;
			}
			temp = temp.next;
		}
		return null;
	}
	
	public AddressCarryingEvent removeByAddress(AddressCarryingEvent removeNode)
	{
		LinkedListNode temp, prev;
		temp = head;
		prev = null;
		while(temp != null)
		{
			if(temp.valid == false)
			{
				break;
			}
			if(removeNode.getAddress() == temp.element.getAddress() &&
					removeNode.coreId == temp.element.coreId)
			{
				if(prev != null)
				{
					prev.next = temp.next;
				}
				else
				{
					head = temp.next;
				}
				tail.next = temp;
				tail = temp;
				temp.next = null;
				//clearNode(temp);
				temp.valid = false;
				return temp.element;
			}
			prev = temp;
			temp = temp.next;
		}
		//System.out.println("called from mode1 MSHR-removeByAddress");
		//removeNode.dump();
		//ArchitecturalComponent.dumpOutStandingLoads();
		//ArchitecturalComponent.dumpAllMSHRs();
		//misc.Error.showErrorAndExit("returned null from remove");
		return null;
	}
	
	public AddressCarryingEvent removeByRequestType(AddressCarryingEvent removeNode)
	{
		LinkedListNode temp, prev;
		temp = head;
		prev = null;
		while(temp != null)
		{
			if(temp.valid == false)
			{
				break;
			}
			if(removeNode.getAddress() == temp.element.getAddress() &&
					removeNode.getRequestType() == temp.element.getRequestType() &&
					removeNode.coreId == temp.element.coreId)
			{
				if(prev != null)
				{
					prev.next = temp.next;
				}
				else
				{
					head = temp.next;
				}
				tail.next = temp;
				tail = temp;
				temp.next = null;
				temp.valid = false;
				//clearNode(temp);
				return temp.element;
			}
			prev = temp;
			temp = temp.next;
		}
		System.out.println("called from mode1 MSHR - removeByRequestType");
		removeNode.dump();
		//ArchitecturalComponent.dumpOutStandingLoads();
		//ArchitecturalComponent.dumpAllMSHRs();
		//ArchitecturalComponent.exitOnAssertionFail("returned null from remove");
		return null;
	}
	
	/*public E getFirstInvalidElement()
	{
		LinkedListNode<E> temp;
		temp = head;
		while(temp != null && temp.valid == true)
		{
			temp = temp.next;
		}
		
		if(temp == null)
		{
			ArchitecturalComponent.exitOnAssertionFail("mshr overflow!!");
		}
		
		temp.valid = true;
		return temp.element;
	}*/
	
	public void add(AddressCarryingEvent newObject)
	{
		/*LinkedListNode temp;
		temp = head;
		while(temp != null && temp.valid == true)
		{
			temp = temp.next;
		}
		
		if(temp == null)
		{
			ArchitecturalComponent.exitOnAssertionFail("mshr overflow!!");
		}
		
		temp.valid = true;
		temp.element.updateEvent(newObject.getEventQ(),
				0,
				newObject.getRequestingElement(),
				newObject.getProcessingElement(),
				newObject.getRequestType(),
				newObject.getAddress(),newObject.coreId);*/
		
		addPoint.valid = true;
		addPoint.element.updateEvent(newObject.getEventQ(),
				GlobalClock.getCurrentTime() + newObject.getEventTime(),
				newObject.getRequestingElement(),
				newObject.getProcessingElement(),
				newObject.getRequestType(),
				newObject.getAddress(),newObject.coreId);
		addPoint = addPoint.next;
		if(addPoint == null)
		{
			System.out.println("called from mode1 MSHR");
			ArchitecturalComponent.dumpOutStandingLoads();
			ArchitecturalComponent.dumpAllEventQueues();
			ArchitecturalComponent.dumpAllMSHRs();
			misc.Error.showErrorAndExit("mshr overflow !!");
		}
	}
	
	void clearNode(AddressCarryingEvent event)
	{
		event.updateEvent(null, 0, null, null, RequestType.Invalid_Event,0,-1);
		//node.valid = false;
	}
	
	public void dump()
	{
		LinkedListNode temp = head;
		int count =0;
		while(temp != null && temp.valid == true)
		{
			System.out.println(temp.element.getAddress() + " : " + temp.element.getRequestType() + " : " + temp.element.coreId + " : " + temp.element.getEventTime());
			temp = temp.next;
			count++;
		}
		System.out.println(" count of number of entries in MSHR" + count);
	} 
}

class LinkedListNode {
	
	AddressCarryingEvent element;
	LinkedListNode next;
	boolean valid;
	
	
	
	LinkedListNode()
	{
		element = new AddressCarryingEvent();		
		element.updateEvent(null, 0, null, null, RequestType.Invalid_Event,0,-1);
		next = null;
		valid = false;
	}
	
}

	


