package generic;

import org.apache.commons.pool.PoolableObjectFactory;

import emulatorinterface.Newmain;

public class PoolableOperandFactory implements PoolableObjectFactory<Operand> {

	@Override
	public void activateObject(Operand arg0) throws Exception {
		
		
	}

	@Override
	public void destroyObject(Operand arg0) throws Exception {
		
		
	}

	@Override
	public Operand makeObject() throws Exception {
		
		return new Operand();
	}

	@Override
	public void passivateObject(Operand arg0) throws Exception {
		
		if(arg0.getMemoryLocationFirstOperand() != null)
		{
			Newmain.operandPool.returnObject(arg0.getMemoryLocationFirstOperand());
		}
		if(arg0.getMemoryLocationSecondOperand() != null)
		{
			Newmain.operandPool.returnObject(arg0.getMemoryLocationSecondOperand());
		}
		
		
	}

	@Override
	public boolean validateObject(Operand arg0) {
		
		return true;
	}

}
