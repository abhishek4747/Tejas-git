/**
 * 
 */
package net;

/**
 * @author eldhose
 *
 */
public class ID {
	int x;
	int y;
	public ID(int a, int b){
		x=a;
		y=b;
	}
	public ID(ID id)
	{
		x=id.getx();
		y=id.gety();
	}
	public void setx(int a)
	{
		x=a;
	}
	public void sety(int b)
	{
		y=b;
	}
	public int getx()
	{
		return x;
	}
	public int gety()
	{
		return y;
	}
	public ID clone()
	{
		return new ID(x,y);
	}
}
