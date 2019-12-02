package json;

public class SysMessage implements Cloneable{
    public int target;
    public String data;
    public float delay;

    @Override
    public SysMessage clone() throws CloneNotSupportedException {
        return (SysMessage) super.clone();
    }
}
