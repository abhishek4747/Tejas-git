package memorysystem;

import java.util.Comparator;

public class AddressCarryingEventListInterface implements GenericPooledLinkedListInterface<AddressCarryingEvent> {
	
	public int compare(AddressCarryingEvent arg0, AddressCarryingEvent arg1)
	{
		if(arg0.getAddress() == arg1.getAddress())
		{
			return 0;
		}
		else
		{
			return -1;
		}
	}
	
	public void copy(AddressCarryingEvent src, AddressCarryingEvent dest)
	{
		dest.setAddress(src.getAddress());
		dest.setDestinationBankId(src.getDestinationBankId());
		dest.setProcessingElement(src.getProcessingElement());
		dest.setRequestType(src.getRequestType());
		dest.setSourceBankId(src.getSourceBankId());
		dest.setRequestingElement(src.getRequestingElement());
	}

}
