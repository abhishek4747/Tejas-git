package emulatorinterface.translator.visaHandler;

import generic.OperationType;


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

	public static VisaHandler selectHandler(OperationType operationType)
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
				return inValid;

			case integerALU:
				return integerALU;
				
			case integerMul:
				return integerMul;
				
			case integerDiv:
				return integerDiv;

			case floatALU:
				return floatALU;
				
			case floatMul:
				return floatMul;
				
			case floatDiv:
				return floatDiv;
				
			case load:
				return load;
				
			case store:
				return store;
				
			case jump:
				return jump;
				
			case branch:
				return branch;
				
			case mov:
				return mov;
				
			case xchg:
				return xchg;
				
			case acceleratedOp:
				return acceleratedOp;
				
			case nop:
				return nop;
				
			default:
				System.out.print("Invalid operation");
				System.exit(0);
				return null;
		}
	}

	private static void createVisaHandlers() 
	{
		inValid = new Invalid();
		integerALU = new IntegerALU();
		integerMul = new IntegerMul();
		integerDiv = new IntegerDiv();
		floatALU = new FloatALU();
		floatMul = new FloatMul();
		floatDiv = new FloatDiv();
		load = new Load();
		store = new Store();
		jump = new Jump();
		branch = new Branch();
		mov = new Mov();
		xchg = new Xchg();
		acceleratedOp = new AcceleratedOp();
		nop = new NOP();
	}
}
