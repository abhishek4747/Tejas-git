
package emulatorinterface.translator;

import java.util.LinkedHashMap;
import java.util.Map;

import main.CustomObjectPool;
import generic.InstructionList;

public class LRUCache extends LinkedHashMap<String, InstructionList> {
	private static final float loadFactor = 1f;
	private final int cacheSize;
//	private final int MaxNumOfMicroOpsPerInstruction = 5;
	
	public LRUCache(final int cacheSize) {
		super((int)Math.ceil(cacheSize/loadFactor) + 1, loadFactor, true);
		this.cacheSize = cacheSize;
//		byte[] asmBytes = new byte[64];
//		for(int i=0; i<(int)Math.ceil(cacheSize/loadFactor)+1; i++) {
//			InstructionList instList = new InstructionList(MaxNumOfMicroOpsPerInstruction);
//			this.put(asmBytes.clone(), instList);
//		}
	}
	
	@Override protected boolean removeEldestEntry(final Map.Entry<String, InstructionList> eldest) {
		if(super.size() > cacheSize) {
			InstructionList eldestList = eldest.getValue();
			for(int i=0; i<eldestList.length(); i++) {
//				if(eldestList.get(i).getOperand1()!=null) {
//					eldestList.get(i).getOperand1().incrementNumReferences();
//				}
//
//				if(eldestList.get(i).getOperand2()!=null) {
//					eldestList.get(i).getOperand2().incrementNumReferences();
//				}
//
//				if(eldestList.get(i).getDestinationOperand()!=null) {
//					eldestList.get(i).getDestinationOperand().incrementNumReferences();
//				}
				CustomObjectPool.getInstructionPool().returnObject(eldestList.get(i));	
			}
			return true;
		} else {
			return false;
		}
	}
}