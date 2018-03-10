package main;

public class IPInfo {
    private String ip;
    private int count;

    public IPInfo(String ip) {
        this.ip = ip;
        count = 0;
    }

    public void increment() {
        count++;
    }

    public int getCount(){
        return count;
    }

    @Override
    public String toString() {
        return "IP: "+ip+" Count: "+count;
    }
}
