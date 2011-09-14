package emulatorinterface.translator.visaHandler;

import generic.OperationType;

import java.util.Hashtable;

public class VisaHandlerSelector
{
	private static VisaHandler inValid;
	private static VisaHandler integerALU;
	private static VisaHandler integerMul;
	private static VisaHandler integerDiv;
	private static VisaHandler floatALU;
	private static VisaHandler floatMul;
	private static VisaHandler floatDiv;
	private static VisaHandler load;
	private static VisaHandler store;
	private static VisaHandler jump;
	private static VisaHandler branch;
	private static VisaHandler mov;
	private static VisaHandler xchg;
	private static VisaHandler acceleratedOp;
	private static VisaHandler nop;

	public VisaHandler selectHandler(OperationType operationType)
	{
		// if the handlers are not defined in the beginning, we
		// must initialise them.
		if(inValid==null)
		{
			createVisaHandlers();
		}
		
		switch(operationType)
		{
			case inValid:
				break;
			case integerALU:
				break;
			case integerMul:
				break;
			case integerDiv:
				break;
			case floatALU:
				break;
			case floatMul:
				break;
			case floatDiv:
				break;
			case load:
				break;
			case store:
				break;
			case jump:
				break;
			case branch:
				break;
			case mov:
				break;
			case xchg:
				break;
			case acceleratedOp:
				break;
			case nop:
				break;
		}
	}
}
