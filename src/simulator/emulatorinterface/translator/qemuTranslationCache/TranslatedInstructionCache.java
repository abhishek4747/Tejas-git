package emulatorinterface.translator.qemuTranslationCache;
//import 

//import java.util.LinkedHashMap;
//import java.util.Map;
import java.io.FileWriter;

import main.CustomObjectPool;

import generic.Instruction;
import generic.InstructionList;
import generic.OperationType;

public class TranslatedInstructionCache {
	private static final int cacheSize = 100000;
	private static long cacheHit =0;
	private static long cacheMiss =0;
	public static  LRUCache translatedInstructionTable;
	
	private static void createTranslatedInstructionTable(){
		translatedInstructionTable = new LRUCache(cacheSize);
	}
	
	public static void add(String asmText, InstructionList instructionList){//, FileWriter hitFile){
		if(translatedInstructionTable == null) {
			createTranslatedInstructionTable();
		}
		
		InstructionList instList = new InstructionList(instructionList.length());
		for(int i=0; i<instructionList.length(); i++) {
			Instruction newInsn = CustomObjectPool.getInstructionPool().borrowObject();
			newInsn.copy(instructionList.get(i));
			instList.appendInstruction(newInsn);
		}
		
		translatedInstructionTable.put(asmText, instList);
		
//		try {
//			hitFile.write("asmText = " + asmText +/* " IP = " + p.ip + */" No_micro-ops: " + instList.length()+"\n");	
//		} catch (Exception e) {
//			misc.Error.showErrorAndExit("Unable to write in translation-cache-hit-details-file");
//		}
	}
	
	public static InstructionList getInstructionList(String instText) {
		if(translatedInstructionTable == null) {
			return null;
		}
		else {
			InstructionList instructionList = translatedInstructionTable.get(instText);
			InstructionList instructionListToReturn = new InstructionList(instructionList.getListSize());
		
			// increment references for each argument for each instruction
			for(int i=0; i<instructionList.length(); i++) {
				Instruction newInstruction = new Instruction();
				newInstruction.copy(instructionList.get(i));
				instructionListToReturn.appendInstruction(newInstruction);
//				instructionListToReturn.appendInstruction(instructionList.get(i));
//				if(instructionListToReturn.get(i).getOperand1()!=null) {
//					instructionListToReturn.get(i).getOperand1().incrementNumReferences();
//				}
//
//				if(instructionListToReturn.get(i).getOperand2()!=null) {
//					instructionListToReturn.get(i).getOperand2().incrementNumReferences();
//				}
//
//				if(instructionListToReturn.get(i).getDestinationOperand()!=null) {
//					instructionListToReturn.get(i).getDestinationOperand().incrementNumReferences();
//				}
			}
//			if(instructionListToReturn.length() == 0) {
//				System.out.println("asmText = " + instText + " No_micro-ops = " + instructionListToReturn.length());
//			}
			return instructionListToReturn;
		}
	}
	
	public static boolean isPresent(String instText) {
		if(translatedInstructionTable == null) {
			return false;
		} else {
			boolean ret = translatedInstructionTable.containsKey(instText);
			
			if (ret) {
				cacheHit++;
			} else {
				cacheMiss++;
			}
			
			return ret;
		}
	}
	
	public static float getHitRate() {
		if((cacheHit+cacheMiss)==0) {
			return -1f;
		} else {
			return ((float)(cacheHit)/(float)(cacheHit+cacheMiss));
		}
	}
}

