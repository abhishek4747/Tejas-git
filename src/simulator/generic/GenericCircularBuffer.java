package generic;

/*
 * represents a circular buffer of type E
 * circular buffer is implemented as a linked list
 * starts of with size bufferSize, specified in constructor
 * if isGrowable is set, then upon exhaustion of the buffer, new objects are created and added; bufferSize is duly incremented
 * (idea is to support a higher level pool class)
 */

public class GenericCircularBuffer<E> {
	
	Class type;
	Element<E> head;
	Element<E> tail;
	int bufferSize;
	boolean isGrowable;
	int currentSize;
	
	@SuppressWarnings("unchecked")
	public GenericCircularBuffer(Class E, int bufferSize, boolean isGrowable)
	{
		this.type = E;
		this.bufferSize = bufferSize;
		this.currentSize = bufferSize;
		
		tail = new Element<E>(E, null);
		
		Element<E> temp = tail;
		for(int i = 0; i < bufferSize - 1; i++)
		{
			temp = new Element<E>(E, temp);
		}
		
		head = temp;
		tail.next = head;
		
		this.isGrowable = isGrowable;
	}
	
	public boolean append(E newObject)
	{
		if(isFull())
		{
			return false;
		}
		
		tail = tail.next;
		tail.object = newObject;
		
		currentSize++;
		
		return true;
	}
	
	public E removeObjectAtHead()
	{
		if(isEmpty() && !isGrowable)
		{
			return null;
		}
		
		else if(!isEmpty())
		{
			E toBeReturned = head.object;
			head = head.next;
			
			currentSize--;
			
			return toBeReturned;
		}
		
		else
		{
			Element<E> newElement = new Element<E>(type, tail);
			head.next = newElement;
			
			bufferSize++;
			currentSize++;
			
			return newElement.object;
		}
	}
	
	public boolean isFull()
	{
		if(currentSize == bufferSize)
		{
			return true;
		}
		return false;
	}
	
	public boolean isEmpty()
	{
		if(currentSize == 2)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int size()
	{
		return currentSize;
	}
}

@SuppressWarnings("unchecked")
class Element<E> {
	
	E object;
	Element<E> next;
	
	Element(Class E, Element<E> next)
	{
		try {
			object = (E) E.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		this.next = next;
	}
}
/*
package generic;

public class GenericCircularBuffer<E> {
	
	E[] buffer;
	int bufferSize;
	int head;
	int tail;
	
	@SuppressWarnings("unchecked")
	public GenericCircularBuffer(int bufferSize, Class E)
	{
		this.bufferSize = bufferSize;
		
		buffer = (E[]) new Object[bufferSize];
		for(int i = 0; i < bufferSize; i++)
		{
			try {
				buffer[i] = (E) E.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		head = 0;
		tail = bufferSize - 1;
	}
	
	public boolean append(E newObject)
	{
		if(isFull())
		{
			return false;
		}
		
		if(tail == -1)
		{
			head = tail = 0;
		}
		else
		{
			tail = (tail + 1)%bufferSize;
		}
		buffer[tail] = newObject;
		
		return true;
	}
	
	public E removeObjectAtHead()
	{
		if(isEmpty())
		{
			return null;
		}
		
		E toBeReturned = buffer[head];
		if(head == tail)
		{
			head = tail = -1;
		}
		else
		{
			head = (head + 1)%bufferSize;
		}
		return toBeReturned;
	}
	
	public E removeObjectAtTail()
	{
		if(isEmpty())
		{
			return null;
		}
		
		E toBeReturned = buffer[tail];
		if(head == tail)
		{
			head = tail = -1;
		}
		else
		{
			tail = (tail - 1)%bufferSize;
		}
		return toBeReturned;
	}
	
	public boolean isFull()
	{
		if((head == 0 && tail == bufferSize - 1) ||
				head == tail + 1)
		{
			return true;
		}
		return false;
	}
	
	public boolean isEmpty()
	{
		if(head == -1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int size()
	{
		if(head == -1)
		{
			return 0;
		}
		if(tail >= head)
		{
			return (tail - head + 1);
		}
		return (bufferSize - head + tail + 1);
	}
}

@SuppressWarnings("unchecked")
class Element<E> {
	
	E object;
	Element<E> next;
	
	Element(Class E)
	{
		try {
			object = (E) E.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		next = null;
	}
}

*/