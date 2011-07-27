package generic;

import java.util.Comparator;

/**
 *events firstly sorted in increasing order of event time
 *secondly, by event type
 *		- denoted by priority : higher the priority, earlier it is scheduled
 *thirdly, by tie-breaker
 *      - in some cases, a relative ordering, between events of the same type,
 *        that are scheduled at the same time, is desired.
 *      - this is enforced by having a third parameter for sorting, i.e, tie-breaker.
 *      - smaller the tie-breaker, earlier it is scheduled
 */

public class EventComparator implements Comparator<Event> {

	//@Override
	public int compare(Event arg0, Event arg1) {
		
		if(arg0.getEventTime() < arg1.getEventTime())
		{
			return -1;
		}
		else if(arg0.getEventTime() > arg1.getEventTime())
		{
			return 1;
		}
		else
		{
			if(arg0.getPriority() > arg1.getPriority())
			{
				return -1;
			}
			else if(arg0.getPriority() < arg1.getPriority())
			{
				return 1;
			}
			else
			{
				if(arg0.getTieBreaker() < arg1.getTieBreaker())
				{
					return -1;
				}
				else if(arg0.getTieBreaker() > arg1.getTieBreaker())
				{
					return 1;
				}
				else
				{
					return 0;
				}
			}
		}
	}

}