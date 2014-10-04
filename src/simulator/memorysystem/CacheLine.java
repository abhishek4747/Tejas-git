/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem;

public class CacheLine implements Cloneable
{
	protected long tag;

//	private boolean valid;
	protected double timestamp;
	protected long address;
//	private boolean modified;

	protected MESI state;

	
	public Object clone()
    {
        try
        {
            // call clone in Object.
            return super.clone();
        } catch(CloneNotSupportedException e)
        {
            System.out.println("Cloning not allowed.");
            return this;
        }
    }
	
	public boolean hasTagMatch(long tag)
	{
		if (tag == this.getTag())
			return true;
		else
			return false;
	}
	
	protected CacheLine(int line_num)
	{
		this.setTag(-1);
		this.setState(MESI.INVALID);
		this.setTimestamp(0);
		//this.setModified(false);
	}
	
	public CacheLine copy()
	{
		CacheLine newLine = new CacheLine(0);
		newLine.setTag(this.getTag());
		newLine.setState(this.getState());
		newLine.setTimestamp(this.getTimestamp());
		//newLine.setModified(this.isModified());
		return newLine;
	}

	public long getTag() {
		return tag;
	}

	public void setTag(long tag) {
		this.tag = tag;
	}


	public boolean isValid() {
		if (state != MESI.INVALID)
			return true;
		else
			return false;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double timestamp) {
		this.timestamp = timestamp;
	}

	protected boolean isModified() {
		if (state == MESI.MODIFIED)
			return true;
		else 
			return false;
	}
/*
	protected void setModified(boolean modified) {
		this.modified = modified;
	}
*/
	public MESI getState() {
		return state;
	}

	public void setState(MESI state) {
		this.state = state;
	}

	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}
}
