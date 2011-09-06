package pipeline.outoforder;

import generic.FunctionalUnitType;
import generic.OperationType;

/**
 * given an operation type, what is the functional unit required?
 */
public class OpTypeToFUTypeMapping {
	
	public static FunctionalUnitType getFUType(OperationType opType)
	{
		switch(opType)
		{
			case integerALU	:	{
									return FunctionalUnitType.integerALU;
								}
			case integerMul	:	{
									return FunctionalUnitType.integerMul;
								}
			case integerDiv	:	{
									return FunctionalUnitType.integerDiv;
								}
			case floatALU	:	{
									return FunctionalUnitType.floatALU;
								}
			case floatMul	:	{
									return FunctionalUnitType.floatMul;
								}
			case floatDiv	:	{
									return FunctionalUnitType.floatDiv;
								}
			case load		:	{
									return FunctionalUnitType.memory;
								}
			case store		:	{
									return FunctionalUnitType.memory;
								}
			case jump		:	{
									return FunctionalUnitType.integerALU;
								}
			case branch		:	{
									return FunctionalUnitType.integerALU;
								}
			case acceleratedOp : {
									return FunctionalUnitType.acceleratedFU;
								}
			default			:	{
									return FunctionalUnitType.inValid;
								}
		}
	}

}