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

public class CacheLine 
{
	private long tag;
	private int line_num;
//	private boolean valid;
	private double timestamp;
//	private boolean modified;
	private int pid;
	private MESI state;

	protected boolean hasTagMatch(long tag)
	{
		if (tag == this.getTag())
			return true;
		else
			return false;
	}
	
	protected CacheLine(int line_num)
	{
		this.setLine_num(line_num);
		this.setTag(-1);
		this.setState(MESI.INVALID);
		this.setTimestamp(0);
		//this.setModified(false);
		this.setPid(0);
	}
	
	protected CacheLine copy()
	{
		CacheLine newLine = new CacheLine(0);
		newLine.setLine_num(this.getLine_num());
		newLine.setTag(this.getTag());
		newLine.setState(this.getState());
		newLine.setTimestamp(this.getTimestamp());
		//newLine.setModified(this.isModified());
		newLine.setPid(this.getPid());
		return newLine;
	}

	protected long getTag() {
		return tag;
	}

	protected void setTag(long tag) {
		this.tag = tag;
	}

	protected int getLine_num() {
		return line_num;
	}

	protected void setLine_num(int lineNum) {
		line_num = lineNum;
	}

	protected boolean isValid() {
		if (state != MESI.INVALID)
			return true;
		else
			return false;
	}

	protected double getTimestamp() {
		return timestamp;
	}

	protected void setTimestamp(double timestamp) {
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
	protected int getPid() {
		return pid;
	}

	protected void setPid(int pid) {
		this.pid = pid;
	}

	public MESI getState() {
		return state;
	}

	public void setState(MESI state) {
		this.state = state;
	}
}
